package com.cxs.client;

import java.util.HashMap;
import java.util.Map;

import com.cxs.client.model.ClientRequestContext;
import com.cxs.client.model.RequestHead;

public class PortalClient {

	private String portalUr;
	private String uid;
	private String appSecret;
	private String encrypt; 

	public PortalClient(String portalUrl) {
		this.portalUr = portalUrl;
	}
	
	public PortalClient(String portalUrl,String uid,String appSecret) {
		this.portalUr = portalUrl;
		this.uid = uid;
		this.appSecret = appSecret;
	}
	
	public PortalClient(String portalUrl,String uid,String appSecret,String encrypt) {
		this.portalUr = portalUrl;
		this.uid = uid;
		this.appSecret = appSecret;
		if(encrypt == null || encrypt.trim().length() == 0) {
			encrypt = "none";
		}
		this.encrypt = encrypt;
	}
	
	public RequestHead header() {
		RequestHead head = new RequestHead(new ClientRequestContext(portalUr));
		return head;
	}
	
	public String send(String serviceCode, Map<String, Object> body, Integer timeoutSecond) {
		return this.header()
			.serviceCode(serviceCode)
			.appSecret(appSecret)
			.uid(uid)
			.encrypt(encrypt)
		.body()
			.addAll(body)
		.timeoutSecond(timeoutSecond)//设置超时时间
		.send();
	}
	

	public static void main(String[] args) {

		//接口服务器调用地址（运营提供）
		String portalUrl = "http://127.0.0.1:8002/default";
		
		//客户编号（运营提供）
		String uid = "a7bd180a709ac0cc12d9f32f72b3511f";
		//客户密钥（运营提供）
		String appSecret = "3aaa75570f844b08bb6eb95aef837f52";
		//加密方式，默认为"none",不区分大小写（运营指定）
		String encrypt = "AES";
		
		//构造客户端，每个账号要初始化一个客户端（如果有多个调用的账号，就要new多个PortalClient）
		PortalClient client = new PortalClient(portalUrl,uid,appSecret,encrypt);
		
		//被调用接口的编号
		String serviceCode = "100000010006";
		
		//请求体body的参数
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("email", "1111@11.com");
		body.put("customer", "222222222");
		
		//请求超时时间
		Integer timeoutSecond = 12;
		
		//开始请求
		String result = client.send(serviceCode,body,timeoutSecond);
		
		//请求结果
		System.err.println("result:"+result);
		
		
	}

	

}
