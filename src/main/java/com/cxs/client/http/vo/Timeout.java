package com.cxs.client.http.vo;

public class Timeout {

	private Integer connectTimeout;// 毫秒
	private String host;
	private Integer socketTimeout;// 毫秒

	public Timeout(String host, Integer socketTimeout, Integer connectTimeout) {
		super();
		this.host = host;
		this.socketTimeout = socketTimeout;
		this.connectTimeout = connectTimeout;
	}

	public Integer getConnectTimeout() {
		return connectTimeout;
	}

	public String getHost() {
		return host;
	}

	public Integer getSocketTimeout() {
		return socketTimeout;
	}

	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	@Override
	public String toString() {
		return "[host=" + host + ",connectTimeout=" + connectTimeout
				+ ", socketTimeout=" + socketTimeout + "]";
	}
}
