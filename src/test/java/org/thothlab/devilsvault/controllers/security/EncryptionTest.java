package org.thothlab.devilsvault.controllers.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

class EncryptionTest {

	@Test
	void loadsAesKeyFromPkcs12KeystoreOnClasspath() throws Exception {
		assertThat(Encryption.loadKey().getAlgorithm()).isEqualTo("AES");
	}

	@Test
	void encodeDecodeRoundTripsSsnAndDateOfBirth() {
		String ssn = Encryption.Encode("123-45-6789");
		String dob = Encryption.Encode("1990-01-31");

		assertThat(ssn).isNotEqualTo("encoding error").isNotEqualTo("123-45-6789");
		assertThat(Encryption.Decode(ssn)).isEqualTo("123-45-6789");
		assertThat(Encryption.Decode(dob)).isEqualTo("1990-01-31");
	}

	@Test
	void legacyJceksKeystoreStillDecryptsOnJdk21() throws Exception {
		SecretKey key = KeyGenerator.getInstance("AES").generateKey();
		KeyStore jceks = KeyStore.getInstance("JCEKS");
		jceks.load(null, null);
		jceks.setEntry("mykey", new KeyStore.SecretKeyEntry(key),
				new KeyStore.PasswordProtection("password".toCharArray()));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		jceks.store(out, "password".toCharArray());

		KeyStore reloaded = KeyStore.getInstance("JCEKS");
		reloaded.load(new java.io.ByteArrayInputStream(out.toByteArray()), "password".toCharArray());
		byte[] raw = reloaded.getKey("mykey", "password".toCharArray()).getEncoded();

		String cipherText = Encryption.encryptWithAESKey("secret", raw);
		assertThat(Encryption.decryptWithAESKey(cipherText, raw)).isEqualTo("secret");
	}

	@Test
	void decodeOfGarbageReturnsLegacyErrorMarker() {
		assertThat(Encryption.Decode("not-base64-cipher")).isEqualTo("decoding error");
	}
}
