/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 *
 * @author hainpt
 */
public class SmsConfirmController extends DbgClientCore
{
    private static Logger logger = Logger.getLogger(SmsConfirmController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();

    public SmsConfirmController()
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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws TException, TemplateException
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

    private String renderByTemplate(HttpServletRequest request) throws TemplateException
    {

        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();

        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);

        dic.showSection("smsConfirm");

        String userID = request.getParameter("userid");
        String itemID = request.getParameter("itemid");
        String itemName = request.getParameter("itemname");
        String itemQuantity = request.getParameter("itemquantity");
        String chargeAmt = request.getParameter("chargeamt");
        String platform = request.getParameter("platform");
        String flow = request.getParameter("flow");
        String serverID = request.getParameter("appserverid");
        String appTransID = request.getParameter("apptransid");
        String pmcID = request.getParameter("pmcid");
        String appID = request.getParameter("appid");
        String transID = request.getParameter("transid");

        dic.setVariable("userid", userID);
        dic.setVariable("itemid", itemID);
        dic.setVariable("itemname", itemName);
        dic.setVariable("itemquantity", itemQuantity);
        dic.setVariable("chargeamt", chargeAmt);
        dic.setVariable("platform", platform);
        dic.setVariable("flow", flow);
        dic.setVariable("serverid", serverID);
        dic.setVariable("appTransID", appTransID);
        dic.setVariable("pmcid", pmcID);
        dic.setVariable("deviceplatform", request.getParameter("pl"));
        dic.setVariable("appserverid", request.getParameter("appserverid"));
        dic.setVariable("transid", transID);
        dic.setVariable("appid", appID);
        dic.setVariable("apptransid", String.valueOf(appTransID));
        
//        DbgClient client = new DbgClient(DbgTestToolConfig.DBG_CLIENT_CONFIG);
//        CreateRequestDataResult r = client.createRequestData(userID,
//                Integer.parseInt(platform),
//                Integer.parseInt(flow),
//                serverID,
//                itemID,
//                itemName,
//                Long.parseLong(itemQuantity),
//                Long.parseLong(chargeAmt), appTransID);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DbgClient.DateTimeFormatString);
        
//        dic.setVariable("cfreturncode", r.returnMessage + ":" + String.valueOf(r.returnCode));
        
        DbgClient client = new DbgClient(DbgTestToolConfig.DBG_CLIENT_CONFIG);
        CreateSmsRequestDataResult r = client.createSmsRequestData(userID, 
                Integer.parseInt(platform), 
                Integer.parseInt(flow), 
                serverID, 
                itemID, 
                itemName, 
                Long.parseLong(itemQuantity), 
                Long.parseLong(chargeAmt), appTransID);
        
        
        
        dic.setVariable("returnCode1", String.valueOf(r.returnCode));
        dic.setVariable("returnMessage1", String.valueOf(r.returnMessage));
        if (r.returnCode == 1)
        {                        
            dic.setVariable("transID1", String.valueOf(r.transID));
            dic.setVariable("reqTime1", simpleDateFormat.format(r.requestTime));
            dic.setVariable("smsMessage1", r.smsMessage);
            
            StringBuilder sBuilder = new StringBuilder();
            for(int i=0; i< r.servicePhones.length;i++)
            {
                sBuilder.append(r.servicePhones[i]);
                sBuilder.append("-");
            }
            
            dic.setVariable("servicePhones1", sBuilder.toString());
        }
        
        return template.renderToString(dic);
    }
}
