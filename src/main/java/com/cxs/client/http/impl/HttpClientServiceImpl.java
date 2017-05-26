package com.cxs.client.http.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.SetCookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.cxs.client.http.HttpClientService;
import com.cxs.client.http.vo.CertInfo;
import com.cxs.client.http.vo.FileInput;
import com.cxs.client.http.vo.HttpRequestMeta;
import com.cxs.client.http.vo.HttpResult;
import com.cxs.client.http.vo.MyHttpClient;
import com.cxs.client.http.vo.Timeout;

public class HttpClientServiceImpl implements HttpClientService {

	private static final Logger LOG = Logger.getLogger(HttpClientServiceImpl.class);

	private Integer connectTimeout = 10000;// 连接超时（连接到服务器的时间）
	private Integer socketTimeout = 30000;//请求超时|读取超时（获取服务器响应的时间）
	private String defaultCharset = "utf-8";
	private Integer poolMaxTotal = 1;//连接池最大并发连接数
	private Integer poolMaxPerRoute = 1;//单路由最大并发数
	private HttpVersion httpVersion = HttpVersion.HTTP_1_1;
	private HttpResult lastResult;//最后一次请求结果

	private Boolean singleton = false;

//private final Map<String, Long> loserCookies = new ConcurrentHashMap<String, Long>();

	private final Map<String, MyHttpClient> httpClientMap = new ConcurrentHashMap<String, MyHttpClient>();

	private final Map<String, CertInfo> httpsCertMap = new ConcurrentHashMap<String, CertInfo>();

	private final Map<String, Timeout> timeoutMap = new ConcurrentHashMap<String, Timeout>();

	private final FileInput [] tmpeFiles = new FileInput[0];

	private RequestConfig defaultRequestConfig;

	private NTCredentials ntCredentials;

	private String cookie;

	private Boolean enableCookieStore = false;
	private final List<Cookie> emptyCookies = new ArrayList<Cookie>();

	@Override
	public void setEnableCookieStore(Boolean enable) {
		this.enableCookieStore = enable;
	}

	@Override
	public Boolean getEnableCookieStore() {
		return this.enableCookieStore;
	}

	private synchronized MyHttpClient getMyHttpClientByHost(String host) {
		if(singleton) {
			if(httpClientMap.size() > 0) {
				return httpClientMap.entrySet().iterator().next().getValue();
			}
		}
		return httpClientMap.get(host);
	}

	@Override
	public List<Cookie> getCookies(String host) {
		if(!userCookieStore()) {
			throw new RuntimeException("cookie store is not enable.");
		}
		if(host != null) {
			host = getHost(host);
			MyHttpClient myHttpClient = getMyHttpClientByHost(host);
			return myHttpClient.getCookieStore().getCookies();
		}
		return emptyCookies;
	}

