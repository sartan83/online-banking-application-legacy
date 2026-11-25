package org.thothlab.devilsvault.dao.userauthentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;

class OTPDeliveryTest {

	@Test
	void sendsSixDigitOtpToRecipient() throws Exception {
		try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
			String otp = new OTPDelivery().send("alice@bank.test");

			ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
			transport.verify(() -> Transport.send(sent.capture()));
			Message message = sent.getValue();

			assertThat(otp).matches("\\d{6}");
			assertThat(message.getRecipients(Message.RecipientType.TO))
					.containsExactly(new InternetAddress("alice@bank.test"));
			assertThat(message.getFrom()).containsExactly(new InternetAddress("securebanking.ss4@gmail.com"));
			assertThat(message.getSubject()).isEqualTo("Please do not share this with anyone");
			assertThat(message.getContent()).isEqualTo("Your One Time Password is " + otp);
			assertThat(message.getSession().getProperty("mail.smtp.host")).isEqualTo("smtp.gmail.com");
		}
	}

	@Test
	void mailFailureIsWrapped() {
		try (MockedStatic<Transport> transport = mockStatic(Transport.class)) {
			transport.when(() -> Transport.send(any(Message.class))).thenThrow(new SendFailedException("down"));

			assertThatThrownBy(() -> new OTPDelivery().send("alice@bank.test")).isInstanceOf(RuntimeException.class)
					.hasCauseInstanceOf(MessagingException.class);
		}
	}
}
