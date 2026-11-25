package org.thothlab.devilsvault.dao.userauthentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OneTimePasswordTest {

	private static final byte[] SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

	/** Reference HOTP (RFC 4226 dynamic truncation) over HMAC-SHA256, which is what OneTimePassword uses. */
	private static String referenceHotp(byte[] secret, long counter, int digits) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret, "RAW"));
		byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
		int offset = hash[hash.length - 1] & 0xf;
		int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
				| ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
		return String.format("%0" + digits + "d", binary % (int) Math.pow(10, digits));
	}

	@ParameterizedTest
	@ValueSource(longs = { 0, 1, 2, 20, 1_000_000 })
	void matchesReferenceHotp(long counter) throws Exception {
		assertThat(OneTimePassword.generateOTP(SECRET, counter, 6, false, -1))
				.isEqualTo(referenceHotp(SECRET, counter, 6));
	}

	@Test
	void isDeterministic() throws Exception {
		assertThat(OneTimePassword.generateOTP(SECRET, 20, 6, false, 20))
				.isEqualTo(OneTimePassword.generateOTP(SECRET, 20, 6, false, 20));
	}

	@Test
	void checksumAppendsOneDigit() throws Exception {
		String plain = OneTimePassword.generateOTP(SECRET, 0, 6, false, -1);
		assertThat(OneTimePassword.generateOTP(SECRET, 0, 6, true, -1)).hasSize(7).startsWith(plain);
	}

	@Test
	void randomOtpIsSixDigits() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 20; i++) {
			String otp = OneTimePassword.getOTP();
			assertThat(otp).matches("\\d{6}");
			seen.add(otp);
		}
		assertThat(seen).hasSizeGreaterThan(1);
	}
}
