package com.demo.payment.model;

import java.math.BigDecimal;

import com.demo.payment.entity.PaymentMethod;

/**
 * Java 21 Sealed Interface hierarchy modeling industry-standard payment instruments.
 * 
 * Using sealed interfaces restricts implementation to explicit permitted records,
 * allowing compiler-checked exhaustive Pattern Matching for switch without default cases.
 */
public sealed interface PaymentInstrument permits
		PaymentInstrument.CardInstrument,
		PaymentInstrument.UpiInstrument,
		PaymentInstrument.NetBankingInstrument,
		PaymentInstrument.WalletInstrument,
		PaymentInstrument.BnplInstrument,
		PaymentInstrument.DirectDebitMandateInstrument,
		PaymentInstrument.EmiInstrument,
		PaymentInstrument.BankTransferInstrument {

	PaymentMethod getPaymentMethod();

	String getInstrumentSummary();

	/**
	 * 1. Credit / Debit Card instrument (PCI-DSS tokenized).
	 */
	record CardInstrument(
			String cardToken,
			String last4,
			String brand,
			boolean isCredit
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return isCredit ? PaymentMethod.CREDIT_CARD : PaymentMethod.DEBIT_CARD;
		}

		@Override
		public String getInstrumentSummary() {
			return (isCredit ? "Credit Card [" : "Debit Card [") + brand + " **** " + last4 + "]";
		}
	}

	/**
	 * 2. UPI instrument (India NPCI Standard).
	 */
	record UpiInstrument(
			String vpa,
			String qrReference
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.UPI;
		}

		@Override
		public String getInstrumentSummary() {
			return "UPI VPA [" + vpa + "]";
		}
	}

	/**
	 * 3. Net Banking instrument.
	 */
	record NetBankingInstrument(
			String bankCode,
			String bankName
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.NET_BANKING;
		}

		@Override
		public String getInstrumentSummary() {
			return "Net Banking [" + bankCode + "]";
		}
	}

	/**
	 * 4. Digital Wallet instrument.
	 */
	record WalletInstrument(
			String walletProvider,
			String accountIdentifier
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.WALLET;
		}

		@Override
		public String getInstrumentSummary() {
			return "Wallet [" + walletProvider + "]";
		}
	}

	/**
	 * 5. BNPL (Buy Now Pay Later) instrument.
	 */
	record BnplInstrument(
			String bnplProvider,
			Integer repaymentDays
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.BNPL;
		}

		@Override
		public String getInstrumentSummary() {
			return "BNPL [" + bnplProvider + ", Repayment: " + repaymentDays + " days]";
		}
	}

	/**
	 * 6. Direct Debit / Mandate instrument (ACH, SEPA, NACH).
	 */
	record DirectDebitMandateInstrument(
			String mandateReference,
			String bankAccountMask
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.EMANDATE;
		}

		@Override
		public String getInstrumentSummary() {
			return "Direct Debit Mandate [" + mandateReference + "]";
		}
	}

	/**
	 * 7. EMI / Installment instrument.
	 */
	record EmiInstrument(
			String cardToken,
			Integer tenureMonths,
			BigDecimal monthlyInstallment
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.EMI;
		}

		@Override
		public String getInstrumentSummary() {
			return "EMI [" + tenureMonths + " months @ " + monthlyInstallment + "/month]";
		}
	}

	/**
	 * 8. Bank Transfer (IMPS/NEFT/RTGS/Wire) instrument.
	 */
	record BankTransferInstrument(
			String accountReference,
			String routingOrIfsc
	) implements PaymentInstrument {
		@Override
		public PaymentMethod getPaymentMethod() {
			return PaymentMethod.BANK_TRANSFER;
		}

		@Override
		public String getInstrumentSummary() {
			return "Bank Transfer [" + routingOrIfsc + " / " + accountReference + "]";
		}
	}
}
