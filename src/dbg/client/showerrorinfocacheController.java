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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

/**
 *
 * @author bangdq
 */
public class showerrorinfocacheController  extends DbgClientCore
{
    private static Logger logger = Logger.getLogger(showerrorinfocacheController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();
    private ErrorInfoCacheClient _errorInfoCacheClient = null;
    public showerrorinfocacheController()
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
        int minuteinterval = 15;
        
        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();
        
        dic.setVariable("PAYTITLE", DbgTestToolConfig.MASTER_FORM_TITLE);
        dic.setVariable("PAYURL", DbgTestToolConfig.SYSTEM_URL);
        dic.setVariable("STATIC_URL", DbgTestToolConfig.STATIC_CONTENT_URL);
        dic.setVariable("SYSTEM_CREDITS_URL", DbgTestToolConfig.SYSTEM_CREDITS_URL);
        
        dic.showSection("showerrorinfo");    
        dic.setVariable("minuteinterval", String.valueOf(minuteinterval));
      
       try
       {
           String strerrorCode = request.getParameter("errorcode");
           Integer returnCode = -100;
           Integer  errorCount = 0;
           if(strerrorCode!=null && strerrorCode.trim()!="")
           {
               returnCode = Integer.parseInt(strerrorCode);
           }else
           {
               returnCode =  getReturnCode(request,"errorcode");
           }
            if (_errorInfoCacheClient == null) 
            {
            _errorInfoCacheClient = ErrorInfoCacheClient.getInstance(DbgTestToolConfig.ErrorInfoCacheHost,
                    DbgTestToolConfig.ErrorInfoCachePort, DbgTestToolConfig.ErrorInfoCacheSource, DbgTestToolConfig.ErrorInfoCacheAuth);
            }
           if(returnCode!=-100)
           {
               errorCount = _errorInfoCacheClient.CountErrorInfo(minuteinterval,returnCode);
           }           
           dic.setVariable("errorcode", String.valueOf(returnCode));
           dic.setVariable("errorcount", String.valueOf(errorCount));           
           List<T_ErrorInfo>  lists = _errorInfoCacheClient.getErrorInfos(minuteinterval);
           if(lists!=null && lists.size()>0)
            {
                for(int i= 0 ; i < lists.size(); i++)
                {
                    TemplateDataDictionary itemDic = dic.addSection("errorinfo");
                    itemDic.setVariable("transid",String.valueOf(lists.get(i).transactionID));
                    itemDic.setVariable("returncode", String.valueOf(lists.get(i).returnCode));
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(new Date(lists.get(i).rqTimestamp*1000));
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    
                    itemDic.setVariable("datetime", simpleDateFormat.format(cal.getTime()));
                    itemDic.setVariable("order", String.valueOf(i));
                }                        
            }

       }
       catch (Exception ex)
       {
           logger.error(String.format("%s : %s", "showerrorinfocacheController doAction has exception: ", ex.toString()));
           return ex.toString();
           
       }
        return template.renderToString(dic);
    }
     private int getReturnCode(HttpServletRequest request, String returnCode) 
             throws UnsupportedEncodingException
     {
        String queryString = request.getQueryString();
        if(queryString!= null && queryString.length()>0)
        {
            String decoded = URLDecoder.decode(queryString, "UTF-8");
            String[] pares = decoded.split("&");
            Map<String, String> parameters = new HashMap<String, String>();
            for(String pare : pares) {
                String[] nameAndValue = pare.split("=");
                parameters.put(nameAndValue[0], nameAndValue[1]);
            }

            // Now you can get your parameter:
            String valueOfreturnCode = parameters.get(returnCode);
            if(valueOfreturnCode!=null && valueOfreturnCode.trim()!="")
                return Integer.parseInt(valueOfreturnCode.trim());
            else
                return 0;
        }
        else
            
        {
            
            return 0;
        }
            
     }
    
}
