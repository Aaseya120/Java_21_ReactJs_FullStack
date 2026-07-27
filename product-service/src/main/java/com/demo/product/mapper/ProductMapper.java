package com.demo.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.demo.product.dto.ProductResponse;
import com.demo.product.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	@Mapping(target = "availabilityStatus", source = "stockQty", qualifiedByName = "resolveAvailability")
	ProductResponse toResponse(Product product);

	@Named("resolveAvailability")
	default String resolveAvailability(int stockQty) {
		if (stockQty <= 0)
			return "OUT_OF_STOCK";
		if (stockQty <= 10)
			return "LOW_STOCK";
		return "IN_STOCK";
	}
}
