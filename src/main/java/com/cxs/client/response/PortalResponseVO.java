package com.cxs.client.response;

/**
 * 请求返回vo
 * 
 * @author Liuyf
 *
 */
@SuppressWarnings("serial")
public class PortalResponseVO<T> extends PortalBaseResponseVO {

	private T body;

	public PortalResponseVO() {}
	@SuppressWarnings("unchecked")
	public PortalResponseVO(PortalResponseHeadVO head,Object body) {
		super();
		this.setHead(head);
		this.body = (T)body;
	}

	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

}
