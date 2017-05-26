package com.cxs.client.utils;


import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESHelper { // 初始向量
	
	public static final String VIPARA = "9876789012345123"; // AES 为16bytes. DES
															// 为8bytes

	// 编码方式
	public static final String charset = "UTF-8";

	/**
	 * 加密
	 * 
	 * @param cleartext
	 * @return
	 */
	public static String encrypt(String cleartext,String password) {
		// 加密方式： AES128(CBC/PKCS5Padding) + Base64, 私钥：1234567890123456
		try {
			IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
			// 两个参数，第一个为私钥字节数组， 第二个为加密方式 AES或者DES
			SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
			// 实例化加密类，参数为加密方式，要写全
			//Cipher.getInstance("AES")    和    Cipher.getInstance("AES/CBC/PKCS5Padding")   一样，即默认。
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // PKCS5Padding比PKCS7Padding效率高，PKCS7Padding可支持IOS加解密
			// 初始化，此方法可以采用三种方式，按加密算法要求来添加。（1）无第三个参数（2）第三个参数为SecureRandom random
			// = new
			// SecureRandom();中random对象，随机数。(AES不可采用这种方法)（3）采用此代码中的IVParameterSpec
			cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
			// 加密操作,返回加密后的字节数组，然后需要编码。主要编解码方式有Base64, HEX,
			// UUE,7bit等等。此处看服务器需要什么编码方式
			byte[] encryptedData = cipher.doFinal(cleartext.getBytes(charset));

			return java.util.Base64.getEncoder().encodeToString(encryptedData);
//			return Base64.encode(new String(encryptedData, charset));
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 解密
	 * 
	 * @param encrypted
	 * @return
	 */
	public static String decrypt(String encrypted,String password) {
		try {
			byte[] byteMi = Base64.getDecoder().decode(encrypted);
			IvParameterSpec zeroIv = new IvParameterSpec(VIPARA.getBytes());
			SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			// 与加密时不同MODE:Cipher.DECRYPT_MODE
			cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
			byte[] decryptedData = cipher.doFinal(byteMi);
			return new String(decryptedData, charset);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 测试
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String content = "hello";
		// 私钥
		String password = "4de0e32d8d1240bea92ec57bde67893b";
//		String password = "4de0e32d8d1240be";
		// 加密
		System.out.println("加密前：" + content);
		String encryptResult = encrypt(content,password);

		System.out.println("加密后：" + new String(encryptResult));
		// 解密
		String decryptResult = decrypt(encryptResult,password);
		System.out.println("解密后：" + new String(decryptResult));
		//5BDyU4YKGtvTT0W3GESNMQ==
		//5BDyU4YKGtvTT0W3GESNMQ==
//		
		
//		byte [] bytes = Base64.decode("d++/vRhg77+9EO+/ve+/ve+/vW3vv73vv71C77+977+9fg==").getBytes(charset);
//		System.err.println(Arrays.toString(bytes));
//		System.err.println(UUIDUtils.getUUID());
//		SecretKeySpec key = new SecretKeySpec(UUIDUtils.getUUID().getBytes(), "AES");
//		System.err.println(key);

	}
}
