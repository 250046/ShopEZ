# ShopEZ — Java OOP E-Commerce Desktop Application

A full-featured JavaFX e-commerce desktop application demonstrating core OOP concepts.

## Tech Stack

- **Language**: Java 17
- **UI**: JavaFX 17 + FXML
- **Database**: MySQL 8 (via JDBC)
- **Build**: Maven

## Setup

### Prerequisites
- Java 17+ (JDK)
- MySQL 8.0+
- Maven 3.8+

### Database Setup
1. Start your MySQL server
2. Run the schema file:
   ```bash
   mysql -u root -p < schema.sql
   ```
3. Update `src/main/resources/db.properties` with your MySQL credentials

### Build & Run
```bash
mvn clean package
mvn javafx:run
```

## Default Accounts

| Role     | Email              | Password     |
|----------|--------------------|--------------|
| Admin    | admin@shopez.com   | admin123     |
| Seller   | alice@shopez.com   | seller123    |
| Seller   | bob@shopez.com     | seller123    |
| Customer | john@shopez.com    | customer123  |
| Customer | sara@shopez.com    | customer123  |
| Customer | timur@shopez.com   | customer123  |

## OOP Concepts Demonstrated

- Abstract classes (User, Product, Payment)
- Inheritance (Customer/Seller/Admin, Physical/DigitalProduct, Credit/WalletPayment)
- Polymorphism (calculateFinalPrice(), processPayment(), getDashboardTitle())
- Interfaces (StockNotifiable)
- Encapsulation (private fields with getters/setters)
- Singleton pattern (DBConnection, SessionManager)
- Observer pattern (StockObserver / StockNotifiable)
- Exception handling (InsufficientFundsException, OutOfStockException)
- JDBC with PreparedStatement
- JavaFX GUI with FXML
- Enums (OrderStatus, PaymentStatus)
