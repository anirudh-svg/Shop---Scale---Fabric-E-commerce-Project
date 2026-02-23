package com.shopscale.product.repository;

import com.shopscale.product.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Product documents
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * Find products by category
     */
    List<Product> findByCategoryAndActiveTrue(String category);

    /**
     * Find products by name containing (case-insensitive search)
     */
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    /**
     * Find products within price range
     */
    List<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Find products by category and price range
     */
    List<Product> findByCategoryAndPriceBetweenAndActiveTrue(String category, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Find in-stock products
     */
    @Query("{ 'stockQuantity': { $gt: 0 }, 'active': true }")
    List<Product> findInStockProducts();

    /**
     * Find active products only
     */
    List<Product> findByActiveTrue();

    /**
     * Find product by ID if active
     */
    Optional<Product> findByProductIdAndActiveTrue(String productId);

    /**
     * Count products by category
     */
    long countByCategoryAndActiveTrue(String category);
}
