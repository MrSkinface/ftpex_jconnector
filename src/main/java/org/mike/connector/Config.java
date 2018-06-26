package org.mike.connector;

import java.util.List;

import javax.xml.bind.annotation.*;

@XmlType
@XmlRootElement(name="config")
public class Config {

	@XmlElement(name="run_with_intervar")
	public RunWithInterval runWithInterval;

	@XmlElement
	public String server;

	@XmlElement
	public String login;

	@XmlElement
	public String password;

	@XmlElement
	public Direction inbound;

	@XmlElement
	public Direction outbound;
	
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
	
	@Override
	public String toString() {
		return "Folder [doctype=" + doctype + ", localPath=" + localPath + ", serverPath=" + serverPath + "]";
	}	
}