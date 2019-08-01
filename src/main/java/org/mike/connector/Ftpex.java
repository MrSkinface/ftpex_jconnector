package org.mike.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

public class Ftpex {

	private static final Logger log = Logger.getLogger(Ftpex.class);

	private Config conf;
	private FTPClient client;

	public Ftpex(Config conf) {
		this.conf = conf;
        connect();
	}

	public Map<String, byte[]> getFiles(String folder, String filter) {
        return getFiles(folder, filter, true);
	}

    private Map<String, byte[]> getFiles(String folder, String filter, boolean reconnect) {
        final Map<String, byte[]> files = new HashMap<>();
        try{
            checkConnection();
            changeSrvFolder(folder);
            for (String name : this.client.listNames()) {
                if(Pattern.matches(filter, name)) {
                    byte[]file = getFile(name);
                    if(file != null) {
                        files.put(name, file);
                    }
                }
            }
        } catch (Exception e) {
            if(reconnect){
                log.info("Connection lost.. try to reconnect ..");
                reConnect();
                return getFiles(folder, filter, false);
            } else {
                log.error(e);
            }
        }
        return files;
    }

    public void uploadFiles(String folder, Map<String,byte[]> files) {
        uploadFiles(folder, files, true);
    }

	private void uploadFiles(String folder, Map<String,byte[]> files, boolean reconnect) {
        try {
            checkConnection();
            changeSrvFolder(folder);
            for (final String name : files.keySet()) {
                uploadFile(name, files.get(name));
            }
        } catch (Exception e) {
            if(reconnect){
                log.info("Connection lost.. try to reconnect ..");
                reConnect();
                uploadFiles(folder, files, false);
            } else {
                log.error(e);
            }
        }
	}
	
	private byte[] getFile(String fileName) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
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

	public void removeFile(String fileName) {
		try {
			if(this.client.deleteFile(fileName))
                log.info("file ["+fileName+"] removed from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
		} catch (IOException e) {
			log.error(e);
		}
	}

	private void uploadFile(String fileName, byte[]file) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(file);
        this.client.setFileType(FTP.BINARY_FILE_TYPE);
        if(this.client.storeFile(fileName, bais))
            log.info("file ["+fileName+"] uploaded to "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
	}
	
	private void changeSrvFolder(String folder) throws Exception {
        while(!this.client.printWorkingDirectory().equals("/"))
            this.client.changeToParentDirectory();
        this.client.changeWorkingDirectory(folder);
	}

    public void close() {
        try {
            if(this.client != null)
                this.client.disconnect();
        } catch (Exception e){
            log.error(e);
        }
    }

    private void reConnect() {
        try {
            close();
            connect();
        } catch (Exception e){
            log.error(e);
        }
    }

    public void checkConnection() {
        if(this.client == null || !this.client.isConnected())
            connect();
    }

    public void connect(){
        try {
            this.client = new FTPClient();

            if(!this.client.isConnected()) {
                this.client.connect(this.conf.server);
                this.client.login(this.conf.login, this.conf.password);
            }
            if(conf.passive_mode){
                this.client.enterLocalPassiveMode();
                //this.client.enterRemotePassiveMode();
            }
            if(conf.debug){
                this.client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            }
        } catch (Exception e){
            log.error(e);
        }
    }

}
