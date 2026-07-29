package com.demo.payment.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Industry-Standard Payment Cryptography Service for PCI-DSS & EMVCo Card Token Security.
 * 
 * Implements RSA-2048 Asymmetric Public/Private Key Cryptography to:
 * 1. Expose Merchant/Gateway Public Key for client-side PAN/CVV encryption (like Visa Token Service / Apple Pay).
 * 2. Decrypt Encrypted Card Cryptograms using the Hardware/Merchant Private Key.
 * 3. Generate and verify SHA256withRSA Digital Signatures for Visa/Mastercard payloads and webhooks.
 */
@Service
@Slf4j
public class PaymentCryptographyService {

	private static final String RSA_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
	private static final int KEY_SIZE = 2048;

	private KeyPair rsaKeyPair;

	@PostConstruct
	public void initKeys() throws NoSuchAlgorithmException {
		log.info("Initializing RSA-{} Public/Private Key Pair for PCI-DSS Card Token & Webhook Security", KEY_SIZE);
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
		keyPairGenerator.initialize(KEY_SIZE);
		this.rsaKeyPair = keyPairGenerator.generateKeyPair();
	}

	/**
	 * Exposes standard PEM-formatted Public Key for client-side encryption of card details.
	 */
	public String getPublicKeyPem() {
		PublicKey publicKey = rsaKeyPair.getPublic();
		String encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
	}

	/**
	 * Encrypts sensitive card payload (PAN, CVV, Expiry) with Public Key (client-side simulation).
	 */
	public String encryptWithPublicKey(String plainText)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic());
		byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	/**
	 * Decrypts encrypted card cryptogram/token using the Merchant/Gateway Private Key.
	 */
	public String decryptWithPrivateKey(String base64EncryptedText)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
		byte[] decodedBytes = Base64.getDecoder().decode(base64EncryptedText);
		byte[] decryptedBytes = cipher.doFinal(decodedBytes);
		return new String(decryptedBytes, StandardCharsets.UTF_8);
	}

	/**
	 * Generates a SHA256withRSA digital signature for a payment transaction payload.
	 */
	public String signPayload(String payload)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(rsaKeyPair.getPrivate());
		signature.update(payload.getBytes(StandardCharsets.UTF_8));
		byte[] signatureBytes = signature.sign();
		return Base64.getEncoder().encodeToString(signatureBytes);
	}

	/**
	 * Verifies a SHA256withRSA digital signature using the Public Key (for Visa/Mastercard webhooks).
	 */
	public boolean verifySignature(String payload, String base64Signature) {
		try {
			Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
			signature.initVerify(rsaKeyPair.getPublic());
			signature.update(payload.getBytes(StandardCharsets.UTF_8));
			byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
			return signature.verify(signatureBytes);
		} catch (Exception ex) {
			log.warn("Cryptographic signature verification failed: {}", ex.getMessage());
			return false;
		}
	}

	public PublicKey getPublicKey() {
		return rsaKeyPair.getPublic();
	}

	public PrivateKey getPrivateKey() {
		return rsaKeyPair.getPrivate();
	}
}
