package com.cxs.client.http.vo;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;

public class MyHttpClient {

	private CloseableHttpClient httpClient;
	private CookieStore cookieStore;

	public MyHttpClient() {
		super();
	}
	
	public MyHttpClient(CloseableHttpClient httpClient,
			CookieStore cookieStore) {
		super();
		this.httpClient = httpClient;
		this.cookieStore = cookieStore;
	}
	
	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	public CookieStore getCookieStore() {
		return cookieStore;
	}

}
