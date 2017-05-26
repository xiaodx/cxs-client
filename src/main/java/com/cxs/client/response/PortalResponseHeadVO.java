package com.cxs.client.response;

import java.io.Serializable;

public class PortalResponseHeadVO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9144205026639738433L;
	private String code;
	private String message;
	private String encrypt;
//	private String requestId;
	
	public PortalResponseHeadVO() {
		
	}

	public PortalResponseHeadVO(String code, String message, String encrypt) {
		super();
		this.code = code;
		this.message = message;
		this.encrypt = encrypt;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}


	public String getEncrypt() {
		return encrypt;
	}

	public void setEncrypt(String encrypt) {
		this.encrypt = encrypt;
	}

}
