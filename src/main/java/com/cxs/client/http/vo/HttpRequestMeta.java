package com.cxs.client.http.vo;

import java.util.Map;

public class HttpRequestMeta {

	private String url;
	private Map<String, String> params;
	private Map<String, String> header;
	private String method;// POST|GET
	private String charset;
	
	public HttpRequestMeta(String url, Map<String, String> params,
			Map<String, String> header, String method, String charset) {
		super();
		this.url = url;
		this.params = params;
		this.header = header;
		this.method = method;
		if(charset == null) {
			charset = "utf-8";
		}
		this.charset = charset;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Map<String, String> getHeader() {
		return header;
	}

	public void setHeader(Map<String, String> header) {
		this.header = header;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

}
