package org.mike.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mike.connector.utils.Utils;

public class Ftpex {

    private static final Logger log = LogManager.getLogger();

	private static final int MAX_RECONNECT_ATTEMPTS = 5;

	private Config conf;
	private FTPClient client;

	public Ftpex(final Config conf) {
		this.conf = conf;
	}

    void connect() throws Exception {
        this._connect(1, false);
    }

    private void _connect(int attemptCounter, final boolean forceReconnect) throws Exception {
        try {
            if (this.client == null || forceReconnect)
                this.client = new FTPClient();

            if (forceReconnect) this.close();

            if (!this.client.isConnected() || forceReconnect) {
                this.client.connect(this.conf.server);
                this.client.login(this.conf.login, this.conf.password);
            }
            if (conf.passive_mode){
                this.client.enterLocalPassiveMode();
            }
            if (conf.use_epsv){
                this.client.setUseEPSVwithIPv4(true);
            }
            if (conf.debugFtp){
                this.client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            }
        } catch (ConnectException | FTPConnectionClosedException e) {
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_connect]");
            log.warn("Connection lost [_connect] .. try to reconnect [{}] ..", attemptCounter);
            this._connect(++attemptCounter, true);
        }
    }

    void close() {
        try {
            if (this.client != null)
                this.client.disconnect();
        } catch (Exception e){
            log.error(e.getMessage(), e);
        }
    }

    Map<String, byte[]> getFiles(final String folder, final String filter, final List<ContentMatchingRule> rules) throws Exception {
        _connect(1, true);
        return _getFiles(folder, filter, rules, 1);
	}

    private Map<String, byte[]> _getFiles(final String folder, final String filter, final List<ContentMatchingRule> rules, int attemptCounter) throws Exception {
        final Map<String, byte[]> files = new HashMap<>();
	    try {
            changeSrvFolder(folder);
            for (final String name : this.client.listNames()) {
                final boolean fileNameMatch = Pattern.matches(filter, name);
                log.debug("filename [{}] matches pattern [{}]: {}", name, filter, fileNameMatch);
                if (fileNameMatch) {
                    final byte[] file = getFile(name);
                    if (file != null) {
                        if (this.contentMatchesRules(file, rules))
                            files.put(name, file);
                    }
                }
            }
        } catch (ConnectException | FTPConnectionClosedException e) {
            if (attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_getFiles]");
            log.warn("Connection lost [_getFiles] .. try to reconnect [{}] ..", attemptCounter);
            this._connect(++attemptCounter, true);
            return this._getFiles(folder, filter, rules, ++attemptCounter);
        }
        return files;
    }

    private boolean contentMatchesRules(byte[] file, List<ContentMatchingRule> rules) {
	    if (rules == null) {
	        log.debug("Content matching rules are missing");
            return true;
        }
	    boolean result = true;
	    for (final ContentMatchingRule rule : rules) {
	        final String xpathValue = Utils.fromXmlNode(file, rule.field);
	        final boolean contentMatch = Pattern.matches(rule.pattern, xpathValue);
	        log.debug("content [{}] value [{}] matches pattern [{}]: {}", rule.field, xpathValue, rule.pattern, contentMatch);
            result &= contentMatch;
        }
	    return result;
    }
	
	private byte[] getFile(final String fileName) throws Exception {
        connect();
        return _getFile(fileName, 1);
	}

    private byte[] _getFile(final String fileName, int attemptCounter) throws Exception {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.client.setFileType(FTP.BINARY_FILE_TYPE);
            this.client.retrieveFile(fileName, baos);
            if (baos.size() != 0) {
                log.info("file [{}] downloaded from {}::{}::{}", fileName, this.client.getRemoteAddress(), this.conf.login, this.client.printWorkingDirectory());
                return baos.toByteArray();
            }
        } catch (ConnectException | FTPConnectionClosedException e) {
            if (attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_getFile]");
            log.warn("Connection lost [_getFile] .. try to reconnect [{}] ..", attemptCounter);
            this._connect(++attemptCounter, true);
            return this._getFile(fileName, ++attemptCounter);
        }
        return null;
    }

	void removeFile(final String fileName) throws Exception {
        connect();
        _removeFile(fileName, 1);
	}

    private void _removeFile(final String fileName, int attemptCounter) throws Exception {
	    try {
            if (this.client.deleteFile(fileName))
                log.info("file [{}] removed from {}::{}::{}", fileName, this.client.getRemoteAddress(), this.conf.login, this.client.printWorkingDirectory());
        } catch (ConnectException | FTPConnectionClosedException e) {
            if (attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_removeFile]");
            log.warn("Connection lost [_removeFile] .. try to reconnect [{}] ..", attemptCounter);
            this._connect(++attemptCounter, true);
            this._removeFile(fileName, ++attemptCounter);
        }
    }

    void uploadFile(final String fileName, final byte[] file) throws Exception {
        connect();
        _uploadFile(fileName, file, 1);
	}

    private void _uploadFile(final String fileName, final byte[]file, int attemptCounter) throws Exception {
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(file)) {
            this.client.setFileType(FTP.BINARY_FILE_TYPE);
            if (this.client.storeFile(fileName, bais))
                log.info("file [{}] uploaded to {}::{}::{}", fileName, this.client.getRemoteAddress(), this.conf.login, this.client.printWorkingDirectory());
        } catch (ConnectException | FTPConnectionClosedException e) {
            if (attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_uploadFile]");
            log.warn("Connection lost [_uploadFile] .. try to reconnect [{}] ..", attemptCounter);
            this._connect(++attemptCounter, true);
            this._uploadFile(fileName, file, ++attemptCounter);
        }
    }
	
	void changeSrvFolder(final String folder) throws Exception {
        connect();
        _changeSrvFolder(folder, 1);
	}

    private void _changeSrvFolder(final String folder, int attemptCounter) throws Exception {
        try {
            while (!this.client.printWorkingDirectory().equals("/"))
                this.client.changeToParentDirectory();
            this.client.changeWorkingDirectory(folder);
        } catch (ConnectException | FTPConnectionClosedException e) {
            if(attemptCounter >= MAX_RECONNECT_ATTEMPTS)
                throw new Exception("Maximum reconnects exceeded [_changeSrvFolder]");
            log.warn("Connection lost [_changeSrvFolder] .. try to reconnect [{}] ..", attemptCounter);
            this._connect(++attemptCounter, true);
            this._changeSrvFolder(folder, ++attemptCounter);
        }
    }

}
