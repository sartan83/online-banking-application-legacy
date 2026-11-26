package org.thothlab.devilsvault.controllers.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Encryption {
	
	static final String KEYSTORE_RESOURCE = "mykeystore.jks";
	private static final char[] KEYSTORE_PASSWORD = "password".toCharArray();
	private static final String KEY_ALIAS = "mykey";

	public static String Encode(String data) {
		try {
			return encryptWithAESKey(data, loadKey().getEncoded());
		} catch (Exception e) {
			e.printStackTrace();
			return "encoding error";
		}
	}

	public static String Decode(String encryptedData) {
		try {
			return decryptWithAESKey(encryptedData, loadKey().getEncoded());
		} catch (Exception e) {
			e.printStackTrace();
			return "decoding error";
		}
	}

	/** Reads the AES key from a PKCS12 keystore (JDK 9+ default), falling back to legacy JCEKS stores. */
	static Key loadKey() throws Exception {
		byte[] bytes;
		try (InputStream stream = Encryption.class.getClassLoader().getResourceAsStream(KEYSTORE_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Keystore " + KEYSTORE_RESOURCE + " not found on classpath");
			}
			bytes = stream.readAllBytes();
		}
		Exception failure = null;
		for (String type : new String[] { "PKCS12", "JCEKS" }) {
			try {
				KeyStore keyStore = KeyStore.getInstance(type);
				keyStore.load(new ByteArrayInputStream(bytes), KEYSTORE_PASSWORD);
				Key key = keyStore.getKey(KEY_ALIAS, KEYSTORE_PASSWORD);
				if (key != null) {
					return key;
				}
			} catch (Exception e) {
				failure = e;
			}
		}
		throw new IllegalStateException("No AES key '" + KEY_ALIAS + "' in " + KEYSTORE_RESOURCE, failure);
	}

	public static String encryptWithAESKey(String data, byte[] key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, UnsupportedEncodingException {
		SecretKey secKey = new SecretKeySpec(key, "AES");

		Cipher cipher = Cipher.getInstance("AES");

		cipher.init(Cipher.ENCRYPT_MODE, secKey);
		byte[] newData = cipher.doFinal(data.getBytes());

		return Base64.encodeBase64String(newData);
	}

	public static String decryptWithAESKey(String inputData, byte[] key) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("AES");
		SecretKey secKey = new SecretKeySpec(key, "AES");

		cipher.init(Cipher.DECRYPT_MODE, secKey);
		byte[] newData = cipher.doFinal(Base64.decodeBase64(inputData.getBytes()));
		return new String(newData);

	}

}