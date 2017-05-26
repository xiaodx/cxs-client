package com.cxs.client.http.vo;

public class CertInfo {

	private String filePath;
	private String host;
	private String keyStoreType;
	private String password;
	
	public CertInfo(String host, String keyStoreType, String filePath,
			String password) {
		super();
		this.host = host;
		this.keyStoreType = keyStoreType;
		this.filePath = filePath;
		this.password = password;
	}
	
	public String getFilePath() {
		return filePath;
	}

	public String getHost() {
		return host;
	}

	public String getKeyStoreType() {
		return keyStoreType;
	}
	public String getPassword() {
		return password;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	

}
