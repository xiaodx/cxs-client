package com.cxs.client.utils;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;

public class Base64Cache {
	private static final int BASE64_QUEUE_SIZIE = 1024;
	private static final ArrayBlockingQueue<Base64> base64Queue = new ArrayBlockingQueue<Base64>(BASE64_QUEUE_SIZIE);
	
	static {
		for(int i=0;i<BASE64_QUEUE_SIZIE;i++) {
			base64Queue.offer(reset(new Base64()));
		}
	}
	
	public static final Base64 getBase64() {
		Base64 base64 = base64Queue.poll();
		if(base64 == null) {
			base64 = new Base64();
		}
		return base64;
	}
	
	public static final void resetBase64(Base64 base64) {
		base64 = reset(base64);
		if(base64 != null) {
			base64Queue.offer(base64);
		}
	}
	
	
	private static Base64 reset(Base64 base64) {
		if(base64 != null) {
			base64.init();
			return base64;
		}
		return null;
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		String msg = Base64.encode("123456");
		System.err.println(msg);//msg=kla.5Qmi
		String msg2 = Base64.decode(msg);
		System.err.println(msg2);//msg2=123456
	}
}
