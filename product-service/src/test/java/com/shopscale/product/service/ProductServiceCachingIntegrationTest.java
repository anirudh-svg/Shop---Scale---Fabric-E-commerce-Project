package com.shopscale.product.service;

import com.shopscale.product.domain.Product;
import com.shopscale.product.dto.CreateProductRequest;
import com.shopscale.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ProductService Redis caching behavior
 * Tests Requirements: 6.1, 6.2, 6.3
 * 
 * Note: These tests require Docker to be running. They will be skipped if Docker is not available.
 * To run these tests, ensure Docker is running and set DOCKER_AVAILABLE=true environment variable.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true", disabledReason = "Docker is not available")
class ProductServiceCachingIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clear database and cache
        productRepository.deleteAll();
        if (cacheManager.getCacheNames() != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }

        // Create test product
        CreateProductRequest request = new CreateProductRequest(
                "Test Laptop",
                "High-performance laptop",
                new BigDecimal("1299.99"),
                "electronics",
                50
        );
        testProduct = productService.createProduct(request);
    }

    @Test
    void getProduct_ShouldCacheResult() {
        // First call - should hit database and cache result
        Product product1 = productService.getProduct(testProduct.getProductId());
        assertThat(product1).isNotNull();
        assertThat(product1.getName()).isEqualTo("Test Laptop");

        // Verify cache contains the product
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        var cachedValue = cache.get(testProduct.getProductId());
        assertThat(cachedValue).isNotNull();

        // Second call - should hit cache (verify by checking it returns same instance)
        Product product2 = productService.getProduct(testProduct.getProductId());
        assertThat(product2).isNotNull();
        assertThat(product2.getProductId()).isEqualTo(product1.getProductId());
    }

    @Test
    void getAllProducts_ShouldCacheResult() {
        // First call
        List<Product> products1 = productService.getAllProducts();
        assertThat(products1).hasSize(1);

        // Verify cache
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        var cachedValue = cache.get("all");
        assertThat(cachedValue).isNotNull();

        // Second call - should use cache
        List<Product> products2 = productService.getAllProducts();
        assertThat(products2).hasSize(1);
    }

    @Test
    void getProductsByCategory_ShouldCacheResult() {
        // First call
        List<Product> products1 = productService.getProductsByCategory("electronics");
        assertThat(products1).hasSize(1);

        // Verify cache
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        var cachedValue = cache.get("category:electronics");
        assertThat(cachedValue).isNotNull();

        // Second call - should use cache
        List<Product> products2 = productService.getProductsByCategory("electronics");
        assertThat(products2).hasSize(1);
    }

    @Test
    void searchProducts_ShouldCacheResult() {
        // First call
        List<Product> products1 = productService.searchProducts("laptop");
        assertThat(products1).hasSize(1);

        // Verify cache
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        var cachedValue = cache.get("search:laptop");
        assertThat(cachedValue).isNotNull();

        // Second call - should use cache
        List<Product> products2 = productService.searchProducts("laptop");
        assertThat(products2).hasSize(1);
    }

    @Test
    void updateStock_ShouldEvictCache() {
        // Cache the product
        productService.getProduct(testProduct.getProductId());
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        assertThat(cache.get(testProduct.getProductId())).isNotNull();

        // Update stock - should evict cache
        productService.updateStock(testProduct.getProductId(), 100);

        // Cache should be cleared
        assertThat(cache.get(testProduct.getProductId())).isNull();
    }

    @Test
    void updatePrice_ShouldEvictCache() {
        // Cache the product
        productService.getProduct(testProduct.getProductId());
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        assertThat(cache.get(testProduct.getProductId())).isNotNull();

        // Update price - should evict cache
        productService.updatePrice(testProduct.getProductId(), new BigDecimal("1499.99"));

        // Cache should be cleared
        assertThat(cache.get(testProduct.getProductId())).isNull();
    }

    @Test
    void createProduct_ShouldEvictCache() {
        // Cache all products
        productService.getAllProducts();
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        assertThat(cache.get("all")).isNotNull();

        // Create new product - should evict cache
        CreateProductRequest newRequest = new CreateProductRequest(
                "New Product",
                "Brand new",
                new BigDecimal("199.99"),
                "accessories",
                100
        );
        productService.createProduct(newRequest);

        // Cache should be cleared
        assertThat(cache.get("all")).isNull();
    }

    @Test
    void deactivateProduct_ShouldEvictCache() {
        // Cache the product
        productService.getProduct(testProduct.getProductId());
        var cache = cacheManager.getCache("products");
        assertThat(cache).isNotNull();
        assertThat(cache.get(testProduct.getProductId())).isNotNull();

        // Deactivate product - should evict cache
        productService.deactivateProduct(testProduct.getProductId());

        // Cache should be cleared
        assertThat(cache.get(testProduct.getProductId())).isNull();
    }
}
