package com.shopscale.product.mapper;

import com.shopscale.product.domain.Product;
import com.shopscale.product.dto.CreateProductRequest;
import com.shopscale.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for Product entity and DTOs
 */
@Component
public class ProductMapper {

    /**
     * Convert Product entity to ProductResponse DTO
     */
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getStockQuantity(),
                product.isInStock(),
                product.getAttributes(),
                product.getCreatedAt()
        );
    }

    /**
     * Convert CreateProductRequest DTO to Product entity
     */
    public Product toEntity(CreateProductRequest request) {
        if (request == null) {
            return null;
        }

        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.getStockQuantity()
        );

        if (request.getAttributes() != null) {
            product.setAttributes(request.getAttributes());
        }

        return product;
    }
}
