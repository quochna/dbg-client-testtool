/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import dbg.cache.client.DBGCacheClient;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 *
 * @author hainpt
 */
public class ViewCacheController extends DbgClientCore {

    private static Logger logger = Logger.getLogger(ViewCacheController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();

    public ViewCacheController() {
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

    private String renderByTemplate(HttpServletRequest request) throws TemplateException {

        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();
        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);

        dic.showSection("viewCache");
        String cacheValue = "";
        String cacheCntValue = "";
        String key = "";
        String cntKey = "";

        try {
            key = request.getParameter("cacheKey");


            if (key != null && !"".equals(key.trim())) {
                DBGCacheClient cache = DBGCacheClient.getInstance(DbgTestToolConfig.CacheHost,
                        DbgTestToolConfig.CachePort, DbgTestToolConfig.CacheSource, DbgTestToolConfig.CacheAuth);
                try {
                    cacheValue = cache.get(key.trim());
                } catch (TException ex) {
                    logger.error(ex.toString());
                    cacheValue = ex.toString();
                }
            }
            //dic.setVariable("confirmbutton", r.returnMessage);
        } catch (Exception ex1) {
            cacheValue = ex1.toString();
            logger.error(ex1.toString());
        }
        
        try {
            cntKey = request.getParameter("cacheCntKey");


            if (cntKey != null && !"".equals(cntKey.trim())) {
                DBGCacheClient cache = DBGCacheClient.getInstance(DbgTestToolConfig.CacheHost,
                        DbgTestToolConfig.CachePort, DbgTestToolConfig.CacheSource, DbgTestToolConfig.CacheAuth);
                try {
                    cacheCntValue = Integer.toString(cache.getCounterValue(cntKey.trim()));
                } catch (TException ex) {
                    logger.error(ex.toString());
                    cacheCntValue = ex.toString();
                }
            }
            //dic.setVariable("confirmbutton", r.returnMessage);
        } catch (Exception ex1) {
            cacheCntValue = ex1.toString();
            logger.error(ex1.toString());
        }        

        dic.setVariable("cacheKey", key);
        dic.setVariable("cacheValue", cacheValue);
        dic.setVariable("cacheCntKey", cntKey);
        dic.setVariable("cacheCntValue", cacheCntValue);

        return template.renderToString(dic);
    }
}
