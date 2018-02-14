package org.mike.connector;

import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

public class Connector {

    private static final Logger log=Logger.getLogger(Connector.class);

    public static Config config;

    public Connector() {
        do {
            log.info("start");
            new Handler();
            log.info("end");
            try {
                if(isDaemon()){
                    Thread.sleep(config.runWithInterval.value * 1000);
                }
            } catch (Exception e) {
                log.debug(e);
            }

        } while (isDaemon());
    }

    private static void loadConfig(String path) throws JAXBException, FileNotFoundException {
        JAXBContext jc;
        jc=JAXBContext.newInstance(Config.class);
        Unmarshaller um=jc.createUnmarshaller();
        config = (Config) um.unmarshal(new FileInputStream(Paths.get(path).toString()));
    }

    private boolean isDaemon(){
        try{
            return Boolean.valueOf(config.runWithInterval.enabled);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            if(args != null && args.length > 0){
                loadConfig(args[0]);
            } else {
                loadConfig("config/folders-config.xml");
            }
        } catch (JAXBException e) {
            log.error(e);
            System.exit(12);
        } catch (FileNotFoundException e) {
            try{
                loadConfig("config/folders-config.xml");
            } catch (Exception e1){
                log.error(e1);
                System.exit(13);
            }
        }
        new Connector();
    }

}
