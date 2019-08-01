package org.mike.connector;

import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

public class Connector {

    private static final Logger log = Logger.getLogger(Connector.class);

    public Connector(Config config) {
        do {
            log.info("start");
            new Handler(config);
            log.info("end");
            sleep(config.intervalValue());
        } while (config.isDaemon());
    }

    public static void main(String[] args) throws Exception {
        final String configFile = (args != null && args.length > 0) ? args[0] : "config/folders-config.xml" ;
        new Connector(initConfig(configFile));
    }

    private static Config initConfig(String path) throws JAXBException, FileNotFoundException {
        Unmarshaller um = JAXBContext.newInstance(Config.class).createUnmarshaller();
        return (Config)um.unmarshal(new FileInputStream(Paths.get(path).toString()));
    }

    private static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            log.debug(e);
        }
    }

}
