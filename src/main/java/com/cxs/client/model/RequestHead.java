package com.cxs.client.model;

public class RequestHead {

	private String service_code;// ":"200000010003",
	private String uid;// ":"e9b646ba2b2138ff7fd9a5b8e893abbd",
	// private String request_time;// ":"2017-04-20 16:06:54",
	private String encrypt = "none";// ":"AES",
	// private String signature;// ":"b79f9a1a10673c7104a90253c292ef24",
	private String version = "1.0.0";// ":"1.0.0"
	private String appSecret;
	
	// 附加属性
	private ClientRequestContext context;

	public RequestHead(ClientRequestContext context) {
		this.context = context;
		this.context.setHead(this);
	}

	public RequestHead serviceCode(String serviceCode) {
		this.service_code = serviceCode;
		return this;
	}

	public RequestHead appSecret(String appSecret) {
		this.appSecret = appSecret;
		return this;
	}
	
	public RequestHead encrypt(String encrypt) {
		this.encrypt = encrypt;
		return this;
	}
	
	public RequestHead encryptWithAES() {
		this.encrypt = "AES";
		return this;
	}

	public RequestHead uid(String uid) {
		this.uid = uid;
		return this;
	}

	public String getService_code() {
		return service_code;
	}

	public String getUid() {
		return uid;
	}

	public String getEncrypt() {
		return encrypt;
	}

	public String getVersion() {
		return version;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public ClientRequestContext getContext() {
		return context;
	}

	public RequestBody body() {
		return this.context.getRequestBody();
	}

}
