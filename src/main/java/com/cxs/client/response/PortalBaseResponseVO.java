package com.cxs.client.response;

import java.io.Serializable;

/**
 * 请求返回vo
 * 
 * @author xiaodx
 *
 */
public class PortalBaseResponseVO implements Serializable {

	private static final long serialVersionUID = -9171589117250039121L;
	
	private PortalResponseHeadVO head;

	public PortalResponseHeadVO getHead() {
		return head;
	}

	public void setHead(PortalResponseHeadVO head) {
		this.head = head;
	}

}
