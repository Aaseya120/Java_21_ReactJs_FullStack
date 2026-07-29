package com.demo.payment.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentCryptographyServiceTest {

	private PaymentCryptographyService cryptographyService;

	@BeforeEach
	void setUp() throws Exception {
		cryptographyService = new PaymentCryptographyService();
		cryptographyService.initKeys();
	}

	@Test
	void testPublicKeyPemFormatting() {
		String pem = cryptographyService.getPublicKeyPem();
		assertNotNull(pem);
		assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"));
		assertTrue(pem.endsWith("-----END PUBLIC KEY-----"));
	}

	@Test
	void testAsymmetricRsaEncryptionAndDecryption() throws Exception {
		String sensitiveCardData = "{\"pan\":\"4532000000004242\",\"expiry\":\"12/28\",\"cvv\":\"123\",\"brand\":\"VISA\"}";
		
		// Encrypt with Merchant/Gateway Public Key
		String encryptedToken = cryptographyService.encryptWithPublicKey(sensitiveCardData);
		assertNotNull(encryptedToken);
		assertFalse(encryptedToken.contains("4532000000004242")); // Raw PAN is encrypted

		// Decrypt with Merchant/Gateway Private Key
		String decryptedText = cryptographyService.decryptWithPrivateKey(encryptedToken);
		assertEquals(sensitiveCardData, decryptedText);
	}

	@Test
	void testDigitalSignatureGenerationAndVerification() throws Exception {
		String payload = "orderId=100&amount=99.99&currency=USD&status=SUCCESS";

		// Sign payload with RSA Private Key
		String signature = cryptographyService.signPayload(payload);
		assertNotNull(signature);

		// Verify signature with RSA Public Key
		boolean isValid = cryptographyService.verifySignature(payload, signature);
		assertTrue(isValid);

		// Tampered payload fails verification
		boolean isTamperedValid = cryptographyService.verifySignature(payload + "&tampered=true", signature);
		assertFalse(isTamperedValid);
	}
}
