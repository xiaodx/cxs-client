package com.cxs.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.cxs.client.http.HttpClientService;
import com.cxs.client.http.impl.HttpClientServiceImpl;
import com.cxs.client.http.vo.HttpResult;
import com.cxs.client.utils.AESHelper;
import com.cxs.client.utils.JSONUtil;
import com.cxs.client.utils.MD5Utils;

/**
 * 每次请求的上下文
 * 
 * @author xiaodx
 *
 */
public class ClientRequestContext {

	private RequestHead head;
	private RequestBody body;

	private String portalUr;
	
	private Integer timeoutSecond = 30;
	
	public ClientRequestContext(String portalUrl) {
		this.portalUr = portalUrl;
	}
	
	public void setTimeoutSecond(Integer timeoutSecond) {
		this.timeoutSecond = timeoutSecond;
	}
	
	public RequestBody getRequestBody() {
		if(this.body == null) {
			this.body = new RequestBody();
			this.body.setContext(this);
		}
		return body;
	}
	
	public void setHead(RequestHead head) {
		this.head = head;
	}
	
	public RequestHead getHead() {
		return head;
	}

	public static final boolean isSimpleType(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			return true;
		}

		// JAVA内置的类型
		if (clazz.getClassLoader() == null) {
			return true;
		}
		return false;
	}

	private static final String currentDateStr() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(new Date());
	}

	public final String getRequestData(String uid, String appSecret, String serviceCode, String encrypt, String version,
			Map<String, Object> parameters) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("service_code", serviceCode);
		headers.put("uid", uid);
		String requestTime = currentDateStr();
		headers.put("request_time", requestTime);

		if (encrypt == null || encrypt.trim().length() == 0) {
			headers.put("encrypt", "none");
		} else {
			headers.put("encrypt", encrypt);
		}
		if (version == null || version.trim().length() == 0) {
			headers.put("version", "0.0.1");
		} else {
			headers.put("version", version);
		}

		// signature=md5("uid+service_code+request_time+32位私钥","utf-8")
		String signature = MD5Utils.md5(uid + serviceCode + requestTime + appSecret, "utf-8");
		headers.put("signature", signature);
		Map<String, Object> noEncryptBody = new HashMap<String, Object>();
		for (Map.Entry<String, Object> en : parameters.entrySet()) {
			String key = en.getKey();
			Object vObject = en.getValue();
			if (vObject != null) {
				if (isSimpleType(vObject.getClass())) {
					noEncryptBody.put(key, vObject);
				} else {
					String value = JSONUtil.toString(vObject);
					noEncryptBody.put(key, value);
				}
			}
		}

		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("head", headers);
		if(encrypt.equalsIgnoreCase("AES")) {
			String encryptBody = AESHelper.encrypt(JSONUtil.toString(noEncryptBody), appSecret);
			obj.put("body", encryptBody);
		} else {
			obj.put("body", noEncryptBody);
		}
		
		String jsonData = JSONUtil.toString(obj);
		return jsonData;
	}
	
	private static HttpClientService httpClientService = new HttpClientServiceImpl();
	static {
		httpClientService.setSocketTimeout(60*1000);
	}
	
	
	private Map<String, String> requestHead = new HashMap<String, String>();
	{
		requestHead.put("Content-Type", "application/json");
	}
	
	public String postBodyWithHttpClient(String url, String body) {
		httpClientService.setSocketTimeout(timeoutSecond*1000);
		HttpResult result = httpClientService.postString(url, null, requestHead, body,"utf-8");
		if(result.success()) {
			return result.getData();
		} else {
			return "请求失败：("+result.getCode()+":"+result.getData()+")";
		}
	}
	
	public static String postBody(String url, String body) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Charset", "UTF-8");  
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
//			body = URLEncoder.encode(body, "utf-8");
			out.print(body);
			// flush输出流的缓冲
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
			String line;
			StringBuilder builderLine = new StringBuilder();
			while ((line = in.readLine()) != null) {
				builderLine.append(line);
			}
			return builderLine.toString();
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	public String getPortalUr() {
		return portalUr;
	}

	public void setPortalUr(String portalUr) {
		this.portalUr = portalUr;
	}

}
