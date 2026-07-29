package com.demo.payment.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.demo.payment.entity.PaymentMethod;

class PaymentInstrumentTest {

	@Test
	void testSealedInterfacePatternMatching_ExhaustiveSwitch() {
		PaymentInstrument card = new PaymentInstrument.CardInstrument("tok_4242", "4242", "VISA", true);
		PaymentInstrument upi = new PaymentInstrument.UpiInstrument("user@okicici", null);
		PaymentInstrument emi = new PaymentInstrument.EmiInstrument("tok_emi", 12, BigDecimal.valueOf(100.0));

		assertEquals("Credit Card [VISA **** 4242]", describeInstrument(card));
		assertEquals("UPI VPA [user@okicici]", describeInstrument(upi));
		assertEquals("EMI [12 months @ 100.0/month]", describeInstrument(emi));
		assertEquals(PaymentMethod.CREDIT_CARD, card.getPaymentMethod());
		assertEquals(PaymentMethod.UPI, upi.getPaymentMethod());
		assertEquals(PaymentMethod.EMI, emi.getPaymentMethod());
	}

	/**
	 * Demonstrates Java 21 exhaustive Pattern Matching for switch on a sealed interface.
	 * Notice there is NO default case required because all permitted types are covered.
	 */
	private String describeInstrument(PaymentInstrument instrument) {
		return switch (instrument) {
		case PaymentInstrument.CardInstrument c -> c.getInstrumentSummary();
		case PaymentInstrument.UpiInstrument u -> u.getInstrumentSummary();
		case PaymentInstrument.NetBankingInstrument nb -> nb.getInstrumentSummary();
		case PaymentInstrument.WalletInstrument w -> w.getInstrumentSummary();
		case PaymentInstrument.BnplInstrument bnpl -> bnpl.getInstrumentSummary();
		case PaymentInstrument.DirectDebitMandateInstrument dd -> dd.getInstrumentSummary();
		case PaymentInstrument.EmiInstrument emi -> emi.getInstrumentSummary();
		case PaymentInstrument.BankTransferInstrument bt -> bt.getInstrumentSummary();
		};
	}
}
