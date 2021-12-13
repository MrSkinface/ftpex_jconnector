package org.mike.connector;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

public class Connector {

    private static final Logger log = LogManager.getLogger();
    private final Ftpex ftpex;

    private static Properties properties;

    public Connector(final Config config) {
        this.ftpex = new Ftpex(config);
        do {
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("pom-properties.properties");
                properties = new Properties();
                properties.load(is);

                log.info("start [{}]", properties.get("connector.version"));
                ftpex.connect();
                // inbound
                for (final Folder folder : config.inboundFolders()) {
                    final Map<String, byte[]> result = ftpex.getFiles(folder.serverPath, folder.doctype, folder.rules);
                    if (result != null) {
                        log.info("Got [{}] files for type [{}] from server [{}]", result.size(), folder.doctype, folder.serverPath);
                        writeResult(folder, result);
                    }
                }

                // outbound
                for (final Folder folder : config.outboundFolders()) {
                    final File dir = new File(folder.localPath);
                    final String[] listNames = Objects.requireNonNull(dir.list((dir1, name1) -> Pattern.matches(folder.doctype, name1)));
                    log.info("Got [{}] files for type [{}] from local path [{}]", listNames.length, folder.doctype, folder.localPath);
                    ftpex.changeSrvFolder(folder.serverPath);
                    for (final String name : listNames) {
                        try {
                            ftpex.uploadFile(name.replaceAll("[а-яА-ЯёЁ ]", ""), Files.readAllBytes(Paths.get(folder.localPath).resolve(name)));
                            removeLocalFile(name, folder.localPath);
                        } catch (IOException e) {
                            log.error(e);
                        }
                    }
                }
                log.info("end");
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage(), e);
            } finally {
                ftpex.close();
            }
            sleep(config.intervalValue());
        } while (config.isDaemon());
    }

    private void removeLocalFile(final String name, final String path) {
        try {
            Files.delete(Paths.get(path).resolve(name));
            log.info("file [{}] removed from [{}] O.K.", name, path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void writeResult(final Folder folder, final Map<String, byte[]> result) {
        for (final String name : result.keySet()) {
            try {
                final Path path = Paths.get(folder.localPath);
                if (!Files.exists(path))
                    Files.createDirectories(path);

                Files.write(Paths.get(folder.localPath).resolve(name), result.get(name));
                log.info("file [{}] saved to [{}]", name, folder.localPath);
                ftpex.removeFile(name);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final String configFile = (args != null && args.length > 0) ? args[0] : "config/folders-config.xml" ;
        final Config config = initConfig(configFile);
        Configurator.setRootLevel(config.debug ? Level.ALL : Level.INFO);
        new Connector(config);
    }

    private static Config initConfig(final String path) throws JAXBException, FileNotFoundException {
        Unmarshaller um = JAXBContext.newInstance(Config.class).createUnmarshaller();
        return (Config)um.unmarshal(new FileInputStream(Paths.get(path).toString()));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            log.debug(e);
        }
    }

}