	@Override
	public void setHttpClientSingleton(Boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public void clearCookies(String host) {
		LOG.debug("clearCookies["+host+"].");
		if(!userCookieStore()) {
			throw new RuntimeException("cookie store is not enable.");
		}
		if(host != null) {
			host = getHost(host);
			MyHttpClient myHttpClient = getMyHttpClientByHost(host);
			if(myHttpClient != null && myHttpClient.getCookieStore() != null) {
				LOG.debug("clearCookies["+host+"] success.");
				myHttpClient.getCookieStore().clear();
			} else {
				LOG.debug("clearCookies["+host+"] fail. cookie not found!");
			}
		}
	}

	@Override
	public void addCookie(String host,String name,String value,String path,Integer version) {
		LOG.debug(">>>>>>>>addCookie[host="+host+",cookie="+name+"="+value+"]");
		if(!userCookieStore()) {
			throw new RuntimeException("cookie store is not enable.");
		}
		if(host == null) {
			LOG.debug(">>>>>>>>addCookie[host="+host+",cookie="+name+"="+value+"] fail.host is empty.");
			return;
		}

		host = getHost(host);
		MyHttpClient myHttpClient = getMyHttpClientByHost(host);
		if(myHttpClient != null) {
			SetCookie cookie = new BasicClientCookie(name, value);
			cookie.setDomain(host);
			cookie.setExpiryDate(null);
			if(path == null) {
				path = "/";
			}
			if(version == null) {
				version = 1;
			}
			cookie.setPath(path);
			cookie.setVersion(version);
			LOG.debug(">>>>>>>>addCookie[host="+host+",cookie="+cookie+"] success.");
			myHttpClient.getCookieStore().addCookie(cookie);
		} else {
			LOG.debug(">>>>>>>>addCookie[host="+host+",cookie="+name+"="+value+"] fail.httpClient not found.");
		}
	}

	private boolean userCookieStore() {
		return enableCookieStore != null && enableCookieStore;
	}

	@Override
	public synchronized void removeHttpClient(String host) {
		String realHost = getHost(host);
		httpClientMap.remove(realHost);
	}

	@Override
	public void setCookieInHeader(String cookie) {
		if(userCookieStore()) {
			throw new RuntimeException("cookiestore is enable,use addCookie instead!");
		}
		this.cookie = cookie;
	}

	@Override
	public String getCookieInHeader() {
		if(userCookieStore()) {
			throw new RuntimeException("cookiestore is enable,use getCookies instead!");
		}
		return cookie;
	}

	@Override
	public void setProtocolVersion(HttpVersion httpVersion) {
		this.httpVersion = httpVersion;
	}

	@Override
	public void setNtCredentials(NTCredentials ntCredentials) {
		this.ntCredentials = ntCredentials;
	}

	@Override
	public void setPoolMaxPerRoute(Integer poolMaxPerRoute) {
		this.poolMaxPerRoute = poolMaxPerRoute;
	}

	@Override
	public void setPoolMaxTotal(Integer poolMaxTotal) {
		this.poolMaxTotal = poolMaxTotal;
	}

	/**
	 * 添加https证书
	 * @param host
	 * @param keyStoreType
	 * @param filePath
	 * @param password
	 */
	@Override
	public void addHttpsCert(String host,String keyStoreType,String filePath,String password) {
		host = getHost(host);
		File file = new File(filePath);
		if(!file.exists()) {
			throw new RuntimeException("file ["+filePath+"] not found!");
		}
		if(password == null || password.trim().length() == 0) {
			throw new RuntimeException("password is empty!");
		}
		CertInfo certInfo = new CertInfo(host, keyStoreType, filePath, password);
		httpsCertMap.put(host, certInfo);
	}

	@Override
	public final void closeResponse(CloseableHttpResponse response) {
		if (response == null) {
			return;
		}
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = entity.getContent();
				if (inputStream != null) {
					inputStream.close();
				}
			}
			response.close();
		} catch (Exception e) {
			LOG.error("closeResponse", e);
		}
	}

	@Override
	public <T> List<T> batRequest(List<Callable<T>> requests,ExecutorService executorService,AtomicInteger idx) {
		try {
			List<T> results = new ArrayList<T>();
			List<Future<T>> futures = new ArrayList<Future<T>>();
			for(Callable<T> request:requests) {
				futures.add(executorService.submit(request));
			}
			System.err.println(">>>>>>>>>>waiting result["+requests+"]>>>>>>>>>>");
			for(Future<T> future:futures) {
				results.add(future.get());
				idx.getAndIncrement();
			}
			return results;
		} catch (Exception e) {
			LOG.error("",e);
			throw new RuntimeException("batRequest",e);
		}
	}

	@Override
	public HttpResult doHttp(HttpClientService httpClientService,HttpRequestMeta httpRequestMeta) {
		HttpResult result = null;
		String url = httpRequestMeta.getUrl();
		HttpHost proxy = null;
		FileInput[] fileInputs = new FileInput[0];
		if(httpRequestMeta.getMethod().equalsIgnoreCase("POST")) {
			result = httpClientService.post(url, proxy, httpRequestMeta.getHeader(), httpRequestMeta.getParams(), httpRequestMeta.getCharset(), fileInputs);
		} else {
			result = httpClientService.get(url, httpRequestMeta.getHeader(), httpRequestMeta.getParams(), httpRequestMeta.getCharset());
		}
		return result;
	}

	private final MyHttpClient createSimpleHttpClient(String host) {
		if(host == null) {
			throw new RuntimeException("host is null");
		}
		//nt 此类型需要公用一个httpclient
		if(ntCredentials != null) {
			host = "ntHost";
		}
		MyHttpClient myHttpClient = getMyHttpClientByHost(host);
		if(myHttpClient == null) {
			PoolingHttpClientConnectionManager cm = null;
			HttpClientBuilder builder = HttpClients.custom();
			CookieStore cookieStore = new BasicCookieStore();
			builder.setDefaultCookieStore(cookieStore);
			CloseableHttpClient httpClient = null;
			if(poolMaxTotal >= 1 && poolMaxPerRoute > 1) {
				cm = new PoolingHttpClientConnectionManager();
				cm.setMaxTotal(poolMaxTotal);//连接池最大并发连接数
				cm.setDefaultMaxPerRoute(poolMaxPerRoute);//单路由最大并发数
				builder.setConnectionManager(cm);
			}
			if(ntCredentials != null) {
				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY, ntCredentials);
				httpClient = builder
						.setDefaultCredentialsProvider(credentialsProvider)
						.setRedirectStrategy(new LaxRedirectStrategy())//支持重定向
						.build();
				LOG.info(">>>>>>>>>> create nt httpclient["+httpClient.hashCode()+"] host=["+host+"]>>>>>>>>>>");
			} else {
				httpClient = builder.setRedirectStrategy(new LaxRedirectStrategy()).build();
				LOG.info(">>>>>>>>>> create default httpclient["+httpClient.hashCode()+"] host=["+host+"]>>>>>>>>>>");
			}
			myHttpClient = new MyHttpClient(httpClient, cookieStore);
			httpClientMap.put(host, myHttpClient);
		} else {
			LOG.info(">>>>>>>>>> get cached httpclient["+myHttpClient.hashCode()+"] host=["+host+"]>>>>>>>>>>");
		}
		return myHttpClient;
	}

	private MyHttpClient createSSLClientDefault() {
		try {
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(
					null, new TrustStrategy() {
						// 信任所有
						public boolean isTrusted(X509Certificate[] chain,
												 String authType) throws CertificateException {
							return true;
						}
					}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslContext,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			CookieStore cookieStore = new BasicCookieStore();
			CloseableHttpClient httpClient = HttpClients.custom().setDefaultCookieStore(cookieStore).setRedirectStrategy(new LaxRedirectStrategy()).setSSLSocketFactory(sslsf).build();
			return new MyHttpClient(httpClient, cookieStore);
		} catch (Exception e) {
			LOG.error("",e);
			throw new RuntimeException("",e);
		}
	}

	private MyHttpClient createSSLClientDefault(String host) {
		try {
			host = getHost(host);
			if(host == null) {
				return createSSLClientDefault();
			}
			MyHttpClient myHttpClient = getMyHttpClientByHost(host);
			if(myHttpClient != null) {
				return myHttpClient;
			}
			CertInfo certInfo = getCertInfo(host);
			if(certInfo != null) {
				KeyStore keyStore  = KeyStore.getInstance(certInfo.getKeyStoreType());
				FileInputStream instream = new FileInputStream(new File(certInfo.getFilePath()));
				try {
					keyStore.load(instream, certInfo.getPassword().toCharArray());
				} finally {
					instream.close();
				}
				// Trust own CA and all self-signed certs
				SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, certInfo.getPassword().toCharArray())
						.build();
				// Allow TLSv1 protocol only
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
						sslContext,
						new String[] { "TLSv1" },
						null,
						SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
				HttpClientBuilder builder = HttpClients.custom();
				//不支持PoolingHttpClientConnectionManager
//		        PoolingHttpClientConnectionManager cm = null;
//		        if(poolMaxTotal >= 1 && poolMaxPerRoute > 1) {
//		        	cm = new PoolingHttpClientConnectionManager();
//					cm.setMaxTotal(poolMaxTotal);//连接池最大并发连接数
//					cm.setDefaultMaxPerRoute(poolMaxPerRoute);//单路由最大并发数
//		        }
//		        if(cm != null) {
//		        	 httpClient = builder
//				        		.setRedirectStrategy(new LaxRedirectStrategy())
//				                .setSSLSocketFactory(sslsf)
//				                .setConnectionManager(cm)
//				                .build();
//		        } else {
//		        	 httpClient = builder
//				        		.setRedirectStrategy(new LaxRedirectStrategy())
//				                .setSSLSocketFactory(sslsf)
//				                .build();
//		        }
				CookieStore cookieStore = new BasicCookieStore();
				CloseableHttpClient httpClient = builder
						.setDefaultCookieStore(cookieStore)
						.setRedirectStrategy(new LaxRedirectStrategy())
						.setSSLSocketFactory(sslsf)
						.build();
				myHttpClient = new MyHttpClient(httpClient, cookieStore);
				httpClientMap.put(host, myHttpClient);
				return myHttpClient;
			} else {
				myHttpClient = createSSLClientDefault();
				httpClientMap.put(host, myHttpClient);
				return myHttpClient;
			}
		} catch (Exception e) {
			LOG.error("",e);
		}
		return createSSLClientDefault();
	}

	@Override
	public HttpResult get(String url) {
		return get(url, null, null, null,null);
	}

	@Override
	public HttpResult get(String url,HttpHost proxy) {
		return get(url, proxy, null, null,null);
	}

	@Override
	public HttpResult get(String url,HttpHost proxy, Map<String, String> params) {
		return get(url, proxy, null, params,null);
	}

