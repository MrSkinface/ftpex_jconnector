package org.mike.connector;

import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class Connector {

    private static final Logger log = Logger.getLogger(Connector.class);
    private final Ftpex ftpex;

    public Connector(final Config config) {
        this.ftpex = new Ftpex(config);
        do {
            try {
                log.info("start");
                ftpex.connect();

                // inbound
                for (final Folder folder : config.inboundFolders()) {
                    final Map<String, byte[]> result = ftpex.getFiles(folder.serverPath, folder.doctype);
                    if(result != null) {
                        log.info("Got [" + result.size() + "] files for type [" + folder.doctype + "] from server [" + folder.serverPath + "]");
                        writeResult(folder, result);
                    }
                }

                // outbound
                for (final Folder folder : config.outboundFolders()) {
                    final Map<String, byte[]> files = prepareFiles(folder);
                    log.info("Got [" + files.size() + "] files for type [" + folder.doctype + "] from local path [" + folder.localPath + "]");
                    ftpex.uploadFiles(folder.localPath, folder.serverPath, files);
                }

                log.info("end");
            } catch (Exception e){
                e.printStackTrace();
                log.error(e);
            } finally {
                ftpex.close();
            }
            sleep(config.intervalValue());
        } while (config.isDaemon());
    }

    private void writeResult(final Folder folder, final Map<String, byte[]> result) {
        for (final String name : result.keySet()) {
            try {
                final Path path = Paths.get(folder.localPath);
                if(!Files.exists(path))
                    Files.createDirectories(path);

                Files.write(Paths.get(folder.localPath).resolve(name), result.get(name));
                log.info("file ["+name+"] saved to ["+folder.localPath+"]");
                ftpex.removeFile(name);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private Map<String, byte[]> prepareFiles(final Folder folder) {
        final Map<String, byte[]>files = new HashMap<>();
        File dir = new File(folder.localPath);
        for (String name : Objects.requireNonNull(dir.list((dir1, name1) -> Pattern.matches(folder.doctype, name1)))) {
            try {
                files.put(name.replaceAll("[а-яА-ЯёЁ ]", ""), Files.readAllBytes(Paths.get(folder.localPath).resolve(name)));
                log.info("file [" + name + "] extracted from [" + folder.localPath + "]");
            } catch (IOException e) {
                log.error(e);
            }
        }
        return files;
    }

    public static void main(String[] args) throws Exception {
        final String configFile = (args != null && args.length > 0) ? args[0] : "config/folders-config.xml" ;
        new Connector(initConfig(configFile));
    }

    private static Config initConfig(final String path) throws JAXBException, FileNotFoundException {
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
