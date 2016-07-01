/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import static dbg.client.DbgClient.DateTimeFormatString;
import dbg.entity.TransMiniEntity;
import dbg.request.HistoryTransReq;
import dbg.util.HashUtil;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
public class ViewTransHistoryController extends DbgClientCore {

    private static Logger logger = Logger.getLogger(ViewTransHistoryController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();
    final List<String> transFields = new ArrayList<>(Arrays.asList(
            "transID", "appID", "userID", "platform", "flow", "serverID",
            "reqDate", "itemID", "itemName", "quantity", "chargeAmt", "pmcID",
            "pmcTransID", "grossAmt", "netAmt", "status", "appTransID"));

    public ViewTransHistoryController() {
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

        // TODO : decode to check params
        echoAndStats(startTime, renderByTemplate(request), response);
    }

    private void echoAndStats(long startTime, String html, HttpServletResponse response) {
        this.echo(html, response);
        this.readStats.addMicro((System.nanoTime() - startTime) / 1000);
    }

    private String renderHistoryHeader() {

        StringBuilder strBuff = new StringBuilder();

        strBuff.append("<tr>");
        strBuff.append("<td>");
        strBuff.append("#");
        strBuff.append("</td>");
        for (String title : transFields) {
            strBuff.append("<td>");
            strBuff.append(title);
            strBuff.append("</td>");
        }
        strBuff.append("</tr>");
        return strBuff.toString();
    }

    private String renderHistoryBoday(List<TransMiniEntity> trans) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        StringBuilder strBuff = new StringBuilder();
        int count = 0;

        for (TransMiniEntity tran : trans) {

            count += 1;
            strBuff.append("<tr>");

            strBuff.append("<td>");
            strBuff.append(count);
            strBuff.append("</td>");

            for (String fieldName : transFields) {
                strBuff.append("<td>");
                Class<?> c = tran.getClass();
                java.lang.reflect.Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(tran);
                strBuff.append(value);
                strBuff.append("</td>");
            }
            strBuff.append("</tr>");
        }
        return strBuff.toString();
    }

    private String renderByTemplate(HttpServletRequest request) throws TemplateException {

        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();
        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);

        dic.showSection("viewTransHistory");

        String userId = "";
        int appId = -1;
        List<TransMiniEntity> historyTrans = null;

        if (request.getParameter("appId") != null && request.getParameter("userId") != null) {
            try {
                userId = request.getParameter("userId");
                appId = Integer.parseInt(request.getParameter("appId"));

                //HistoryTransResp resp = null;

                try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                    HistoryTransReq req = new HistoryTransReq();
                    req.appId = appId;
                    req.userId = userId;
                    String data = String.format("%s%s%s", req.appId, userId, DbgTestToolConfig.DBG_CLIENT_CONFIG.hashKey);
                    req.sig = HashUtil.hashSHA256(data);

                    HttpPost httpPost = new HttpPost(DbgTestToolConfig.TransHistoryUrl);
                    List<NameValuePair> nvps = new ArrayList<>();
                    nvps.add(new BasicNameValuePair("appid", String.valueOf(req.appId)));
                    nvps.add(new BasicNameValuePair("userId", req.userId));
                    nvps.add(new BasicNameValuePair("sig", req.sig));
                    httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));


                    try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                        HttpEntity entity = response.getEntity();

                        InputStream inputStream = entity.getContent();;
                        String sResponse = IOUtils.toString(inputStream, "UTF-8");
                        //.replace("[{", "{").replace("}]", "}");

                        JsonElement jsonElement = new JsonParser().parse(sResponse);
                        JsonObject jsonObj = jsonElement.getAsJsonObject();

                        if (jsonObj != null && jsonObj.get("returnCode").getAsInt() == 1) {

                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.setDateFormat(DateTimeFormatString);
                            Gson gson = gsonBuilder.create();

                            Type collectionType = new TypeToken<List<TransMiniEntity>>() {
                            }.getType();
                            historyTrans = gson.fromJson(jsonObj.get("historyTrans").toString(), collectionType);
                            if (historyTrans != null && historyTrans.size() > 0) {
                                logger.info("First trans data:" + historyTrans.get(0).toJsonString());
                            }
                        }

                        logger.info("HistoryTransResp:" + sResponse);

                    }
                }

                dic.setVariable("transHistory", "");

                if (historyTrans != null) {
                    dic.setVariable("transHistory", renderHistoryHeader() + renderHistoryBoday(historyTrans));
                }

            } catch (Exception ex1) {
                dic.setVariable("transHistory", ex1.toString());
                logger.error(ex1.toString());
            }
        } else {
            dic.setVariable("transHistory", "");
        }

        dic.setVariable("appId", appId + "");
        dic.setVariable("userId", userId);

        return template.renderToString(dic);
    }
}
