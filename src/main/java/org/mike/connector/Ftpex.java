package org.mike.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class Ftpex {

	private static final Logger log=Logger.getLogger(Ftpex.class);
	
	private final String host="ftpex.edi.su";
	/*private final String host="ftpex.ua.int";*/
	private Config conf;
	private FTPClient client;
	
	public Ftpex(Config conf) {

		this.conf=conf;
		this.client=new FTPClient();
		try {

			this.client.connect(this.host);
			this.client.login(this.conf.login, this.conf.password);
		} catch (SocketException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
	}
	
	public HashMap<String, byte[]>getFiles(String folder,String filter) {

		HashMap<String, byte[]>files = new HashMap<>();
		try {
			if(!this.client.isConnected()) {
				this.client.connect(this.host);
				this.client.login(this.conf.login, this.conf.password);
			}
			changeSrvFolder(folder);
			for (String name : this.client.listNames()) {
				if(Pattern.matches(filter, name)) {
					byte[]file=getFile(name);
					if(file!=null) {
						files.put(name, file);
						removeFile(name);
					}	
				}								
			}
		} catch (Exception e) {
			log.error(e);
		}
        return files;
	}

	public void uploadFiles(String folder, Map<String,byte[]> files) {
		try {
			if(!this.client.isConnected()) {
				this.client.connect(this.host);
				this.client.login(this.conf.login, this.conf.password);
			}			
			changeSrvFolder(folder);
			for (String name : files.keySet()) {
				uploadFile(name, files.get(name));					
			}
		} catch (IOException e) {
			log.error(e);
		}		
	}
	
	private byte[] getFile(String fileName) {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try {
			this.client.setFileType(FTP.BINARY_FILE_TYPE);
			this.client.retrieveFile(fileName, baos);
			if(baos.size() != 0) {
				log.info("file ["+fileName+"] downloaded from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
				return baos.toByteArray();
			}
		} catch (IOException e) {
			log.error(e);
		}
		return null;
	}

	private boolean removeFile(String fileName) {
		try {
			if(this.client.deleteFile(fileName)) {
				log.info("file ["+fileName+"] removed from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
				return true;
			}			
		} catch (IOException e) {
			log.error(e);
		}
		return false;
	}

	private boolean uploadFile(String fileName,byte[]file) {
		ByteArrayInputStream bais=new ByteArrayInputStream(file);
		try {
			this.client.setFileType(FTP.BINARY_FILE_TYPE);
			if(this.client.storeFile(fileName, bais)) {
				log.info("file ["+fileName+"] uploaded to "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
				return true;
			}				
		} catch (IOException e) {
			log.error(e);
		}
		return false;
	}
	
	private void changeSrvFolder(String folder) {
		try {
			while(!this.client.printWorkingDirectory().equals("/"))
				this.client.changeToParentDirectory();
			this.client.changeWorkingDirectory(folder);
		} catch (Exception e) {
			log.error(e);
		}
	}

}
