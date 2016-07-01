/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.edit;


import dbg.client.*;
import com.vng.jcore.common.Config;
import dbg.util.BinaryConverter;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
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
public class ConfirmController extends DbgClientCore {

    private static Logger logger = Logger.getLogger(ConfirmController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();

    public ConfirmController() {
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
            logger.error(String.format("StackTrace:%s", ExceptionUtils.getStackTrace(ex)));
            logger.error(String.format("RootCause:%s", ExceptionUtils.getRootCause(ex)));
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

        // TODO : decode to check params
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

        dic.showSection("confirm");

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
        dic.setVariable("bankCode", request.getParameter("bankCode"));
        try {
            dic.setVariable("addInfo", BinaryConverter.toBase64RFC(request.getParameter("addInfo").getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(ConfirmController.class.getName()).log(Level.SEVERE, null, ex);
        }
        String appTest = request.getParameter("appTest");
        logger.info("appTest: " + appTest);
        dic.setVariable("appTest", appTest);

        DbgClientConfig testDbgClientConfig = null;
        if (appTest != null) {
            try {
                int appTestID = Integer.parseInt(appTest);
                testDbgClientConfig = new DbgClientConfig();
                testDbgClientConfig.appID = appTestID;
                testDbgClientConfig.key1 = Config.getParam("appid=" + appTestID, "key1");
                testDbgClientConfig.key2 = Config.getParam("appid=" + appTestID, "key2");
                testDbgClientConfig.hashKey = Config.getParam("appid=" + appTestID, "hashkey");
                testDbgClientConfig.apiBaseUrl = Config.getParam("dbgclient", "apiBaseUrl");

            } catch (Exception e) {
                testDbgClientConfig = null;
            }
        }

        DbgClient client;
        if (testDbgClientConfig != null) {
            client = new DbgClient(testDbgClientConfig);
            appID = appTest;
            logger.info("Request transid: appID=" + testDbgClientConfig.appID);
            logger.info("Request transid: key1=" + testDbgClientConfig.key1);
            logger.info("Request transid: key2=" + testDbgClientConfig.key2);
            logger.info("Request transid: hashKey=" + testDbgClientConfig.hashKey);
            logger.info("Request transid: apiBaseUrl=" + testDbgClientConfig.apiBaseUrl);

        } else {
            client = new DbgClient(DbgTestToolConfig.DBG_CLIENT_CONFIG);
        }
//        CreateRequestDataResult r = client.createRequestData(userID,
//                Integer.parseInt(platform),
//                Integer.parseInt(flow),
//                serverID,
//                itemID,
//                itemName,
//                Long.parseLong(itemQuantity),
//                Long.parseLong(chargeAmt), appTransID);

//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DbgClient.DateTimeFormatString);

//        dic.setVariable("cfreturncode", r.returnMessage + ":" + String.valueOf(r.returnCode));

//        if ((transID == null) || transID.isEmpty()) {
//            if (r.returnCode == 1) {
//                dic.setVariable("transid", String.valueOf(r.transID));
//            }
//        } else {
//            dic.setVariable("transid", transID);
//        }
//
        if ((appID == null) || appID.isEmpty()) {
          //  if (r.returnCode == 1) {
                dic.setVariable("appid", String.valueOf(client._appID));
           // }
        } else {
            dic.setVariable("appid", appID);
        }

        dic.setVariable("apptransid", String.valueOf(appTransID));

//        dic.setVariable("returnCode1", String.valueOf(r.returnCode));
//        dic.setVariable("returnMessage1", r.returnMessage);
//        if (r.returnCode == 1) {
        dic.setVariable("submittransurl", Config.getParam("dbgclient", "submittransurl"));
//            dic.setVariable("appdata", r.appData);

            // dic.setVariable("confirmbutton", r.htmlForPostReq);
//            dic.setVariable("transid1", String.valueOf(r.transID));
//            dic.setVariable("reqtime1", simpleDateFormat.format(r.requestTime));
//            dic.setVariable("appdata1", r.appData);

            dic.setVariable("appid", String.valueOf(DbgTestToolConfig.DBG_CLIENT_CONFIG.appID));
//        } else {
//            dic.setVariable("confirmbutton", r.returnMessage);
//        }

        return template.renderToString(dic);
    }
}
