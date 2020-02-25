package org.mike.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.log4j.Logger;

public class Ftpex {

	private static final Logger log = Logger.getLogger(Ftpex.class);

	private static final int MAX_RECONNECT_ATTEMPTS = 5;

	private Config conf;
	private FTPClient client;

	public Ftpex(final Config conf) {
		this.conf = conf;
	}

    void connect() throws Exception {
	    this.connect(1);
    }

    void connect(int attemptCounter) throws Exception {
        try {
            if(this.client == null)
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
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded");
            log.warn("Connection lost.. try to reconnect ["+attemptCounter+"] ..");
            this.connect(++attemptCounter);
        }
    }

    void close() {
        try {
            if(this.client != null)
                this.client.disconnect();
        } catch (Exception e){
            log.error(e);
        }
    }

    Map<String, byte[]> getFiles(final String folder, final String filter) throws Exception {
        final Map<String, byte[]> files = new HashMap<>();
        connect();
        changeSrvFolder(folder);
        for (final String name : this.client.listNames()) {
            if(Pattern.matches(filter, name)) {
                final byte[]file = getFile(name);
                if(file != null) {
                    files.put(name, file);
                }
            }
        }
        return files;
	}

    void uploadFiles(final String localFolder, final String srvFolder, final Map<String,byte[]> files) throws Exception {
        connect();
        changeSrvFolder(srvFolder);
        for (final String name : files.keySet()) {
            uploadFile(name, files.get(name));
            removeLocalFile(name, localFolder);
        }
    }

    private void removeLocalFile(final String name, final String path) {
        try {
            Files.delete(Paths.get(path).resolve(name));
            log.info("file ["+name+"] removed from ["+path+"] O.K.");
        } catch (IOException e) {
            log.error(e);
        }
    }
	
	private byte[] getFile(final String fileName) throws Exception {
        connect();
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.client.setFileType(FTP.BINARY_FILE_TYPE);
            this.client.retrieveFile(fileName, baos);
            if(baos.size() != 0) {
                log.info("file ["+fileName+"] downloaded from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
                return baos.toByteArray();
            }
        }
        return null;
	}

	void removeFile(final String fileName) throws Exception {
        connect();
        if(this.client.deleteFile(fileName))
            log.info("file ["+fileName+"] removed from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
	}

	private void uploadFile(final String fileName, final byte[]file) throws Exception {
        connect();
        try(final ByteArrayInputStream bais = new ByteArrayInputStream(file)){
            this.client.setFileType(FTP.BINARY_FILE_TYPE);
            if(this.client.storeFile(fileName, bais))
                log.info("file ["+fileName+"] uploaded to "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
        }
	}
	
	private void changeSrvFolder(final String folder) throws Exception {
        connect();
        while(!this.client.printWorkingDirectory().equals("/"))
            this.client.changeToParentDirectory();
        this.client.changeWorkingDirectory(folder);
	}

}
