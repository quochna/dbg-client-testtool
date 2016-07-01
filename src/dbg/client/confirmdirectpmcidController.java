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
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 *
 * @author hainpt
 */
public class confirmdirectpmcidController extends DbgClientCore
{
    private static Logger logger = Logger.getLogger(confirmdirectpmcidController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();
    
    public confirmdirectpmcidController()
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
        echoAndStats(startTime, renderByTemplate( request), response);
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
        
        dic.showSection("confirmdirectpmcid");
        
        String userID = request.getParameter("userid");
        String itemID = request.getParameter("itemid");
        String itemName = request.getParameter("itemname");
        String itemQuantity = request.getParameter("itemquantity");
        String chargeAmt = request.getParameter("chargeamt");
        String platform = request.getParameter("platform");
        String flow = request.getParameter("flow");
        String serverID = request.getParameter("serverid");
        String pmcID = request.getParameter("pmcid");
        String appTransID = request.getParameter("apptransid");
        
        String appserverid = request.getParameter("appserverid");
        String pl = request.getParameter("pl");
        
        dic.setVariable("userid", userID);
        dic.setVariable("itemid", itemID);
        dic.setVariable("itemname", itemName);
        dic.setVariable("itemquantity", itemQuantity);
        dic.setVariable("chargeamt", chargeAmt);
        dic.setVariable("platform", platform);
        dic.setVariable("flow", flow);
        dic.setVariable("serverid", serverID);
        dic.setVariable("pmcid", pmcID);
        dic.setVariable("apptransid", appTransID);
        dic.setVariable("appserverid", appserverid);
        dic.setVariable("pl", pl);
        
        DbgClient client = new DbgClient(DbgTestToolConfig.DBG_CLIENT_CONFIG);
        CreateRequestDataResult r = client.createRequestData(userID, 
                                    Integer.parseInt(platform), 
                                    Integer.parseInt(flow), 
                                    serverID, 
                                    itemID, 
                                    itemName, 
                                    Long.parseLong(itemQuantity), 
                                    Long.parseLong(chargeAmt),
                                    appTransID);
        
        
        dic.setVariable("cfreturncode", r.returnMessage + ":"  + String.valueOf(r.returnCode ));
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DbgClient.DateTimeFormatString);
        if (r.returnCode == 1)
        {
            // dic.setVariable("confirmbutton", r.htmlForPostReq);
            dic.setVariable("transid1", String.valueOf(r.transID));
            dic.setVariable("reqtime1", simpleDateFormat.format(r.requestTime));
            dic.setVariable("appdata1", r.appData);            
            dic.setVariable("submittransthanhtoanurl", DbgTestToolConfig.SubmittransThanhtoanurl);
            dic.setVariable("transid", String.valueOf(r.transID));
            dic.setVariable("appid", String.valueOf(DbgTestToolConfig.DBG_CLIENT_CONFIG.appID));
            dic.setVariable("appdata", r.appData);
           
            
        }
        else
        {
            dic.setVariable("confirmbutton", r.returnMessage);
        }
        
        return template.renderToString(dic);
    }
}
