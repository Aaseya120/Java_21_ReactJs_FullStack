package com.demo.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.demo.payment.dto.PaymentResponse;
import com.demo.payment.entity.Payment;
import com.demo.payment.entity.PaymentStatus;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

	@Mapping(target = "statusDescription", source = "status", qualifiedByName = "resolveStatusDescription")
	PaymentResponse toResponse(Payment payment);

	@Named("resolveStatusDescription")
	default String resolveStatusDescription(PaymentStatus status) {
		if (status == null)
			return null;
		return switch (status) {
		case PENDING -> "Payment is being processed";
		case SUCCESS -> "Payment completed successfully";
		case FAILED -> "Payment processing failed";
		case REFUNDED -> "Payment has been refunded";
		};
	}
}
