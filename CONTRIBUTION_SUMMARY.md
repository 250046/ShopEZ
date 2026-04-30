# ShopEZ - Team Contribution Summary

## Project Repository
**GitHub URL**: https://github.com/250046/ShopEZ

## Team Members & Contributions

### 1. Asadbek (250046@newuu.uz) - 6 commits
**Role**: Core Infrastructure & Database Lead

**Contributions**:
- Database schema design and implementation (schema.sql)
- Maven project configuration (pom.xml)
- Project documentation (README.md)
- Application entry point (Main.java)
- Database connection with Singleton pattern (DBConnection.java)
- Utility classes:
  - PasswordUtil: Password hashing with SHA-256
  - SessionManager: User session management
  - AlertUtil: JavaFX alert dialogs
- Application CSS styling

### 2. Sadi (250122@newuu.uz) - 4 commits
**Role**: User Management & Authentication Lead

**Contributions**:
- User model hierarchy with inheritance:
  - Abstract User base class
  - Customer, Seller, Admin subclasses
- UserDAO for database operations
- Login screen and controller
- User registration functionality
- Authentication and session management integration

### 3. Sardor (250408@newuu.uz) - 4 commits
**Role**: Product Management Lead

**Contributions**:
- Product model hierarchy:
  - Abstract Product base class
  - PhysicalProduct (weight, dimensions, shipping)
  - DigitalProduct (download URL, license key)
- ProductDAO with CRUD operations
- Product listing page with search and filtering
- Product detail page with reviews integration

### 4. Islom (250030@newuu.uz) - 4 commits
**Role**: Seller Features Lead

**Contributions**:
- Seller dashboard with statistics
- Seller product management interface
- Add/Edit product form with validation
- Seller order management system
- Product activation/deactivation functionality

### 5. Muhammadyusuf (250143@newuu.uz) - 5 commits
**Role**: Customer Shopping Features Lead

**Contributions**:
- Shopping cart models (Cart, CartItem)
- CartDAO for cart operations
- Shopping cart page with quantity controls
- Customer dashboard
- Checkout process with payment integration

### 6. Sherbek (250110@newuu.uz) - 7 commits
**Role**: Orders, Payments & Advanced Features Lead

**Contributions**:
- Order models (Order, OrderItem)
- Payment system with polymorphism:
  - Abstract Payment base class
  - CreditCardPayment implementation
  - WalletPayment implementation
- OrderDAO and PaymentDAO
- Product review system
- Observer pattern for stock notifications
- Order history page
- Admin dashboard with user management

## Project Statistics

- **Total Commits**: 30
- **Total Files**: 60+ Java files, 13 FXML files
- **Lines of Code**: ~6,000+ lines
- **Technologies**: Java 17, JavaFX 17, MySQL 8, Maven

## OOP Concepts Demonstrated

1. **Inheritance**: User → Customer/Seller/Admin, Product → Physical/Digital
2. **Polymorphism**: getDashboardTitle(), calculateFinalPrice(), processPayment()
3. **Abstraction**: Abstract classes (User, Product, Payment)
4. **Encapsulation**: Private fields with getters/setters
5. **Interfaces**: StockNotifiable
6. **Design Patterns**:
   - Singleton (DBConnection, SessionManager)
   - Observer (Stock notification system)
7. **Exception Handling**: Custom exceptions (InsufficientFundsException, OutOfStockException)

## How to View Contributions on GitHub

1. Visit: https://github.com/250046/ShopEZ
2. Click on "Insights" → "Contributors" to see contribution graphs
3. Click on "Commits" to see detailed commit history
4. Each commit shows the author's name and email

## Repository Setup Details

- **Branch**: main
- **Remote**: origin (https://github.com/250046/ShopEZ.git)
- **Collaborators**: All 6 team members added with write access
- **Commit History**: Properly attributed to each team member with their university emails

## Next Steps

1. ✅ Repository created and pushed to GitHub
2. ✅ All team members added as collaborators
3. ✅ Commit history properly attributed
4. 🔄 Team members can now clone and work on the project
5. 🔄 Set up MySQL database using schema.sql
6. 🔄 Configure db.properties with database credentials
7. 🔄 Run the application: `mvn javafx:run`

## Clone Instructions for Team Members

```bash
git clone https://github.com/250046/ShopEZ.git
cd ShopEZ
mvn clean install
```

## Important Notes

- Each team member's contributions are properly tracked in git history
- GitHub will show contribution graphs for each member
- The commit messages clearly describe what each person implemented
- All commits use official university email addresses (@newuu.uz)
