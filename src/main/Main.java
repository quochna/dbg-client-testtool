package main;

import java.io.File;
import com.vng.jcore.common.LogUtil;
import dbg.client.DbgTestToolConfig;
import httpservice.WebServer;
import org.apache.log4j.Logger;

public class Main {

    private static Logger ClassLogger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try {
            LogUtil.init();
            InitBusiness();
           

            String pidFile = System.getProperty("pidfile");
            if (pidFile != null) {
                new File(pidFile).deleteOnExit();
            }

//            if (System.getProperty("foreground") == null) {
//                System.out.close();
//                System.err.close();
//            }

            WebServer webserver = new WebServer();
            webserver.start();

        } catch (Throwable e) {
            ClassLogger.error("Exception at start up: " + e.getMessage());
            System.exit(3);
        }
    }

    public static void InitBusiness() {
        // Init cac business khoi dau de chay nhanh		
        if (DbgTestToolConfig.loadConfigs() == false) {
            System.exit(1);
        }
    }
}
