package com.cxs.client.http.vo;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

public class HttpResult {

	
	private byte[] bytes;
	private String charset;
	private int code;
	private String data;
	private Header[] headers;
	private CookieStore cookieStore;
	private final List<Cookie> emptyList = new ArrayList<Cookie>();

	public HttpResult() {
		super();
	}

	public HttpResult(int code, String data,String charset, byte[] bytes) {
		super();
		this.code = code;
		this.data = data;
		this.charset = charset;
		this.bytes = bytes;
	}
	
	public HttpResult(int code,Header[] headers,CookieStore cookieStore, String data,String charset, byte[] bytes) {
		super();
		this.headers = headers;
		this.cookieStore = cookieStore;
		this.code = code;
		this.data = data;
		this.charset = charset;
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	public List<Cookie> getCookies() {
		if(cookieStore == null) {
			return emptyList;
		}
		return cookieStore.getCookies();
	}
	
	public String getCharset() {
		return charset;
	}

	public int getCode() {
		return code;
	}
	
	public Header[] getHeaders() {
		return headers;
	}

	public String getData() {
		if(data == null && charset != null) {
			try {
				data= new String(bytes,charset);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * 请求成功，响应为200
	 * @return
	 */
	public boolean success() {
		return code == HttpStatus.SC_OK;
	}

	@Override
	public String toString() {
		return "HttpResult [code=" + code + ", data=" + getData() + ", bytes="
				+ Arrays.toString(bytes) + "]";
	}


}
