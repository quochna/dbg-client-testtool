/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dbg.request.ReceiveMOReq;
import dbg.response.ReceiveMOResp;
import dbg.util.BinaryConverter;
import dbg.util.HashUtil;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 *
 * @author hainpt
 */
public class SmsSendMOResultController extends DbgClientCore
{
    private static Logger logger = Logger.getLogger(SmsSendMOResultController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();
    private final String DateTimeFormatString="dd/MM/yyyy HH:mm:ss";
    
    
    public SmsSendMOResultController()
    {
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try
        {
            processRequest(request, response);
        }
        catch (Exception ex)
        {
            logger.error(ex.toString());
            logger.error(String.format("StackTrace:%s", ExceptionUtils.getStackTrace(ex)));
            logger.error(String.format("RootCause:%s", ExceptionUtils.getRootCause(ex)));
            this.echo(DbgTestToolConfig.MAINTAIN_MSG, response);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws TException, TemplateException, ParseException
    {
        long startTime = System.nanoTime();
        String stats = request.getParameter(PARAM_STATS);

        if (stats != null && stats.equals(PARAM_STATS))
        {
            this.echo(this.readStats.dumpHtmlStats(), response);
            return;
        }

        // TODO : decode to check params
        echoAndStats(startTime, renderByTemplate(request), response);
    }

    private void echoAndStats(long startTime, String html, HttpServletResponse response)
    {
        this.echo(html, response);
        this.readStats.addMicro((System.nanoTime() - startTime) / 1000);
    }
    
    private String renderByTemplate(HttpServletRequest request) throws TemplateException, ParseException
    {

        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();

        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);

        dic.showSection("smsSendMOResult");     
        // dic.setVariable("requestid", requestID);
        
        try
        {
            ReceiveMOReq req = new ReceiveMOReq();
            req.requestID = request.getParameter("requestid");
            req.userID = request.getParameter("userid");
            req.serviceID = request.getParameter("serviceid");
            req.commandCode = request.getParameter("commandcode");
            req.message = request.getParameter("message");
            req.mobileOperator = request.getParameter("mobileoperator");
            req.userName = request.getParameter("username");
            req.password = request.getParameter("password");
            req.requestTime = request.getParameter("requesttime");

            ReceiveMOResp resp = receiveMO(req);
            dic.setVariable("returncode", String.valueOf(resp.returnCode));
        }
        catch(Exception ex)
        {
            dic.setVariable("returncode", ExceptionUtils.getRootCauseMessage(ex));
        }
  
        return template.renderToString(dic);
    }
    
    private ReceiveMOResp receiveMO(ReceiveMOReq request) throws Exception
    {        
        ReceiveMOResp resp = null;
        
        request.message= BinaryConverter.toBase64RFC(request.message.getBytes(Charset.forName("UTF-8")));
        
        try (CloseableHttpClient httpclient = HttpClients.createDefault())
        {
            String revMoUrl = DbgTestToolConfig.DBG_CLIENT_CONFIG.apiBaseUrl + "receivemo";
            HttpPost httpPost = new HttpPost(revMoUrl);
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("requestid", request.requestID));
            nvps.add(new BasicNameValuePair("userid", request.userID));
            nvps.add(new BasicNameValuePair("serviceid", request.serviceID));
            nvps.add(new BasicNameValuePair("commandcode", request.commandCode));
            nvps.add(new BasicNameValuePair("message", request.message));
            nvps.add(new BasicNameValuePair("mobileoperator", request.mobileOperator));
            nvps.add(new BasicNameValuePair("username", request.userName));
            nvps.add(new BasicNameValuePair("password", request.password));
            nvps.add(new BasicNameValuePair("requesttime", request.requestTime));
            
            String plainText = String.format("%s%s%s%s%s%s", request.requestID, request.userID,
                    request.serviceID, request.commandCode, request.mobileOperator,
                    DbgTestToolConfig.receiveMoSecretKey);
            String sig = HashUtil.MD5(plainText);
            
            nvps.add(new BasicNameValuePair("sig", sig));
            
            logger.info(String.format("plainText:%s, sig:%s", plainText, sig));
            
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            
            try (CloseableHttpResponse response = httpclient.execute(httpPost))
            {
                HttpEntity entity = response.getEntity();

                InputStream inputStream = entity.getContent();;
                String sResponse = IOUtils.toString(inputStream, "UTF-8");

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setDateFormat(DateTimeFormatString);
                Gson gson = gsonBuilder.create();

                resp = gson.fromJson(sResponse, ReceiveMOResp.class);
            }
        }
        
        return resp;
    }
}