//	@Override
//	public HttpResult get(String uri,HttpHost proxy, Map<String, String> headers,Map<String, String> params,String sendEncoding) {
//		if(!uri.startsWith("http")) {
//			uri = "http://"+uri;
//		}
//		return get(uri, proxy, headers, params, sendEncoding);
//	}


	@Override
	public HttpResult get(String url, Map<String, String> headers,Map<String, String> params) {
		return get(url, null, headers, params,null);
	}

	@Override
	public HttpResult get(String url, Map<String, String> headers,Map<String, String> params,String sendEncoding) {
		return get(url, null, headers, params,sendEncoding);
	}

	@Override
	public HttpResult get(String url,HttpHost proxy, Map<String, String> headers,Map<String, String> params,String sendEncoding) {
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		MyHttpClient myHttpClient = getHttpClient(url);
		httpClient = myHttpClient.getHttpClient();
		HttpGet get = null;
		int code = HttpStatus.SC_OK;
		try {
			Map<String, String> urlParams = getPramasFromUri(url);
			URI uri = null;
			if(urlParams != null && urlParams.size() > 0) {
				uri = new URI(getPureUrl(url));
				
				if(params != null) {
					params.putAll(urlParams);
				} else {
					params = urlParams;
				}
			} else {
				uri = new URI(url);
			}
			
			if (!isEmptyCollection(params)) {
				String paramsStr = "?"
						+ URLEncodedUtils.format(
						getNameValuePairsFromMap(params), StringUtil.isBlank(sendEncoding)?defaultCharset:sendEncoding);
				get = new HttpGet(uri + paramsStr);
				if (LOG.isDebugEnabled()) {
					LOG.debug(uri + paramsStr);
				}
			} else {
				get = new HttpGet(uri);
			}

			setTimeout(get);

			if(!userCookieStore()) {
				Header[] cookies = get.getHeaders("Cookie");
				LOG.debug("before set cookie:>>>>>>>>>>"+Arrays.toString(cookies));
				if(cookie != null){
					get.setHeader("Cookie",cookie);//设置cookie 解决被两个用户同时登录时被踢掉的问题
				}
				cookies = get.getHeaders("Cookie");
				LOG.debug("after set cookie:>>>>>>>>>>"+Arrays.toString(cookies));
			}

			get.setProtocolVersion(httpVersion);

			if(headers == null) {
				headers = new HashMap<String, String>();
				headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
			}
			String userAgent = headers.get("User-Agent");
			if(userAgent == null || userAgent.trim().length() == 0) {
				headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
			}
			for (Entry<String, String> header : headers.entrySet()) {
				get.addHeader(header.getKey(), header.getValue());
			}
			if (proxy != null) {
				response = httpClient.execute(proxy, get);
			} else {
				response = httpClient.execute(get);
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				LOG.error("httpGet Status: "
						+ response.getStatusLine().getStatusCode() + ",Reason:"
						+ response.getStatusLine().getReasonPhrase());
				get.abort();
				code = response.getStatusLine().getStatusCode();
			}
			LOG.info("url=" + uri+",params="+params + ",response=" + response);
			byte [] bytes = EntityUtils.toByteArray(response.getEntity());
			String charset = getCharsetEncodingFromEntity(response.getEntity(),bytes);
			lastResult = new HttpResult(code, null,charset, bytes);
			return lastResult;
		} catch (ConnectTimeoutException e) {//请求超时
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_SERVICE_UNAVAILABLE;//503服务暂时不可用（服务器由于维护或者负载过重未能应答）
			}
			if(get != null) {
				get.abort();
			}
			lastResult = new HttpResult(code, getHost(url)+" 服务暂时不可用（服务器由于维护或者负载过重未能应答）",null, null);
			return lastResult;
		}  catch (ConnectException e) {//找不到服务
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_SERVICE_UNAVAILABLE;//
			}
			if(get != null) {
				get.abort();
			}
			lastResult = new HttpResult(code, getHost(url)+" 服务暂时不可用（服务器由于维护或者负载过重未能应答）",null, null);
			return lastResult;
		} catch (SocketTimeoutException e) {
            if(code == HttpStatus.SC_OK) {
                code = HttpStatus.SC_BAD_GATEWAY;//
            }
            if(get != null) {
                get.abort();
            }
            lastResult = new HttpResult(code, url+" 网络请求超时",null, null);
            return lastResult;
        } catch (Exception e) {
			if(get != null) {
				get.abort();
			}
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			}
			LOG.error(url,e);
			lastResult = new HttpResult(code, "服务器异常",null, null);
			return lastResult;
		} finally {
			closeResponse(response);
		}
	}

	private final CertInfo getCertInfo(String host) {
		host = getHost(host);
		if(host == null) {
			return null;
		}
		return httpsCertMap.get(host);
	}

	private final String getCharsetEncodingFromEntity(HttpEntity entity,byte [] bytes) {
		String charset = defaultCharset;
		//1.try from entity
		if(entity.getContentEncoding() != null) {
			charset = entity.getContentEncoding().getValue();
			if(charset != null && charset.trim().length() > 0) {
				return charset;
			}
		}
		//2.try from ContentType
		ContentType contentType = ContentType.get(entity);
		if (contentType != null) {
			Charset cs = contentType.getCharset();
			if(cs != null) {
				charset = cs.name();
				if(charset != null && charset.trim().length() > 0) {
					return charset;
				}
			}
		}
		//3.try from html
		try {
			String content = new String(bytes,defaultCharset);
			Document doc = Jsoup.parse(content);

			Elements elements = doc.getElementsByAttribute("charset");
			for(Element element:elements) {
				charset = element.attr("charset");
				if(charset != null && charset.trim().length() > 0) {
					return charset;
				}
			}

			elements = doc.getElementsByAttributeValue("http-equiv", "Content-Type");
			for(Element element:elements) {
				String contentChar = element.attr("content");
				if(contentChar != null && contentChar.contains("text/html;")) {
					charset = contentChar.replace("text/html;", "").trim().replace("charset=", "");
					if(charset != null && charset.trim().length() > 0) {
						return charset;
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
//		System.err.println("****************charset="+charset);
		return charset;
	}

	@Override
	public String getHost(String hostOrUrl) {
		if(hostOrUrl == null) {
			return null;
		}
		if(hostOrUrl.trim().length() == 0) {
			return null;
		}
		if(hostOrUrl.startsWith("http:")) {
			hostOrUrl = hostOrUrl.replace("http://", "");
		}
		if(hostOrUrl.startsWith("https:")) {
			hostOrUrl = hostOrUrl.replace("https://", "");
		}
		if(hostOrUrl.contains("/")) {
			hostOrUrl = hostOrUrl.substring(0,hostOrUrl.indexOf("/"));
		}
		if(hostOrUrl.contains(":")) {
			hostOrUrl = hostOrUrl.substring(0,hostOrUrl.indexOf(":"));
		}
		return hostOrUrl;
	}

	private final MyHttpClient getHttpClient(String url) {
		MyHttpClient httpClient = null;
		String host = getHost(url);
		if(url.startsWith("https")) {
			httpClient = createSSLClientDefault(host);
		} else {
			httpClient = createSimpleHttpClient(host);
		}
		return httpClient;
	}

	private final List<NameValuePair> getNameValuePairsFromMap(
			Map<String, String> params) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		if (!isEmptyCollection(params)) {
			for (Entry<String, String> e : params.entrySet()) {
				pairs.add(new BasicNameValuePair(e.getKey(), e.getValue()));
			}
		}
		return pairs;
	}

	/**
	 * 获取纯URL
	 * @param url
	 * @return
	 */
	private static final String getPureUrl(String url) {
		if(url.contains("?")) {
			url = url.substring(0,url.indexOf("?"));
		}
		return url;
	}
	
	/**
	 * 获取url中的参数
	 * @param uri
	 * @return
	 */
	private final Map<String,String> getPramasFromUri(String uri) {
		Map<String,String> map = new HashMap<String, String>();
		if(uri == null || uri.trim().length() == 0) {
			return map;
		}
		if(!uri.contains("?")) {
			return map;
		}
		String pv = uri.substring(uri.indexOf("?")+1);
		String[] kv = pv.split("&");
		if(kv.length == 0) {
			return map;
		}
		for(String s:kv) {
			String[] akv = s.split("=");
			if(akv.length == 2) {
				map.put(akv[0], akv[1]);
			}
		}
		return map;
	}

	@Override
	public HttpResult getWithHeaders(String url, Map<String, String> headers) {
		return get(url, null, headers, null,null);
	}

	@Override
	public HttpResult getWithParameters(String url, Map<String, String> params) {
		return get(url, null, null, params,null);
	}

	private final boolean isEmptyCollection(Map<?, ?> collection) {
		return (collection == null || collection.isEmpty());
	}

	@Override
	public HttpResult post(String url) {
		return post(url, null, null);
	}
//
//	public final HttpResult post(String url, Map<String, String> headers,String data) {
//
//	}

	@Override
	public HttpResult post(String url,HttpHost proxy, Map<String, String> headers,Map<String, String> params) {
		return post(url, proxy, headers, params,null, tmpeFiles);
	}

	@Override
	public HttpResult post(String url,HttpHost proxy, Map<String, String> headers,Map<String, String> params,String sendEncoding,FileInput ...files) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("url="+url+",proxy = "+proxy+",headers="+headers+",params="+params);
		}
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		MyHttpClient myHttpClient = getHttpClient(url);
		httpClient = myHttpClient.getHttpClient();
		HttpPost post = null;
		int code = HttpStatus.SC_OK;

		Map<String, String> extMap = getPramasFromUri(url);
		if(extMap.size() > 0) {
			if(params == null) {
				params = extMap;
			} else {
				params.putAll(extMap);
			}
		}
		
		url = getPureUrl(url);
		
		try {
			post = new HttpPost(url);
			if(files != null && files.length > 0) {
				MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);//.addBinaryBody(name, b)
				for(FileInput input:files) {
					entityBuilder.addBinaryBody(input.getName(), input.getFile());
				}

				if (!isEmptyCollection(params)) {
					entityBuilder.setCharset(Charset.forName(defaultCharset));

					for(Map.Entry<String, String> en:params.entrySet()) {

						ContentType contentType = ContentType.create("text/plain", defaultCharset);
						StringBody stringBody=new StringBody(en.getValue(),contentType);
						entityBuilder.addPart(en.getKey(),stringBody);
					}
				}
				post.setEntity(entityBuilder.build());
			} else {
				if (!isEmptyCollection(params)) {
					if(sendEncoding != null && sendEncoding.trim().length() > 0) {
						sendEncoding = sendEncoding.toLowerCase();
					} else {
						sendEncoding = defaultCharset;
					}
					post.setEntity(new UrlEncodedFormEntity(
							getNameValuePairsFromMap(params), sendEncoding));
				}
			}

			if(headers == null) {
				headers = new HashMap<String, String>();
				headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
			}
			String userAgent = headers.get("User-Agent");
			if(userAgent == null || userAgent.trim().length() == 0) {
				headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
			}
			for (Entry<String, String> header : headers.entrySet()) {
				post.setHeader(header.getKey(), header.getValue());
			}

			setTimeout(post);

			if(!userCookieStore()) {
				Header[] cookies = post.getHeaders("Cookie");
				LOG.debug("before set cookie:>>>>>>>>>>"+Arrays.toString(cookies));
				if(cookie != null){
					post.setHeader("Cookie",cookie);//设置cookie 解决被两个用户同时登录时被踢掉的问题
				}
				cookies = post.getHeaders("Cookie");
				LOG.debug("after set cookie:>>>>>>>>>>"+Arrays.toString(cookies));
			}

			post.setProtocolVersion(httpVersion);

			if (proxy != null) {
				response = httpClient.execute(proxy, post);
			} else {
				response = httpClient.execute(post);
			}
			LOG.info("url=" + url+",params="+params + ",response=" + response);
			code = response.getStatusLine().getStatusCode();
			if (code != HttpStatus.SC_OK) {
				LOG.error("httpPost Status: "
						+ response.getStatusLine().getStatusCode() + ",Reason:"
						+ response.getStatusLine().getReasonPhrase());
				post.abort();
			}
			HttpEntity entity = response.getEntity();
			byte [] bytes = EntityUtils.toByteArray(entity);
			String charset = getCharsetEncodingFromEntity(entity,bytes);
			lastResult = new HttpResult(code, null,charset, bytes);
			return lastResult;
		} catch (ConnectTimeoutException e) {//请求超时
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_SERVICE_UNAVAILABLE;//503服务暂时不可用（服务器由于维护或者负载过重未能应答）
			}
			if(post != null) {
				post.abort();
			}
			lastResult = new HttpResult(code, getHost(url)+" 服务暂时不可用（服务器由于维护或者负载过重未能应答）",null, null);
			return lastResult;
		} catch (SocketTimeoutException e) {
            if(code == HttpStatus.SC_OK) {
                code = HttpStatus.SC_BAD_GATEWAY;//
            }
            if(post != null) {
                post.abort();
            }
            lastResult = new HttpResult(code, url+" 网络请求超时",null, null);
            return lastResult;
        }  catch (ConnectException e) {//找不到服务
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_SERVICE_UNAVAILABLE;//
			}
			if(post != null) {
				post.abort();
			}
			lastResult = new HttpResult(code, getHost(url)+" 服务暂时不可用（服务器由于维护或者负载过重未能应答）",null, null);
			return lastResult;
		} catch (Exception e) {
			if(post != null) {
				post.abort();
			}
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			}
			LOG.error(url,e);
			lastResult = new HttpResult(code, "服务器异常。url=["+url+"]",null, null);
			return lastResult;
		} finally {
			closeResponse(response);
		}
	}

	@Override
	public final HttpResult post(String url,Map<String, String> headers, Map<String, String> params) {
		return post(url, null, headers, params);
	}

	@Override
	public final HttpResult postFile(String url,FileInput ...files) {
		return post(url, null, null, null,null, files);
	}

	@Override
	public final HttpResult postFile(String url,Map<String, String> params,FileInput ...files) {
		return post(url, null, null, params,null, files);
	}

	@Override
	public final HttpResult postString(String url,HttpHost proxy, Map<String, String> headers,String stringValue,String sendEncoding) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("url="+url+",proxy = "+proxy+",headers="+headers+",stringValue="+stringValue);
		}
		if(sendEncoding == null || sendEncoding.trim().length() == 0) {
			sendEncoding = defaultCharset;
		}
		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;
		MyHttpClient myHttpClient = getHttpClient(url);
		httpClient = myHttpClient.getHttpClient();
		HttpPost post = null;
		int code = HttpStatus.SC_OK;

		try {
			post = new HttpPost(url);

			if(stringValue != null && stringValue.trim().length() > 0) {
				post.setEntity(new StringEntity(stringValue, sendEncoding));
			}

			if(headers == null) {
				headers = new HashMap<String, String>();
				headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
			}
			String userAgent = headers.get("User-Agent");
			if(userAgent == null || userAgent.trim().length() == 0) {
				headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
			}
			for (Entry<String, String> header : headers.entrySet()) {
				post.setHeader(header.getKey(), header.getValue());
			}

			setTimeout(post);

			if(!userCookieStore()) {
				if(cookie != null){
					post.setHeader("Cookie",cookie);//设置cookie 解决被两个用户同时登录时被踢掉的问题
				}
			}

			post.setProtocolVersion(httpVersion);

			if (proxy != null) {
				response = httpClient.execute(proxy, post);
			} else {
				response = httpClient.execute(post);
			}
			LOG.info("url=" + url + ",response=" + response);
			code = response.getStatusLine().getStatusCode();
			if (code != HttpStatus.SC_OK) {
				LOG.error("httpPost Status: "
						+ response.getStatusLine().getStatusCode() + ",Reason:"
						+ response.getStatusLine().getReasonPhrase());
				post.abort();
			}
			HttpEntity entity = response.getEntity();
			byte [] bytes = EntityUtils.toByteArray(entity);
			String charset = getCharsetEncodingFromEntity(entity,bytes);
			lastResult = new HttpResult(code, null,charset, bytes);
			return lastResult;
		} catch (ConnectTimeoutException e) {//请求超时
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_SERVICE_UNAVAILABLE;//503服务暂时不可用（服务器由于维护或者负载过重未能应答）
			}
			if(post != null) {
				post.abort();
			}
			lastResult = new HttpResult(code, getHost(url)+" 服务暂时不可用（服务器由于维护或者负载过重未能应答）",null, null);
			return lastResult;
		}  catch (ConnectException e) {//找不到服务
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_SERVICE_UNAVAILABLE;//
			}
			if(post != null) {
				post.abort();
			}
			lastResult = new HttpResult(code, getHost(url)+" 服务暂时不可用（服务器由于维护或者负载过重未能应答）",null, null);
			return lastResult;
		} catch (SocketTimeoutException e) {
            if(code == HttpStatus.SC_OK) {
                code = HttpStatus.SC_BAD_GATEWAY;//
            }
            if(post != null) {
                post.abort();
            }
            lastResult = new HttpResult(code, url+" 网络请求超时",null, null);
            return lastResult;
        } catch (Exception e) {
			if(post != null) {
				post.abort();
			}
			if(code == HttpStatus.SC_OK) {
				code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			}
			LOG.error(url,e);
			lastResult = new HttpResult(code, "服务器异常。url=["+url+"]",null, null);
			return lastResult;
		} finally {
			closeResponse(response);
		}
	}

	@Override
	public HttpResult getLastResult() {
		return lastResult;
	}

	@Override
	public final HttpResult postWithHeaders(String url, Map<String, String> headers) {
		return post(url, headers, null);
	}

	@Override
	public final HttpResult postWithParameters(String url, Map<String, String> params) {
		return post(url, null, params);
	}

	private final void setTimeout(HttpGet httpGet) {

		String host = httpGet.getURI().getHost();
		Timeout timeout = null;
		if(host != null) {
			timeout = timeoutMap.get(host);
		}
		if(timeout != null) {
			LOG.debug("setTimeout["+httpGet.getURI().getHost()+"]="+timeout);
			httpGet.setConfig(RequestConfig
					.custom().setSocketTimeout(timeout.getSocketTimeout())// 请求超时|读取超时
					.setConnectTimeout(connectTimeout).build()// 连接超时
			);
			return;
		}
		LOG.debug("setTimeout.default[connectTimeout="+connectTimeout+",socketTimeout="+socketTimeout+"]");
		httpGet.setConfig(getDefaultRequestConfig());
	}

	private final void setTimeout(HttpPost httpPost) {
		String host = httpPost.getURI().getHost();
		Timeout timeout = null;
		if(host != null) {
			timeout = timeoutMap.get(host);
		}
		if(timeout != null) {
			LOG.debug("setTimeout["+httpPost.getURI().getHost()+"]="+timeout);
			httpPost.setConfig(RequestConfig
					.custom().setSocketTimeout(timeout.getSocketTimeout())// 请求超时|读取超时
					.setConnectTimeout(connectTimeout).build()// 连接超时
			);
			return;
		}
		httpPost.setConfig(getDefaultRequestConfig());
	}

	private RequestConfig getDefaultRequestConfig() {
		if(defaultRequestConfig == null) {
			defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(socketTimeout)// 请求超时|读取超时
					.setConnectTimeout(connectTimeout).build();// 连接超时
		} else {
			//如果超时有变更，修改默认值
			if(defaultRequestConfig.getSocketTimeout() != socketTimeout || defaultRequestConfig.getConnectTimeout() != connectTimeout) {
				defaultRequestConfig = RequestConfig.custom()
						.setSocketTimeout(socketTimeout)// 请求超时|读取超时
						.setConnectTimeout(connectTimeout).build();// 连接超时
			}
		}
		
		return defaultRequestConfig;
	}

	/**
	 * 设置超时,单位为毫秒
	 * @param host 访问服务器的host,如www.baidu.com
	 * @param socketTimeout 请求超时|读取超时
	 * @param connectTimeout 连接超时
	 */
	@Override
	public void setTimeoutMillis(String host,int socketTimeout,int connectTimeout) {
		if(host == null) {
			throw new RuntimeException("host can not be null.");
		}

		host = getHost(host);

		Timeout timeout = timeoutMap.get(host);
		if(timeout != null) {
			LOG.warn("host["+host+"]'s socketTimeout="+timeout.getSocketTimeout()+",connectTimeout="+timeout.getConnectTimeout()+" will be replace by socketTimeout="+socketTimeout+",connectTimeout="+connectTimeout);
			timeout.setConnectTimeout(connectTimeout);
			timeout.setSocketTimeout(socketTimeout);
		} else {
			timeout = new Timeout(host, socketTimeout, connectTimeout);
		}
		timeoutMap.put(host, timeout);
		LOG.info("超时设置："+timeoutMap);
	}

	/**
	 * 设置超时,单位为秒
	 * @param host 访问服务器的host,如www.baidu.com
	 * @param socketTimeout 请求超时|读取超时
	 * @param connectTimeout 连接超时
	 */
	@Override
	public final void setTimeoutSecond(String host,int socketTimeout,int connectTimeout) {
		setTimeoutMillis(host, socketTimeout*1000, connectTimeout*1000);
	}

	@Override
	public Integer getConnectTimeout() {
		return connectTimeout;
	}

	@Override
	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	@Override
	public String getDefaultCharset() {
		return defaultCharset;
	}

	@Override
	public void setDefaultCharset(String defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	@Override
	public Integer getSocketTimeout() {
		return socketTimeout;
	}

	@Override
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}


	public static void main(String[] args) {
		HttpClientService httpClientService = new HttpClientServiceImpl();
		String url = "http://site.baidu.com?CCC=2&ASD=2";
		System.err.println(getPureUrl(url));
		Map<String, String> params = new HashMap<String, String>();
		params.put("aa", "11");
		httpClientService.getWithParameters(url, params);
	}

}
