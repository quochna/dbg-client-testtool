/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import dbg.util.DateTimeUtil;
import dbg.util.HashUtil;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
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
public class GetTransStausController extends DbgClientCore {

    private static Logger logger = Logger.getLogger(GetTransStausController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();

    public GetTransStausController() {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex) {
            logger.error(ex.toString());
            this.echo(DbgTestToolConfig.MAINTAIN_MSG, response);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws TException, TemplateException {
        long startTime = System.nanoTime();
        String stats = request.getParameter(PARAM_STATS);

        if (stats != null && stats.equals(PARAM_STATS)) {
            this.echo(this.readStats.dumpHtmlStats(), response);
            return;
        }

        echoAndStats(startTime, renderByTemplate(request), response);
    }

    private void echoAndStats(long startTime, String html, HttpServletResponse response) {
        this.echo(html, response);
        this.readStats.addMicro((System.nanoTime() - startTime) / 1000);
    }

    private String renderByTemplate(HttpServletRequest request) throws TemplateException {

        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();
        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);

        dic.showSection("getTransStatus");

        String clientFeID = "";
        String hashKey = "";
        String transID = "";

        String result = "";

        try {
            clientFeID = request.getParameter("clientFeID");
            hashKey = request.getParameter("hashKey");
            transID = request.getParameter("transID");
            if (transID != null) {
                result = getTransStatus(clientFeID, hashKey, transID);
            }

        } catch (Exception ex1) {
            result = ex1.toString();
            logger.error(ex1.toString());
        }


        dic.setVariable("result", result);

        return template.renderToString(dic);
    }
    private final String DateTimeFormatString = "yyyy-MM-dd HH:mm:ss.SSS";

    public String getTransStatus(String clientID, String hashKey, String transID) throws UnsupportedEncodingException, IOException, ParseException, NoSuchAlgorithmException {
        String sResponse = null;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

            HttpPost httpPost = new HttpPost(DbgTestToolConfig.gettransstatus);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateTimeFormatString);

            Date reqDate = DateTimeUtil.getCurDateWithMilisec();
            String data = String.format("%s%s%s%s", clientID, transID,
                    simpleDateFormat.format(reqDate), hashKey);
            String sig = HashUtil.hashSHA256(data);

            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("feclientid", clientID + ""));
            nvps.add(new BasicNameValuePair("transID", transID));
            nvps.add(new BasicNameValuePair("reqdate", simpleDateFormat.format(reqDate)));
            nvps.add(new BasicNameValuePair("sig", sig));

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();

                InputStream inputStream = entity.getContent();
                sResponse = IOUtils.toString(inputStream, "UTF-8");
            }
        }

        return sResponse;
    }
}
