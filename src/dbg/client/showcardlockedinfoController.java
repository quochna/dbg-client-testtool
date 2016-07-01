/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;
import dbg.client.DbgTestToolConfig;
import dbg.cache.CardLockedInfoCacheClient;
import dbg.cache.thrift.T_CardLockedSerialInfo;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 *
 * @author bangdq
 */
public class showcardlockedinfoController extends DbgClientCore 
{
    private static Logger logger = Logger.getLogger(showcardlockedinfoController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();
    private CardLockedInfoCacheClient _cardLockedInfoCacheClient = null;
    
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

  

    private String renderByTemplateByGet(HttpServletRequest request) 
            throws TemplateException {
       
        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();

        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);
       
        dic.showSection("showcardlockedinfo");
        dic.setVariable("pmcid", "1");
        dic.setVariable("maxcounter", String.valueOf(DbgTestToolConfig.maxcounter));
        dic.setVariable("diffinminute", String.valueOf(DbgTestToolConfig.diffinminute));
        dic.setVariable("lockinminute", String.valueOf(DbgTestToolConfig.lockinminute));
       
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
        dic.showSection("showcardlockedinfo");
        
        String cardserial = request.getParameter("cardserial");
        String pmcid = request.getParameter("pmcid");
        String maxcounter = request.getParameter("maxcounter");
        String diffinminute = request.getParameter("diffinminute");   
        String lockinminute = request.getParameter("lockinminute");   
        
        dic.setVariable("maxcounter", maxcounter);
        dic.setVariable("diffinminute", diffinminute);
        dic.setVariable("lockinminute", lockinminute);
        dic.setVariable("cardserial", cardserial);
        dic.setVariable("pmcid", pmcid);
         
          if (_cardLockedInfoCacheClient == null) 
            {
            _cardLockedInfoCacheClient = CardLockedInfoCacheClient.getInstance(DbgTestToolConfig. CardLockedInfoCacheHost,
                    DbgTestToolConfig. CardLockedInfoCachePort, DbgTestToolConfig. CardLockedInfoCacheSource, DbgTestToolConfig. CardLockedInfoCacheAuth);
            }
        T_CardLockedSerialInfo info =   _cardLockedInfoCacheClient.getCardLockedInfo(createkey(cardserial, pmcid) );
        if(info!=null)
        {
            dic.setVariable("islocked", String.valueOf(info.isLock));
            dic.setVariable("counter", String.valueOf(info.counter));
            Calendar cal = new GregorianCalendar();
            cal.setTime(new Date(info.lastUpdateCounter*1000));
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dic.setVariable("lastupdatecounter", simpleDateFormat.format(cal.getTime()));
            cal.setTime(new Date(info.lastLockDate*1000));
            dic.setVariable("lastlockdate", simpleDateFormat.format(cal.getTime()));
        }  
        
        boolean result =  _cardLockedInfoCacheClient.isCardLocked(createkey(cardserial, pmcid),Integer.parseInt(maxcounter), 
                Integer.parseInt(diffinminute), Integer.parseInt(lockinminute));
        dic.setVariable("iscardlocked", String.valueOf(result));
        return template.renderToString(dic);
    }
     private String createkey(String cardserial, String pmcid)
     {         
        return String.format("%s_%s_%s", cardserial.trim().toLowerCase(),pmcid,"lockedcard");
     }
}
