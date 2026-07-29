package com.demo.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.demo.payment.dto.PaymentRequest;
import com.demo.payment.entity.PaymentMethod;
import com.demo.payment.security.PaymentCryptographyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production-ready Simulated Payment Gateway Provider supporting industry standards:
 * Cards (with RSA-2048 Public/Private key security), Instant RTP (UPI, PIX, FedNow), Net Banking, Wallets, Direct Debit Mandates, EMI, and Alternative Payments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimulatedPaymentGatewayProvider implements PaymentGatewayProvider {

	private static final Pattern UPI_VPA_PATTERN = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$");

	private final PaymentCryptographyService cryptographyService;

	@Override
	public GatewayChargeResult charge(PaymentRequest request) {
		log.info("Gateway [{}] charging order {} via method [{}] for amount {} {}",
				request.gatewayProvider() != null ? request.gatewayProvider() : "SIMULATED_GATEWAY",
				request.orderId(), request.paymentMethod(), request.amount(), request.currency());

		// 1. Check for simulation test tokens (DECLINE, FRAUD)
		if (request.paymentToken() != null) {
			if (request.paymentToken().equalsIgnoreCase("FAIL") || request.paymentToken().equalsIgnoreCase("DECLINE")) {
				log.warn("Gateway simulation DECLINED by issuing bank for order {}", request.orderId());
				return GatewayChargeResult.failed("Card/Instrument declined by issuing bank");
			}
			if (request.paymentToken().equalsIgnoreCase("FRAUD")) {
				log.warn("Gateway simulation flagged as FRAUD for order {}", request.orderId());
				return GatewayChargeResult.failed("Transaction blocked by automated risk and anti-fraud rules");
			}
		}

		// 2. Dispatch to industry-standard method handlers covering all enum constants exhaustively
		return switch (request.paymentMethod()) {
		case CREDIT_CARD, DEBIT_CARD, PREPAID_CARD, CORPORATE_CARD, CARD -> processCardPayment(request);
		case UPI, PIX, FASTER_PAYMENTS, FEDNOW, SEPA_INSTANT, QR_CODE -> processInstantRtpPayment(request);
		case NET_BANKING, BANK_TRANSFER -> processNetBankingPayment(request);
		case WALLET, MOBILE_WALLET, BNPL -> processWalletOrBnplPayment(request);
		case ACH_DIRECT_DEBIT, SEPA_DIRECT_DEBIT, EMANDATE -> processDirectDebitPayment(request);
		case EMI -> processEmiPayment(request);
		case GIFT_CARD, REWARD_POINTS, CASH_ON_DELIVERY, POS_TERMINAL, CBDC -> processAlternativePayment(request);
		};
	}

	private GatewayChargeResult processCardPayment(PaymentRequest request) {
		String brand = request.cardBrand() != null ? request.cardBrand() : "VISA";
		String last4 = request.cardLast4() != null ? request.cardLast4() : "4242";
		log.info("Simulating PCI-DSS Tokenized Card charge [{}] ending in {} (3D-Secure Verification OK)", brand, last4);

		String txRef = "CARD-TX-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		try {
			// Generate RSA-2048 Digital Signature for card authorization cryptogram
			String authPayload = request.orderId() + ":" + txRef + ":" + request.amount();
			String signature = cryptographyService.signPayload(authPayload);
			log.info("Generated Visa/Mastercard RSA-2048 cryptogram signature: {}...", signature.substring(0, 16));
		} catch (Exception ex) {
			log.warn("Cryptogram signing warning: {}", ex.getMessage());
		}
		return GatewayChargeResult.successful(txRef);
	}

	private GatewayChargeResult processInstantRtpPayment(PaymentRequest request) {
		if (request.paymentMethod() == PaymentMethod.UPI && request.upiVpa() != null && !UPI_VPA_PATTERN.matcher(request.upiVpa()).matches()) {
			log.warn("Invalid UPI VPA format provided: {}", request.upiVpa());
			return GatewayChargeResult.failed("Invalid UPI Virtual Payment Address (VPA) format. Must be user@bank");
		}
		String identifier = request.upiVpa() != null ? request.upiVpa() : "user@bank";
		log.info("Simulating Real-Time Instant Payment [{}] for identifier [{}]", request.paymentMethod(), identifier);

		String utrRef = "RTP-UTR-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		return GatewayChargeResult.successful(utrRef);
	}

	private GatewayChargeResult processNetBankingPayment(PaymentRequest request) {
		String bank = request.bankCode() != null ? request.bankCode() : "HDFC_BANK";
		log.info("Simulating NetBanking redirection & OTP verification with bank code [{}]", bank);

		String nbRef = "NB-REF-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		return GatewayChargeResult.successful(nbRef);
	}

	private GatewayChargeResult processWalletOrBnplPayment(PaymentRequest request) {
		String provider = request.walletProvider() != null ? request.walletProvider() : "PAYTM_WALLET";
		log.info("Simulating instant digital wallet/BNPL/Mobile debit via provider [{}]", provider);

		String prefix = request.paymentMethod() == PaymentMethod.BNPL ? "BNPL-TX-" : "WLT-TX-";
		String txRef = prefix + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		return GatewayChargeResult.successful(txRef);
	}

	private GatewayChargeResult processDirectDebitPayment(PaymentRequest request) {
		String mandate = request.mandateReference() != null ? request.mandateReference() : "MND-DEFAULT";
		log.info("Simulating recurring Direct Debit / Mandate payment against mandate reference [{}]", mandate);

		String txRef = "MND-TX-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		return GatewayChargeResult.successful(txRef);
	}

	private GatewayChargeResult processEmiPayment(PaymentRequest request) {
		int months = request.emiTenureMonths() != null ? request.emiTenureMonths() : 6;
		log.info("Simulating Equated Monthly Installment (EMI) payment over {} months tenure", months);

		String txRef = "EMI-TX-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		return GatewayChargeResult.successful(txRef);
	}

	private GatewayChargeResult processAlternativePayment(PaymentRequest request) {
		log.info("Simulating alternative payment instrument [{}]", request.paymentMethod());
		String txRef = "ALT-TX-" + UUID.randomUUID().toString().toUpperCase().substring(0, 16);
		return GatewayChargeResult.successful(txRef);
	}

	@Override
	public GatewayRefundResult refund(String transactionReference, BigDecimal amount) {
		log.info("Simulating gateway refund for reference: {}, amount: {}", transactionReference, amount);
		if (transactionReference == null || transactionReference.isBlank()) {
			return GatewayRefundResult.failed("Invalid transaction reference for refund");
		}
		String refundRef = "RF-" + UUID.randomUUID().toString().toUpperCase().substring(0, 18);
		return GatewayRefundResult.successful(refundRef);
	}
}
