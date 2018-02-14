package org.mike.connector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Handler {
	private static final Logger log=Logger.getLogger(Handler.class);

	private Ftpex ftpex;
	
	public Handler() {
		this.ftpex=new Ftpex(Connector.config);
		processInbound();
		processOutbound();
	}
	
	private void processInbound() {

		for (Folder folder : getInboundFoldersConfig()) {
			HashMap<String, byte[]> result = this.ftpex.getFiles(folder.serverPath, folder.doctype);
			log.info("Got [" + result.size() + "] files for type [" + folder.doctype + "] from server");
			writeResult(folder, result);
		}
		
	}
	private void processOutbound() {

		for (Folder folder : getOutboundFoldersConfig()) {
            Map<String, byte[]> files = prepareFiles(folder);
            log.info("Got [" + files.size() + "] files for type [" + folder.doctype + "] from local path");
			this.ftpex.uploadFiles(folder.serverPath, files);
		}
	}
	
	/*
	 * helpers
	 * 
	 * */

    private List<Folder> getInboundFoldersConfig(){
        try{
            return Connector.config.inbound.folders;
        } catch (NullPointerException e){
            log.info("config does not contains inbound settings");
            return new LinkedList<>();
        }
    }
    private List<Folder> getOutboundFoldersConfig(){
        try{
            return Connector.config.outbound.folders;
        } catch (NullPointerException e){
            log.info("config does not contains outbound settings");
            return new LinkedList<>();
        }
    }

	private void writeResult(Folder folder,HashMap<String, byte[]>result) {

		for (String name : result.keySet()) {
			try {
                checkIfExist(Paths.get(folder.localPath));
				Files.write(Paths.get(folder.localPath).resolve(name), result.get(name));
				log.info("file ["+name+"] saved to ["+folder.localPath+"]");
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

	private HashMap<String,byte[]>prepareFiles(Folder folder) {

		HashMap<String,byte[]>files=new HashMap<>();
        Paths.get(folder.localPath);
		File dir=new File(folder.localPath);
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

	private boolean removeLocalFile(String name,String path) {
		try {
			Files.delete(Paths.get(path).resolve(name));
			log.info("file ["+name+"] removed from ["+path+"] O.K.");
			return true;
		} catch (IOException e) {
			log.error(e);
		}
		return false;
	}

    private void checkIfExist(Path path) {
        try{
            if(!Files.exists(path)){
                Files.createDirectories(path);
            }
        }catch (Exception e){
            log.error(e);
        }
    }
}
