package media.platform.rmqmsgsim;

import media.platform.rmqmsgsim.service.ServiceManager;
import org.slf4j.LoggerFactory;

/**
 * @author dajin kim
 */
public class RmqMsgSimMain {

    public static void main(String[] args){

        LoggerFactory.getLogger(RmqMsgSimMain.class).info("RmqMsg Simulator Process Start");

        AppInstance instance = AppInstance.getInstance();
        instance.setConfigPath(args[0]);
        LoggerFactory.getLogger(RmqMsgSimMain.class).info("configPath:{}", args[0]);

        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.loop();

    }
}
