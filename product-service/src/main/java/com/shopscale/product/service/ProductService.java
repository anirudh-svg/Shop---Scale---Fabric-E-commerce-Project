package com.shopscale.product.service;

import com.shopscale.product.domain.Product;
import com.shopscale.product.dto.CreateProductRequest;
import com.shopscale.product.exception.ProductNotFoundException;
import com.shopscale.product.mapper.ProductMapper;
import com.shopscale.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing products with Redis caching
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final String PRODUCT_CACHE = "products";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    /**
     * Create a new product
     */
    @CacheEvict(value = PRODUCT_CACHE, allEntries = true)
    public Product createProduct(CreateProductRequest request) {
        logger.info("Creating new product: {}", request.getName());

        Product product = productMapper.toEntity(request);
        product.setProductId(UUID.randomUUID().toString());

        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully: {}", savedProduct.getProductId());

        return savedProduct;
    }

    /**
     * Get product by ID with caching
     */
    @Cacheable(value = PRODUCT_CACHE, key = "#productId", unless = "#result == null")
    public Product getProduct(String productId) {
        logger.info("Fetching product: {}", productId);

        return productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
    }

    /**
     * Get all active products with caching
     */
    @Cacheable(value = PRODUCT_CACHE, key = "'all'")
    public List<Product> getAllProducts() {
        logger.info("Fetching all active products");
        return productRepository.findByActiveTrue();
    }

    /**
     * Get products by category with caching
     */
    @Cacheable(value = PRODUCT_CACHE, key = "'category:' + #category")
    public List<Product> getProductsByCategory(String category) {
        logger.info("Fetching products for category: {}", category);
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    /**
     * Search products by name with caching
     */
    @Cacheable(value = PRODUCT_CACHE, key = "'search:' + #name")
    public List<Product> searchProducts(String name) {
        logger.info("Searching products with name: {}", name);
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name);
    }

    /**
     * Get products by price range with caching
     */
    @Cacheable(value = PRODUCT_CACHE, key = "'price:' + #minPrice + '-' + #maxPrice")
    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        logger.info("Fetching products in price range: {} - {}", minPrice, maxPrice);
        return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice);
    }

    /**
     * Get in-stock products with caching
     */
    @Cacheable(value = PRODUCT_CACHE, key = "'instock'")
    public List<Product> getInStockProducts() {
        logger.info("Fetching in-stock products");
        return productRepository.findInStockProducts();
    }

    /**
     * Update product stock and evict cache
     */
    @CacheEvict(value = PRODUCT_CACHE, allEntries = true)
    public Product updateStock(String productId, Integer quantity) {
        logger.info("Updating stock for product: {} to quantity: {}", productId, quantity);

        Product product = getProduct(productId);
        product.updateStock(quantity);

        Product updatedProduct = productRepository.save(product);
        logger.info("Stock updated successfully for product: {}", productId);

        return updatedProduct;
    }

    /**
     * Update product price and evict cache
     */
    @CacheEvict(value = PRODUCT_CACHE, allEntries = true)
    public Product updatePrice(String productId, BigDecimal newPrice) {
        logger.info("Updating price for product: {} to: {}", productId, newPrice);

        Product product = getProduct(productId);
        product.updatePrice(newPrice);

        Product updatedProduct = productRepository.save(product);
        logger.info("Price updated successfully for product: {}", productId);

        return updatedProduct;
    }

    /**
     * Deactivate product and evict cache
     */
    @CacheEvict(value = PRODUCT_CACHE, allEntries = true)
    public void deactivateProduct(String productId) {
        logger.info("Deactivating product: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

        product.deactivate();
        productRepository.save(product);

        logger.info("Product deactivated successfully: {}", productId);
    }
}
