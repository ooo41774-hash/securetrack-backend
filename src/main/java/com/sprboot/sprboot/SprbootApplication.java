package com.sprboot.sprboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SprbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(SprbootApplication.class, args);
		// try {
		// SecretKey key = AESUtil.generateKey(128);
		// String keyString = AESUtil.keyToString(key);
		// System.out.println("AES Key Base64 = " + keyString);
		// } catch (Exception e) {
		// System.out.println(e);
		// }
	}

}
