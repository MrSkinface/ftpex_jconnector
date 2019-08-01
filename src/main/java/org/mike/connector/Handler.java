package org.mike.connector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Handler {

	private static final Logger log = Logger.getLogger(Handler.class);

	private final Config config;
	private final Ftpex ftpex;
	
	public Handler(Config config) {
	    this.config = config;
		this.ftpex = new Ftpex(this.config);
		processInbound();
		processOutbound();
        this.ftpex.close();
	}
	
	private void processInbound() {

		for (final Folder folder : config.inboundFolders()) {
            final Map<String, byte[]> result = ftpex.getFiles(folder.serverPath, folder.doctype);
			log.info("Got [" + result.size() + "] files for type [" + folder.doctype + "] from server");
			writeResult(folder, result);
		}
		
	}
	private void processOutbound() {
		for (final Folder folder : config.outboundFolders()) {
            final Map<String, byte[]> files = prepareFiles(folder);
            log.info("Got [" + files.size() + "] files for type [" + folder.doctype + "] from local path");
			ftpex.uploadFiles(folder.serverPath, files);
		}
	}

	private void writeResult(Folder folder, Map<String, byte[]>result) {
		for (final String name : result.keySet()) {
			try {
                checkIfExist(Paths.get(folder.localPath));
				Files.write(Paths.get(folder.localPath).resolve(name), result.get(name));
				log.info("file ["+name+"] saved to ["+folder.localPath+"]");
                ftpex.removeFile(name);
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

	private Map<String, byte[]> prepareFiles(Folder folder) {
        final Map<String, byte[]>files = new HashMap<>();
		File dir = new File(folder.localPath);
		for (String name : dir.list((dir1, name1) -> Pattern.matches(folder.doctype, name1))) {
			try {
				files.put(name.replaceAll("[а-яА-ЯёЁ ]", ""), Files.readAllBytes(Paths.get(folder.localPath).resolve(name)));
				log.info("file [" + name + "] extracted from [" + folder.localPath + "]");
				removeLocalFile(name, folder.localPath);
			} catch (IOException e) {
				log.error(e);
			}
		}
		return files;
	}

	private void removeLocalFile(String name, String path) {
		try {
			Files.delete(Paths.get(path).resolve(name));
			log.info("file ["+name+"] removed from ["+path+"] O.K.");
		} catch (IOException e) {
			log.error(e);
		}
	}

    private void checkIfExist(Path path) {
        try {
            if(!Files.exists(path))
                Files.createDirectories(path);
        } catch (Exception e){
            log.error(e);
        }
    }
}
