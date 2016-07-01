/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.client;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author hainpt
 */
public class DbgClientCore extends HttpServlet
{
    private static Logger logger = Logger.getLogger(DbgClientCore.class);
    protected void echo(Object text, HttpServletResponse response)
    {
        PrintWriter out = null;
        try
        {
            response.setContentType("text/html;charset=UTF-8");
            out = response.getWriter();
            if (out != null)
            {
                out.print(text);
                out.close();
            }
        }
        catch (IOException ex)    
        {
            logger.error(ex.toString());
            logger.error(String.format("StackTrace:%s", ExceptionUtils.getStackTrace(ex)));
            logger.error(String.format("RootCause:%s", ExceptionUtils.getRootCause(ex)));
        } 
        finally
        {
            out.close();
        }
    }
}
