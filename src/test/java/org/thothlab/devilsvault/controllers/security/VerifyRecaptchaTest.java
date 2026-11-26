package org.thothlab.devilsvault.controllers.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** Exercises the Jakarta JSON-P parsing and POST contract against a local fake siteverify endpoint. */
class VerifyRecaptchaTest {

	private HttpServer server;
	private final AtomicReference<String> responseBody = new AtomicReference<>();
	private final AtomicReference<String> receivedForm = new AtomicReference<>();
	private String endpoint;

	@BeforeEach
	void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/recaptcha/api/siteverify", exchange -> {
			receivedForm.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
					+ "|" + exchange.getRequestMethod());
			byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/recaptcha/api/siteverify";
	}

	@AfterEach
	void stop() {
		server.stop(0);
	}

	@Test
	void successTrueIsAccepted() {
		responseBody.set("{\"success\": true, \"hostname\": \"localhost\"}");
		assertThat(VerifyRecaptcha.verify("token-123", endpoint)).isTrue();
		assertThat(receivedForm.get()).startsWith("secret=").contains("&response=token-123").endsWith("|POST");
	}

	@Test
	void successFalseIsRejected() {
		responseBody.set("{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}");
		assertThat(VerifyRecaptcha.verify("bad", endpoint)).isFalse();
	}

	@Test
	void malformedJsonIsRejected() {
		responseBody.set("<html>oops</html>");
		assertThat(VerifyRecaptcha.verify("token", endpoint)).isFalse();
	}

	@Test
	void blankTokenIsRejectedWithoutCallingGoogle() throws Exception {
		assertThat(VerifyRecaptcha.verify("")).isFalse();
		assertThat(VerifyRecaptcha.verify(null)).isFalse();
	}

	@Test
	void unreachableEndpointIsRejected() {
		server.stop(0);
		assertThat(VerifyRecaptcha.verify("token", endpoint)).isFalse();
	}
}
