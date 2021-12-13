package org.mike.connector;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlType
@XmlRootElement(name="config")
public class Config {

    @XmlAttribute
    public boolean debug;
	@XmlElement(name="run_with_intervar")
	public RunWithInterval runWithInterval;
	@XmlElement
	public String server;
	@XmlElement
	public boolean passive_mode = true;
	@XmlElement
	public boolean use_epsv;
	@XmlElement(name = "debug_ftp")
	public boolean debugFtp;
	@XmlElement
	public String login;
	@XmlElement
	public String password;
	@XmlElement
	public Direction inbound;
	@XmlElement
	public Direction outbound;

	public long intervalValue() {
	    return isDaemon() ? runWithInterval.value * 1000 : 0 ;
    }

	public boolean isDaemon() {
        return runWithInterval != null && runWithInterval.enabled;
	}

    public List<Folder> inboundFolders() {
	    if (inbound == null || inbound.folders == null) return Collections.emptyList();
        return this.inbound.folders;
    }

    public List<Folder> outboundFolders() {
        if (outbound == null || outbound.folders == null) return Collections.emptyList();
        return this.outbound.folders;
    }
	
	@Override
	public String toString() {
		return "Config [runWithInterval=" + runWithInterval + ", server=" + server + ", login=" + login
				+ ", password=" + password + ", inbound=" + inbound + ", outbound=" + outbound + "]";
	}	

}

@XmlAccessorType(XmlAccessType.FIELD)
class RunWithInterval {

	@XmlAttribute
	public boolean enabled;
	@XmlValue
	public long value;
	
	@Override
	public String toString() {
		return "RunWithInterval [enabled=" + enabled + ", value=" + value + "]";
	}	
	
}
@XmlAccessorType(XmlAccessType.FIELD)
class CyrilicRemove {

	@XmlAttribute
	public boolean enabled;

	@Override
	public String toString() {
		return "CyrilicRemove [enabled=" + enabled + "]";
	}	
	
}
@XmlAccessorType(XmlAccessType.FIELD)
class Direction {

	@XmlElement(name="folder")
	public List<Folder>folders;

	@Override
	public String toString() {
		return "Direction [folders=" + folders + "]";
	}	
}

@XmlAccessorType(XmlAccessType.FIELD)
class Folder {

	@XmlElement
	public String doctype;
	@XmlElement
	public String localPath;
	@XmlElement
	public String serverPath;
    @XmlElement(name="contentMatchRule")
    public List<ContentMatchingRule> rules;
	
	@Override
	public String toString() {
		return "Folder [doctype=" + doctype + ", localPath=" + localPath + ", serverPath=" + serverPath + "]";
	}	
}

@XmlAccessorType(XmlAccessType.FIELD)
class ContentMatchingRule {

    @XmlElement
    public String field;
    @XmlElement
    public String pattern;

    @Override
    public String toString() {
        return "ContentMatchingRule [field=" + field + ", pattern=" + pattern + "]";
    }
}