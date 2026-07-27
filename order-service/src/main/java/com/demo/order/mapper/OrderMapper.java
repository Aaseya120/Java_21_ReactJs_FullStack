package com.demo.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.demo.order.dto.OrderResponse;
import com.demo.order.entity.Order;
import com.demo.order.entity.OrderStatus;

@Mapper(componentModel = "spring")
public interface OrderMapper {

	@Mapping(target = "statusDescription", source = "status", qualifiedByName = "resolveStatusDescription")
	OrderResponse toResponse(Order order);

	@Named("resolveStatusDescription")
	default String resolveStatusDescription(OrderStatus status) {
		if (status == null)
			return null;
		return switch (status) {
		case PENDING -> "Order received and awaiting confirmation";
		case CONFIRMED -> "Order confirmed by the seller";
		case PROCESSING -> "Order is being prepared";
		case SHIPPED -> "Order has been dispatched";
		case DELIVERED -> "Order successfully delivered";
		case CANCELLED -> "Order was cancelled";
		case REFUNDED -> "Order has been refunded";
		};
	}
}
