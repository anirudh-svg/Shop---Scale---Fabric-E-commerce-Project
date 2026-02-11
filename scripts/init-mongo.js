// Initialize MongoDB for ShopScale Fabric
db = db.getSiblingDB('productdb');

// Create collections
db.createCollection('products');
db.createCollection('categories');

// Create indexes for better performance
db.products.createIndex({ "productId": 1 }, { unique: true });
db.products.createIndex({ "category": 1 });
db.products.createIndex({ "name": "text", "description": "text" });
db.products.createIndex({ "price": 1 });
db.products.createIndex({ "createdAt": 1 });

// Insert sample data
db.products.insertMany([
    {
        "productId": "prod_001",
        "name": "Premium Laptop",
        "description": "High-performance laptop for professionals with 16GB RAM and 512GB SSD",
        "price": 1299.99,
        "category": "electronics",
        "stockQuantity": 50,
        "attributes": {
            "brand": "TechCorp",
            "model": "Pro-X1",
            "specifications": {
                "ram": "16GB",
                "storage": "512GB SSD",
                "processor": "Intel i7",
                "screen": "15.6 inch 4K"
            }
        },
        "createdAt": new Date(),
        "updatedAt": new Date()
    },
    {
        "productId": "prod_002",
        "name": "Wireless Headphones",
        "description": "Premium noise-cancelling wireless headphones with 30-hour battery life",
        "price": 299.99,
        "category": "electronics",
        "stockQuantity": 100,
        "attributes": {
            "brand": "AudioMax",
            "model": "NC-Pro",
            "specifications": {
                "batteryLife": "30 hours",
                "noiseCancelling": true,
                "wireless": true,
                "color": "Black"
            }
        },
        "createdAt": new Date(),
        "updatedAt": new Date()
    },
    {
        "productId": "prod_003",
        "name": "Smart Watch",
        "description": "Advanced fitness tracking smartwatch with heart rate monitor",
        "price": 399.99,
        "category": "electronics",
        "stockQuantity": 75,
        "attributes": {
            "brand": "FitTech",
            "model": "Smart-Pro",
            "specifications": {
                "batteryLife": "7 days",
                "waterproof": true,
                "heartRateMonitor": true,
                "gps": true
            }
        },
        "createdAt": new Date(),
        "updatedAt": new Date()
    }
]);

db.categories.insertMany([
    {
        "categoryId": "cat_001",
        "name": "Electronics",
        "description": "Electronic devices and gadgets",
        "parentCategory": null,
        "createdAt": new Date()
    },
    {
        "categoryId": "cat_002",
        "name": "Computers",
        "description": "Laptops, desktops, and computer accessories",
        "parentCategory": "cat_001",
        "createdAt": new Date()
    },
    {
        "categoryId": "cat_003",
        "name": "Audio",
        "description": "Headphones, speakers, and audio equipment",
        "parentCategory": "cat_001",
        "createdAt": new Date()
    }
]);

print("MongoDB initialization completed successfully!");