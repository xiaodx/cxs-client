package com.cxs.client.model;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cxs.client.response.PortalBaseResponseVO;
import com.cxs.client.response.PortalResponseVO;
import com.cxs.client.utils.AESHelper;
import com.cxs.client.utils.JSONUtil;

public class RequestBody {

	private Logger LOG = LoggerFactory.getLogger(RequestBody.class);
	private Map<String, Object> body = new HashMap<String, Object>();

	// 附加属性
	private ClientRequestContext context;

	public void setContext(ClientRequestContext context) {
		this.context = context;
	}
	
	public RequestBody timeoutSecond(Integer seconds) {
		context.setTimeoutSecond(seconds);
		return this;
	}

	public RequestBody add(String key, Object value) {
		body.put(key, value);
		return this;
	}
	

	public RequestBody addAll(Map<String, Object> bodyData) {
		body.putAll(bodyData);
		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String send() {
		String data = context.getRequestData(context.getHead().getUid(), context.getHead().getAppSecret(),
				context.getHead().getService_code(), context.getHead().getEncrypt(), context.getHead().getVersion(), body);
		LOG.info("requestData:" + data);
		String result = context.postBodyWithHttpClient(context.getPortalUr(), data);
//		String result = context.postBody(context.getPortalUr(), data);
		LOG.info("response:" + result);

		//反解消息头部信息，取出加密类型，判断后再输出
		PortalBaseResponseVO baseResponseVO = JSONUtil.getObject(result,PortalBaseResponseVO.class);
		if(baseResponseVO != null && baseResponseVO.getHead() != null && baseResponseVO.getHead().getEncrypt().trim() != null && baseResponseVO.getHead().getEncrypt().trim().equalsIgnoreCase("AES")) {
			//AES 解密
			PortalResponseVO<String> encryptResponse = JSONUtil.getObject(result, new TypeReference<PortalResponseVO<String>>() {});
			String encryptBody = encryptResponse.getBody();
			String decryptBody = AESHelper.decrypt(encryptBody, context.getHead().getAppSecret());
			PortalResponseVO finalResult = new PortalResponseVO();
			finalResult.setHead(baseResponseVO.getHead());
			finalResult.setBody(JSONUtil.getObject(decryptBody));
			return JSONUtil.toString(finalResult);
		}
		return result;
	}

}
