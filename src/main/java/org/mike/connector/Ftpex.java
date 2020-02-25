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
	    this._connect(1);
    }

    void _connect(int attemptCounter) throws Exception {
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
                throw new Exception("Maximum reconnects exceeded [_connect]");
            log.warn("Connection lost [_connect] .. try to reconnect ["+attemptCounter+"] ..");
            this._connect(++attemptCounter);
        }
    }

    void close() {
        try {
            if(this.client != null)
                this.client.disconnect();
        } catch (Exception e){
            log.error(e.getMessage(), e);
        }
    }

    Map<String, byte[]> getFiles(final String folder, final String filter) throws Exception {
        connect();
        return _getFiles(folder, filter, 1);
	}

    Map<String, byte[]> _getFiles(final String folder, final String filter, int attemptCounter) throws Exception {
        final Map<String, byte[]> files = new HashMap<>();
	    try{
            changeSrvFolder(folder);
            for (final String name : this.client.listNames()) {
                if(Pattern.matches(filter, name)) {
                    final byte[]file = getFile(name);
                    if(file != null) {
                        files.put(name, file);
                    }
                }
            }
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_getFiles]");
            log.warn("Connection lost [_getFiles] .. try to reconnect ["+attemptCounter+"] ..");
            return this._getFiles(folder, filter, ++attemptCounter);
        }
        return files;
    }

    void uploadFiles(final String localFolder, final String srvFolder, final Map<String,byte[]> files) throws Exception {
        connect();
        _uploadFiles(localFolder, srvFolder, files, 1);
    }

    private void _uploadFiles(final String localFolder, final String srvFolder, final Map<String,byte[]> files, int attemptCounter) throws Exception {
	    try{
            changeSrvFolder(srvFolder);
            for (final String name : files.keySet()) {
                uploadFile(name, files.get(name));
                removeLocalFile(name, localFolder);
            }
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_uploadFiles]");
            log.warn("Connection lost [_uploadFiles] .. try to reconnect ["+attemptCounter+"] ..");
            this._uploadFiles(localFolder, srvFolder, files, ++attemptCounter);
        }
    }

    private void removeLocalFile(final String name, final String path) {
        try {
            Files.delete(Paths.get(path).resolve(name));
            log.info("file ["+name+"] removed from ["+path+"] O.K.");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
	
	private byte[] getFile(final String fileName) throws Exception {
        connect();
        return _getFile(fileName, 1);
	}

    private byte[] _getFile(final String fileName, int attemptCounter) throws Exception {
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.client.setFileType(FTP.BINARY_FILE_TYPE);
            this.client.retrieveFile(fileName, baos);
            if(baos.size() != 0) {
                log.info("file ["+fileName+"] downloaded from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
                return baos.toByteArray();
            }
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_getFile]");
            log.warn("Connection lost [_getFile] .. try to reconnect ["+attemptCounter+"] ..");
            return this._getFile(fileName, ++attemptCounter);
        }
        return null;
    }

	void removeFile(final String fileName) throws Exception {
        connect();
        _removeFile(fileName, 1);
	}

    void _removeFile(final String fileName, int attemptCounter) throws Exception {
	    try{
            if(this.client.deleteFile(fileName))
                log.info("file ["+fileName+"] removed from "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_removeFile]");
            log.warn("Connection lost [_removeFile] .. try to reconnect ["+attemptCounter+"] ..");
            this._removeFile(fileName, ++attemptCounter);
        }
    }

	private void uploadFile(final String fileName, final byte[] file) throws Exception {
        connect();
        _uploadFile(fileName, file, 1);
	}

    private void _uploadFile(final String fileName, final byte[]file, int attemptCounter) throws Exception {
        try(final ByteArrayInputStream bais = new ByteArrayInputStream(file)){
            this.client.setFileType(FTP.BINARY_FILE_TYPE);
            if(this.client.storeFile(fileName, bais))
                log.info("file ["+fileName+"] uploaded to "+this.client.getRemoteAddress()+"::"+this.conf.login+"::"+this.client.printWorkingDirectory());
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_uploadFile]");
            log.warn("Connection lost [_uploadFile] .. try to reconnect ["+attemptCounter+"] ..");
            this._uploadFile(fileName, file, ++attemptCounter);
        }
    }
	
	private void changeSrvFolder(final String folder) throws Exception {
        connect();
        _changeSrvFolder(folder, 1);
	}

    private void _changeSrvFolder(final String folder, int attemptCounter) throws Exception {
        try{
            while(!this.client.printWorkingDirectory().equals("/"))
                this.client.changeToParentDirectory();
            this.client.changeWorkingDirectory(folder);
        } catch (ConnectException | FTPConnectionClosedException e){
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_changeSrvFolder]");
            log.warn("Connection lost [_changeSrvFolder] .. try to reconnect ["+attemptCounter+"] ..");
            this._changeSrvFolder(folder, ++attemptCounter);
        }
    }

}
