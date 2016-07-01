    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import dbg.cache.ErrorInfoCacheClient;
import dbg.cache.thrift.T_ErrorInfo;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
/**
 *
 * @author bangdq
 */
public class adderrorinfoController extends DbgClientCore 
{
    private static Logger logger = Logger.getLogger(adderrorinfoController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();
    private ErrorInfoCacheClient _errorInfoCacheClient = null;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        long startTime = System.nanoTime();
        String stats = request.getParameter(PARAM_STATS);

        if (stats != null && stats.equals(PARAM_STATS)) {
            this.echo(this.readStats.dumpHtmlStats(), response);
            return;
        }
        try {
            // TODO : decode to check params
            echoAndStats(startTime, renderByTemplateByGet(request), response);
        } catch (Exception ex) 
        {
            logger.error(ex.toString());
            this.echo(DbgTestToolConfig.MAINTAIN_MSG, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception ex)
        {
            logger.error(ex.toString());
            this.echo(ex.toString(), response);
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
        echoAndStats(startTime, renderByTemplateByPost(request), response);
    }

    private void echoAndStats(long startTime, String html, HttpServletResponse response) {
        this.echo(html, response);
        this.readStats.addMicro((System.nanoTime() - startTime) / 1000);
    }

    private String getAppTransID() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssS");
        return formatter.format(date);
    }

    private String renderByTemplateByGet(HttpServletRequest request) 
            throws TemplateException {
        String appTransID = getAppTransID();
        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();

        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);
       
        dic.showSection("adderrorinfo");
        dic.setVariable("transid", appTransID);

        return template.renderToString(dic);
    }
     private String renderByTemplateByPost(HttpServletRequest request) 
            throws TemplateException, TException 
     {
       
        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();

        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);
        dic.showSection("adderrorinfo");
        
        String transid = request.getParameter("transid");
        String returncode = request.getParameter("returncode");
        
        dic.setVariable("transid", transid);
         dic.setVariable("returncode", returncode);
         
          if (_errorInfoCacheClient == null) 
            {
            _errorInfoCacheClient = ErrorInfoCacheClient.getInstance(DbgTestToolConfig.ErrorInfoCacheHost,
                    DbgTestToolConfig.ErrorInfoCachePort, DbgTestToolConfig.ErrorInfoCacheSource, DbgTestToolConfig.ErrorInfoCacheAuth);
            }
          T_ErrorInfo  rrorinfo = new T_ErrorInfo();
          if(returncode!="" && returncode.length()>0)
            rrorinfo.returnCode = Integer.parseInt(returncode);
          else
             rrorinfo.returnCode = 0;
          
          rrorinfo.transactionID = Long.parseLong(transid);
          rrorinfo.rqTimestamp = 0;
         _errorInfoCacheClient.addErrorInfo(15, rrorinfo);
         dic.setVariable("status", "you are added the error info to cache");

        return template.renderToString(dic);
    }
}
