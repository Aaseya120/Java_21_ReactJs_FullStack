package com.demo.payment.entity;

/**
 * Global industry-standard payment methods covering all major financial payment instruments
 * across ISO 20022, PCI-DSS, NPCI (India), SEPA (Europe), ACH/RTP (US), and emerging fintech schemes.
 */
public enum PaymentMethod {
	// 1. Cards (Credit, Debit, Prepaid, Corporate)
	CARD, // General card for backwards compatibility
	CREDIT_CARD,
	DEBIT_CARD,
	PREPAID_CARD,
	CORPORATE_CARD,

	// 2. Real-Time Instant Payments & UPI (India, US, UK, Europe, LatAm)
	UPI,
	PIX,
	FASTER_PAYMENTS,
	FEDNOW,
	SEPA_INSTANT,

	// 3. Digital Wallets & Mobile Contactless (Apple Pay, Google Pay, PayPal, etc.)
	WALLET,
	MOBILE_WALLET,

	// 4. Internet Banking & Bank Redirects (NetBanking, iDEAL, Sofort, etc.)
	NET_BANKING,
	BANK_TRANSFER, // Wire, NEFT, RTGS, IMPS

	// 5. Direct Debit & Recurring Mandates (ACH, SEPA Direct Debit, NACH/eMandate)
	ACH_DIRECT_DEBIT,
	SEPA_DIRECT_DEBIT,
	EMANDATE,

	// 6. Buy Now Pay Later (BNPL) & Installments (EMI)
	BNPL,
	EMI,

	// 7. Vouchers, Gift Cards & Loyalty Reward Points
	GIFT_CARD,
	REWARD_POINTS,

	// 8. Offline, POS & QR Codes
	CASH_ON_DELIVERY,
	QR_CODE,
	POS_TERMINAL,

	// 9. Digital Currencies & CBDC (e-Rupee, Stablecoins)
	CBDC
}
