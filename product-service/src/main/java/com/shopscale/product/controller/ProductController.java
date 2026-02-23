package com.shopscale.product.controller;

import com.shopscale.product.domain.Product;
import com.shopscale.product.dto.CreateProductRequest;
import com.shopscale.product.dto.ProductResponse;
import com.shopscale.product.mapper.ProductMapper;
import com.shopscale.product.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for Product operations
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    /**
     * Create a new product
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        logger.info("Received request to create product: {}", request.getName());

        Product product = productService.createProduct(request);
        ProductResponse response = productMapper.toResponse(product);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get product by ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String productId) {
        logger.info("Received request to get product: {}", productId);

        Product product = productService.getProduct(productId);
        ProductResponse response = productMapper.toResponse(product);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") boolean inStockOnly) {

        logger.info("Received request to get products with filters - category: {}, search: {}, minPrice: {}, maxPrice: {}, inStockOnly: {}",
                category, search, minPrice, maxPrice, inStockOnly);

        List<Product> products;

        if (inStockOnly) {
            products = productService.getInStockProducts();
        } else if (search != null && !search.isEmpty()) {
            products = productService.searchProducts(search);
        } else if (category != null && !category.isEmpty()) {
            products = productService.getProductsByCategory(category);
        } else if (minPrice != null && maxPrice != null) {
            products = productService.getProductsByPriceRange(minPrice, maxPrice);
        } else {
            products = productService.getAllProducts();
        }

        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Update product stock
     */
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {

        logger.info("Received request to update stock for product: {} to quantity: {}", productId, quantity);

        Product product = productService.updateStock(productId, quantity);
        ProductResponse response = productMapper.toResponse(product);

        return ResponseEntity.ok(response);
    }

    /**
     * Update product price
     */
    @PatchMapping("/{productId}/price")
    public ResponseEntity<ProductResponse> updatePrice(
            @PathVariable String productId,
            @RequestParam BigDecimal price) {

        logger.info("Received request to update price for product: {} to: {}", productId, price);

        Product product = productService.updatePrice(productId, price);
        ProductResponse response = productMapper.toResponse(product);

        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate product
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable String productId) {
        logger.info("Received request to deactivate product: {}", productId);

        productService.deactivateProduct(productId);

        return ResponseEntity.noContent().build();
    }
}
