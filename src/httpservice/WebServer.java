package httpservice;

import dbg.client.DbgTestToolConfig;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import com.vng.jcore.common.Config;
import org.apache.log4j.Logger;

public class WebServer extends Thread {

    private static Logger ClassLogger = Logger.getLogger(WebServer.class);

    @Override
    public void run() {
        try {
            this.startWebServer();
        } catch (Exception ex) {
            ClassLogger.error("Web server error", ex);
        }
    }

    public void startWebServer() throws Exception {

        Server server = new Server();

        int listenPort = Integer.parseInt(Config.getParam("jetty", "listenPort"));
        int maxThreads = Integer.parseInt(Config.getParam("jetty", "maxThreads"));
        int minThreads = Integer.parseInt(Config.getParam("jetty", "minThreads"));
        int acceptors = Integer.valueOf(Config.getParam("jetty", "acceptors"));

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(minThreads);
        threadPool.setMaxThreads(maxThreads);
        server.setThreadPool(threadPool);


        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(listenPort);
        connector.setMaxIdleTime(60000);
        //connector.setConfidentialPort(8443);
        connector.setStatsOn(false);
        connector.setLowResourcesConnections(20000);
        connector.setLowResourcesMaxIdleTime(5000);
        connector.setAcceptors(acceptors);

        server.setConnectors(new Connector[]{connector});

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(dbg.client.EntranceController.class, "/dbgclient/entrance");
        handler.addServletWithMapping(dbg.edit.ConfirmController.class, "/dbgclient/confirm");
        handler.addServletWithMapping(dbg.client.ViewCacheController.class, "/dbgclient/viewCache");
        handler.addServletWithMapping(dbg.client.ViewTransHistoryController.class, "/dbgclient/viewTransHistory");
        handler.addServletWithMapping(dbg.client.GetTransStausController.class, "/dbgclient/getTransStatus");
        
        
        handler.addServletWithMapping(dbg.client.entrancedirectpmcidController.class, "/dbgclient/entrancedirectpmcid");
        handler.addServletWithMapping(dbg.client.confirmdirectpmcidController.class, "/dbgclient/confirmdirectpmcid");

        handler.addServletWithMapping(dbg.client.adderrorinfoController.class, "/dbgclient/adderrorinfo");
        handler.addServletWithMapping(dbg.client.showerrorinfocacheController.class, "/dbgclient/showerrorinfo");
        
        handler.addServletWithMapping(dbg.client.addcardlockedinfoController.class, "/dbgclient/addcardlockedinfo");
        handler.addServletWithMapping(dbg.client.showcardlockedinfoController.class, "/dbgclient/showcardlockedinfo");

        handler.addServletWithMapping(dbg.client.SmsEntranceController.class, "/dbgclient/smsentrance");
        handler.addServletWithMapping(dbg.client.SmsConfirmController.class, "/dbgclient/smsconfirm");
        handler.addServletWithMapping(dbg.client.SmsSendMOController.class, "/dbgclient/smssendmo");
        handler.addServletWithMapping(dbg.client.SmsSendMOResultController.class, "/dbgclient/smsmoresult");
        
        
        handler.addServletWithMapping(dbg.client.TestAddMoreController.class, "/dbgclient/testaddmore");
        
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setResourceBase(DbgTestToolConfig.SYSTEM_PUBLIC_PATH);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, handler});

        server.setHandler(handlers);
        server.setStopAtShutdown(true);
        server.setGracefulShutdown(1000);//1 giay se dong
        server.setSendServerVersion(false);

        ShutdownThread obj = new ShutdownThread(server);
        Runtime.getRuntime().addShutdownHook(obj);

        server.start();
        server.join();


    }
}
