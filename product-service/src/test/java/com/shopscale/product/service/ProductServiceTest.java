package com.shopscale.product.service;

import com.shopscale.product.domain.Product;
import com.shopscale.product.dto.CreateProductRequest;
import com.shopscale.product.exception.ProductNotFoundException;
import com.shopscale.product.mapper.ProductMapper;
import com.shopscale.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        testProduct = new Product(
                "Test Laptop",
                "High-performance laptop",
                new BigDecimal("1299.99"),
                "electronics",
                50
        );
        testProduct.setProductId("prod_001");

        createRequest = new CreateProductRequest(
                "Test Laptop",
                "High-performance laptop",
                new BigDecimal("1299.99"),
                "electronics",
                50
        );
    }

    @Test
    void createProduct_ShouldCreateProductSuccessfully() {
        // Given
        when(productMapper.toEntity(createRequest)).thenReturn(testProduct);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.createProduct(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Laptop");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProduct_ShouldReturnProductWhenExists() {
        // Given
        when(productRepository.findByProductIdAndActiveTrue("prod_001"))
                .thenReturn(Optional.of(testProduct));

        // When
        Product result = productService.getProduct("prod_001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("prod_001");
        verify(productRepository).findByProductIdAndActiveTrue("prod_001");
    }

    @Test
    void getProduct_ShouldThrowExceptionWhenNotExists() {
        // Given
        when(productRepository.findByProductIdAndActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProduct("nonexistent"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getAllProducts_ShouldReturnAllActiveProducts() {
        // Given
        List<Product> products = List.of(testProduct);
        when(productRepository.findByActiveTrue()).thenReturn(products);

        // When
        List<Product> result = productService.getAllProducts();

        // Then
        assertThat(result).hasSize(1);
        verify(productRepository).findByActiveTrue();
    }

    @Test
    void getProductsByCategory_ShouldReturnProductsInCategory() {
        // Given
        List<Product> products = List.of(testProduct);
        when(productRepository.findByCategoryAndActiveTrue("electronics"))
                .thenReturn(products);

        // When
        List<Product> result = productService.getProductsByCategory("electronics");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("electronics");
        verify(productRepository).findByCategoryAndActiveTrue("electronics");
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() {
        // Given
        List<Product> products = List.of(testProduct);
        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue("laptop"))
                .thenReturn(products);

        // When
        List<Product> result = productService.searchProducts("laptop");

        // Then
        assertThat(result).hasSize(1);
        verify(productRepository).findByNameContainingIgnoreCaseAndActiveTrue("laptop");
    }

    @Test
    void getProductsByPriceRange_ShouldReturnProductsInRange() {
        // Given
        BigDecimal minPrice = new BigDecimal("1000.00");
        BigDecimal maxPrice = new BigDecimal("1500.00");
        List<Product> products = List.of(testProduct);
        when(productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice))
                .thenReturn(products);

        // When
        List<Product> result = productService.getProductsByPriceRange(minPrice, maxPrice);

        // Then
        assertThat(result).hasSize(1);
        verify(productRepository).findByPriceBetweenAndActiveTrue(minPrice, maxPrice);
    }

    @Test
    void getInStockProducts_ShouldReturnOnlyInStockProducts() {
        // Given
        List<Product> products = List.of(testProduct);
        when(productRepository.findInStockProducts()).thenReturn(products);

        // When
        List<Product> result = productService.getInStockProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockQuantity()).isGreaterThan(0);
        verify(productRepository).findInStockProducts();
    }

    @Test
    void updateStock_ShouldUpdateStockQuantity() {
        // Given
        when(productRepository.findByProductIdAndActiveTrue("prod_001"))
                .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.updateStock("prod_001", 100);

        // Then
        assertThat(result.getStockQuantity()).isEqualTo(100);
        verify(productRepository).save(testProduct);
    }

    @Test
    void updatePrice_ShouldUpdateProductPrice() {
        // Given
        BigDecimal newPrice = new BigDecimal("1499.99");
        when(productRepository.findByProductIdAndActiveTrue("prod_001"))
                .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.updatePrice("prod_001", newPrice);

        // Then
        assertThat(result.getPrice()).isEqualTo(newPrice);
        verify(productRepository).save(testProduct);
    }

    @Test
    void deactivateProduct_ShouldSetProductToInactive() {
        // Given
        when(productRepository.findById("prod_001")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.deactivateProduct("prod_001");

        // Then
        assertThat(testProduct.isActive()).isFalse();
        verify(productRepository).save(testProduct);
    }

    @Test
    void deactivateProduct_ShouldThrowExceptionWhenNotExists() {
        // Given
        when(productRepository.findById(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.deactivateProduct("nonexistent"))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
