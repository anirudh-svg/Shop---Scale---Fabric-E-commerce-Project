package com.shopscale.product.repository;

import com.shopscale.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        testProduct = new Product(
                "Test Laptop",
                "High-performance laptop",
                new BigDecimal("1299.99"),
                "electronics",
                50
        );
        testProduct.setProductId("prod_001");
        productRepository.save(testProduct);
    }

    @Test
    void findByCategoryAndActiveTrue_ShouldReturnProductsInCategory() {
        // When
        List<Product> products = productRepository.findByCategoryAndActiveTrue("electronics");

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Test Laptop");
    }

    @Test
    void findByNameContainingIgnoreCaseAndActiveTrue_ShouldReturnMatchingProducts() {
        // When
        List<Product> products = productRepository.findByNameContainingIgnoreCaseAndActiveTrue("laptop");

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).contains("Laptop");
    }

    @Test
    void findByPriceBetweenAndActiveTrue_ShouldReturnProductsInPriceRange() {
        // When
        List<Product> products = productRepository.findByPriceBetweenAndActiveTrue(
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00")
        );

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getPrice()).isBetween(
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00")
        );
    }

    @Test
    void findInStockProducts_ShouldReturnOnlyInStockProducts() {
        // Given
        Product outOfStock = new Product("Out of Stock Item", "No stock", new BigDecimal("99.99"), "electronics", 0);
        outOfStock.setProductId("prod_002");
        productRepository.save(outOfStock);

        // When
        List<Product> products = productRepository.findInStockProducts();

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getStockQuantity()).isGreaterThan(0);
    }

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveProducts() {
        // Given
        Product inactiveProduct = new Product("Inactive Product", "Deactivated", new BigDecimal("50.00"), "electronics", 10);
        inactiveProduct.setProductId("prod_003");
        inactiveProduct.setActive(false);
        productRepository.save(inactiveProduct);

        // When
        List<Product> products = productRepository.findByActiveTrue();

        // Then
        assertThat(products).hasSize(1);
        assertThat(products.get(0).isActive()).isTrue();
    }

    @Test
    void findByProductIdAndActiveTrue_ShouldReturnProductIfActive() {
        // When
        Optional<Product> productOpt = productRepository.findByProductIdAndActiveTrue("prod_001");

        // Then
        assertThat(productOpt).isPresent();
        assertThat(productOpt.get().getName()).isEqualTo("Test Laptop");
    }

    @Test
    void findByProductIdAndActiveTrue_ShouldReturnEmptyIfInactive() {
        // Given
        testProduct.setActive(false);
        productRepository.save(testProduct);

        // When
        Optional<Product> productOpt = productRepository.findByProductIdAndActiveTrue("prod_001");

        // Then
        assertThat(productOpt).isEmpty();
    }

    @Test
    void countByCategoryAndActiveTrue_ShouldReturnCorrectCount() {
        // Given
        Product anotherProduct = new Product("Another Laptop", "Budget laptop", new BigDecimal("599.99"), "electronics", 30);
        anotherProduct.setProductId("prod_004");
        productRepository.save(anotherProduct);

        // When
        long count = productRepository.countByCategoryAndActiveTrue("electronics");

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void save_ShouldPersistProduct() {
        // Given
        Product newProduct = new Product("New Product", "Brand new", new BigDecimal("199.99"), "accessories", 100);
        newProduct.setProductId("prod_005");

        // When
        Product saved = productRepository.save(newProduct);

        // Then
        assertThat(saved.getProductId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Product");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
