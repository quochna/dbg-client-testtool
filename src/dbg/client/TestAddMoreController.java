/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import dbg.client.DbgTestToolConfig;
import static dbg.client.DbgTestToolConfig.SubmitTransUrl;
import dbg.util.DateTimeUtil;
import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;
import hapax.TemplateResourceLoader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
public class TestAddMoreController extends DbgClientCore
{

    private static Logger logger = Logger.getLogger(TestAddMoreController.class);
    private final String ITEM_SEPARATE = "\\|";
    private final String PARAM_STATS = "stats";
    private final Monitor readStats = new Monitor();

    public TestAddMoreController()
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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws TException, TemplateException, Exception
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

    private String getAppTransID() throws ParseException
    {
        //Date date = new Date();
        Date date = DateTimeUtil.getCurDateWithMilisec();
        
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssS");
        
        return formatter.format(date);
    }

    private String renderByTemplate(HttpServletRequest request) throws Exception
    {
        TemplateLoader templateLoader = TemplateResourceLoader.create("view/");
        Template template = templateLoader.getTemplate("master");
        TemplateDataDictionary dic = TemplateDictionary.create();
        
        dic.setVariable("submittransurl", SubmitTransUrl);
        
        dic.showSection("testaddmore");

        return template.renderToString(dic);
    }
}
