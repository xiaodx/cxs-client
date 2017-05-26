package com.cxs.client.http;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.cookie.Cookie;

import com.cxs.client.http.vo.FileInput;
import com.cxs.client.http.vo.HttpRequestMeta;
import com.cxs.client.http.vo.HttpResult;
public interface HttpClientService {

	void setProtocolVersion(HttpVersion httpVersion);

	void setNtCredentials(NTCredentials ntCredentials);

	void setPoolMaxPerRoute(Integer poolMaxPerRoute);

	void setPoolMaxTotal(Integer poolMaxTotal);

	//
	void setHttpClientSingleton(Boolean singleton);

	/**
	 * 是否启用cookie store
	 * @param enable
	 */
	void setEnableCookieStore(Boolean enable);

	Boolean getEnableCookieStore();

	List<Cookie> getCookies(String host);

	void clearCookies(String host);

	void addCookie(String host,String name,String value,String path,Integer version);

	void setCookieInHeader(String cookie);

	String getCookieInHeader();

	/**
	 * 添加https证书
	 *
	 * @param host
	 * @param keyStoreType
	 * @param filePath
	 * @param password
	 */
	void addHttpsCert(String host, String keyStoreType,String filePath, String password);

	void closeResponse(CloseableHttpResponse response);

	<T> List<T> batRequest(List<Callable<T>> requests, ExecutorService executorService, AtomicInteger idx);

	HttpResult doHttp(HttpClientService httpClientService, HttpRequestMeta httpRequestMeta);

	HttpResult get(String url);

	HttpResult get(String url, HttpHost proxy);

	HttpResult get(String url, HttpHost proxy, Map<String, String> params);

	HttpResult get(String url, Map<String, String> headers, Map<String, String> params);

	HttpResult get(String url, Map<String, String> headers, Map<String, String> params, String sendEncoding);

	HttpResult get(String uri, HttpHost proxy, Map<String, String> headers, Map<String, String> params, String sendEncoding);

	String getHost(String hostOrUrl);

	HttpResult getWithHeaders(String url, Map<String, String> headers);

	HttpResult getWithParameters(String url, Map<String, String> params);

	HttpResult post(String url);

	HttpResult post(String url, HttpHost proxy, Map<String, String> headers, Map<String, String> params);

	HttpResult post(String url, HttpHost proxy, Map<String, String> headers, Map<String, String> params, String sendEncoding, FileInput... files);

	HttpResult post(String url, Map<String, String> headers, Map<String, String> params);

	HttpResult postFile(String url, FileInput... files);

	HttpResult postFile(String url, Map<String, String> params, FileInput... files);

	HttpResult postString(String url, HttpHost proxy, Map<String, String> headers, String stringValue, String sendEncoding);

	HttpResult getLastResult();

	HttpResult postWithHeaders(String url, Map<String, String> headers);

	HttpResult postWithParameters(String url, Map<String, String> params);

	/**
	 * 设置超时,单位为毫秒
	 *
	 * @param host
	 *            访问服务器的host,如www.baidu.com
	 * @param socketTimeout
	 *            请求超时|读取超时
	 * @param connectTimeout
	 *            连接超时
	 */
	void setTimeoutMillis(String host, int socketTimeout, int connectTimeout);

	/**
	 * 设置超时,单位为秒
	 *
	 * @param host
	 *            访问服务器的host,如www.baidu.com
	 * @param socketTimeout
	 *            请求超时|读取超时
	 * @param connectTimeout
	 *            连接超时
	 */
	void setTimeoutSecond(String host, int socketTimeout, int connectTimeout);

	public Integer getConnectTimeout();

	void setConnectTimeout(Integer connectTimeout);

	String getDefaultCharset();

	void setDefaultCharset(String defaultCharset);

	Integer getSocketTimeout();

	void setSocketTimeout(Integer socketTimeout);

	void removeHttpClient(String host);

}
