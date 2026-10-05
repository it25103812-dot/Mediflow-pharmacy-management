-- ============================================================================
--  MediFlow – Pharmacy Management System
--  LankaCare Pharmacy (Pvt) Ltd
--  Database: mediflow_db   |   MySQL 8.x
--
--  Run:  mysql -u root -p < mediflow.sql
-- ============================================================================

DROP DATABASE IF EXISTS mediflow_db;
CREATE DATABASE mediflow_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mediflow_db;

-- ----------------------------------------------------------------------------
-- 1. ROLES
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 2. USERS
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role_id       BIGINT       NOT NULL,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY fk_users_role (role_id),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 3. CATEGORIES
-- ----------------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NULL,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 4. MEDICINES
-- ----------------------------------------------------------------------------
CREATE TABLE medicines (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    name                   VARCHAR(150)  NOT NULL,
    generic_name           VARCHAR(150)  NULL,
    category_id            BIGINT        NULL,
    manufacturer           VARCHAR(120)  NULL,
    unit_price             DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    description            VARCHAR(500)  NULL,
    prescription_required  TINYINT(1)    NOT NULL DEFAULT 0,
    active                 TINYINT(1)    NOT NULL DEFAULT 1,
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_medicines_category (category_id),
    CONSTRAINT fk_medicines_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 5. MEDICINE BATCHES
-- ----------------------------------------------------------------------------
CREATE TABLE medicine_batches (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    medicine_id    BIGINT        NOT NULL,
    batch_number   VARCHAR(50)   NOT NULL,
    expiry_date    DATE          NOT NULL,
    purchase_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    selling_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_medicine_number (medicine_id, batch_number),
    KEY fk_batches_medicine (medicine_id),
    CONSTRAINT fk_batches_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 6. PRICE HISTORY (unit price changes are never silently lost)
-- ----------------------------------------------------------------------------
CREATE TABLE price_history (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    medicine_id BIGINT        NOT NULL,
    old_price   DECIMAL(12,2) NOT NULL,
    new_price   DECIMAL(12,2) NOT NULL,
    changed_by  BIGINT        NULL,
    changed_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_price_medicine (medicine_id),
    KEY fk_price_user (changed_by),
    CONSTRAINT fk_price_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT fk_price_user     FOREIGN KEY (changed_by)  REFERENCES users (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 7. SUPPLIERS
-- ----------------------------------------------------------------------------
CREATE TABLE suppliers (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100) NULL,
    phone          VARCHAR(30)  NULL,
    email          VARCHAR(100) NULL,
    address        VARCHAR(255) NULL,
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 8. PURCHASE ORDERS
-- ----------------------------------------------------------------------------
CREATE TABLE purchase_orders (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    po_number       VARCHAR(30)   NOT NULL,
    supplier_id     BIGINT        NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    order_date      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_date   DATE          NULL,
    total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    notes           VARCHAR(500)  NULL,
    created_by      BIGINT        NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_po_number (po_number),
    KEY fk_po_supplier (supplier_id),
    KEY fk_po_user (created_by),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_po_user     FOREIGN KEY (created_by)  REFERENCES users (id),
    CONSTRAINT chk_po_status CHECK (status IN ('DRAFT','SENT','PARTIALLY_RECEIVED','RECEIVED','CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE purchase_order_items (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_order_id BIGINT      NOT NULL,
    medicine_id     BIGINT        NOT NULL,
    quantity        INT           NOT NULL,
    received_quantity INT         NOT NULL DEFAULT 0,
    purchase_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    line_total      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    KEY fk_poi_po (purchase_order_id),
    KEY fk_poi_medicine (medicine_id),
    CONSTRAINT fk_poi_po       FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_poi_medicine FOREIGN KEY (medicine_id)       REFERENCES medicines (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 9. GOODS RECEIVED NOTES (stock increases ONLY on VERIFIED GRNs)
-- ----------------------------------------------------------------------------
CREATE TABLE goods_received_notes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    grn_number      VARCHAR(30)  NOT NULL,
    purchase_order_id BIGINT     NOT NULL,
    supplier_id     BIGINT       NOT NULL,
    received_date   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    verified_by     BIGINT       NULL,
    verified_at     DATETIME     NULL,
    notes           VARCHAR(500) NULL,
    created_by      BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grn_number (grn_number),
    KEY fk_grn_po (purchase_order_id),
    KEY fk_grn_supplier (supplier_id),
    CONSTRAINT fk_grn_po       FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id),
    CONSTRAINT fk_grn_supplier FOREIGN KEY (supplier_id)       REFERENCES suppliers (id),
    CONSTRAINT fk_grn_verified FOREIGN KEY (verified_by)       REFERENCES users (id),
    CONSTRAINT fk_grn_creator  FOREIGN KEY (created_by)        REFERENCES users (id),
    CONSTRAINT chk_grn_status CHECK (status IN ('PENDING','VERIFIED','CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE goods_received_note_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    grn_id       BIGINT        NOT NULL,
    medicine_id  BIGINT        NOT NULL,
    po_item_id   BIGINT        NULL,
    batch_number VARCHAR(50)   NOT NULL,
    expiry_date  DATE          NOT NULL,
    quantity     INT           NOT NULL,
    purchase_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    selling_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    KEY fk_gri_grn (grn_id),
    KEY fk_gri_medicine (medicine_id),
    CONSTRAINT fk_gri_grn      FOREIGN KEY (grn_id)      REFERENCES goods_received_notes (id),
    CONSTRAINT fk_gri_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 10. INVENTORY (one row per batch – live, sellable stock)
-- ----------------------------------------------------------------------------
CREATE TABLE inventory (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    batch_id           BIGINT   NOT NULL,
    quantity_available INT      NOT NULL DEFAULT 0,
    reorder_level      INT      NOT NULL DEFAULT 20,
    last_updated       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_batch (batch_id),
    CONSTRAINT fk_inventory_batch FOREIGN KEY (batch_id) REFERENCES medicine_batches (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 11. STOCK ADJUSTMENTS
-- ----------------------------------------------------------------------------
CREATE TABLE stock_adjustments (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    batch_id         BIGINT      NOT NULL,
    adjustment_type  VARCHAR(20) NOT NULL,
    quantity_change  INT         NOT NULL,
    reason           VARCHAR(255) NULL,
    performed_by     BIGINT      NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_adj_batch (batch_id),
    KEY fk_adj_user (performed_by),
    CONSTRAINT fk_adj_batch FOREIGN KEY (batch_id)     REFERENCES medicine_batches (id),
    CONSTRAINT fk_adj_user  FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT chk_adj_type CHECK (adjustment_type IN ('DAMAGE','RETURN','CORRECTION','EXPIRED_DISPOSAL'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 12. CUSTOMERS
-- ----------------------------------------------------------------------------
CREATE TABLE customers (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120) NOT NULL,
    phone       VARCHAR(30)  NULL,
    email       VARCHAR(100) NULL,
    address     VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 13. PRESCRIPTIONS
-- ----------------------------------------------------------------------------
CREATE TABLE prescriptions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    prescription_number VARCHAR(30)  NOT NULL,
    customer_id         BIGINT       NULL,
    doctor_name         VARCHAR(120) NULL,
    prescription_date   DATE         NULL,
    medicine_id         BIGINT       NULL,
    notes               VARCHAR(500) NULL,
    created_by          BIGINT       NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prescription_number (prescription_number),
    KEY fk_prx_customer (customer_id),
    KEY fk_prx_medicine (medicine_id),
    CONSTRAINT fk_prx_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_prx_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT fk_prx_creator  FOREIGN KEY (created_by)  REFERENCES users (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 14. SALES
-- ----------------------------------------------------------------------------
CREATE TABLE sales (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_number  VARCHAR(30)   NOT NULL,
    customer_id     BIGINT        NULL,
    cashier_id      BIGINT        NULL,
    sale_date       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    payment_method  VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    prescription_id BIGINT        NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'COMPLETED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_number (invoice_number),
    KEY fk_sales_customer (customer_id),
    KEY fk_sales_cashier (cashier_id),
    KEY fk_sales_prescription (prescription_id),
    CONSTRAINT fk_sales_customer     FOREIGN KEY (customer_id)     REFERENCES customers (id),
    CONSTRAINT fk_sales_cashier      FOREIGN KEY (cashier_id)      REFERENCES users (id),
    CONSTRAINT fk_sales_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions (id),
    CONSTRAINT chk_sales_payment CHECK (payment_method IN ('CASH','CARD','ONLINE')),
    CONSTRAINT chk_sales_status  CHECK (status IN ('COMPLETED','RETURNED'))
) ENGINE=InnoDB;

CREATE TABLE sale_items (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id    BIGINT        NOT NULL,
    medicine_id BIGINT       NOT NULL,
    batch_id   BIGINT        NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY fk_si_sale (sale_id),
    KEY fk_si_medicine (medicine_id),
    KEY fk_si_batch (batch_id),
    CONSTRAINT fk_si_sale     FOREIGN KEY (sale_id)     REFERENCES sales (id),
    CONSTRAINT fk_si_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT fk_si_batch    FOREIGN KEY (batch_id)    REFERENCES medicine_batches (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 15. PAYMENTS
-- ----------------------------------------------------------------------------
CREATE TABLE payments (
    id      BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id BIGINT        NOT NULL,
    amount  DECIMAL(12,2) NOT NULL,
    method  VARCHAR(20)   NOT NULL,
    paid_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_pay_sale (sale_id),
    CONSTRAINT fk_pay_sale FOREIGN KEY (sale_id) REFERENCES sales (id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 16. AUDIT LOGS
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50)  NULL,
    entity_id   VARCHAR(50)  NULL,
    details     VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_audit_user (user_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ============================================================================
--  SAMPLE DATA
-- ============================================================================

-- Roles -------------------------------------------------------------------
INSERT INTO roles (id, name) VALUES
 (1,'ADMINISTRATOR'),
 (2,'PHARMACIST'),
 (3,'STORE_KEEPER'),
 (4,'PROCUREMENT_OFFICER'),
 (5,'CASHIER'),
 (6,'CUSTOMER_RELATIONS_OFFICER'),
 (7,'FINANCE_MANAGER');

-- Users (password for ALL sample accounts: Admin@123 -- BCrypt, dev only) --
INSERT INTO users (id, first_name, last_name, email, password_hash, role_id, active) VALUES
 (1,'System','Admin','admin@mediflow.com','$2a$10$aEJLLdqF/7gxTwBTFbY9VOVcj0.XNEdvvw.LzgnDIsyTVgw0F/gNq',1,1),
 (2,'Nimali','Perera','pharmacist@mediflow.com','$2a$10$ozBflKzRk6f1HjxDcBd.6eAnXHe06DDKVHfxe5ajVe3Bxmhy6IFdy',2,1),
 (3,'Kasun','Silva','storekeeper@mediflow.com','$2a$10$OdcdK1QiqMbsmzusUI3L/uGkTl48pjXRsgXTkB/abP4iu85u6mZoO',3,1),
 (4,'Ruwan','Fernando','procurement@mediflow.com','$2a$10$r0DDS86/5ahNYztJTE10quevo/.XkhnD9H0XqDQBhKoZWTrw0dFKa',4,1),
 (5,'Sanduni','Jayawardena','cashier@mediflow.com','$2a$10$M77JTd31DQUrWrwGGJ.I7OZ84Qlgt7Ri1N1ubX88U/LXIp59oBMf6',5,1),
 (6,'Dilani','Wickramasinghe','cro@mediflow.com','$2a$10$wBmdGFCcEkpQf.rbi/DyWuJd0uSpG92mR.VcBpfLx8poA18j3SSje',6,1),
 (7,'Tharindu','Rathnayake','finance@mediflow.com','$2a$10$jjMjeD13kQPaXSQULvZSWON6dbSN3KW9YwAGtSjBKJSz3GrzgDgi.',7,1);

-- Categories ----------------------------------------------------------------
INSERT INTO categories (id, name, description) VALUES
 (1,'Analgesics','Pain relief medicines'),
 (2,'Antibiotics','Infection treatment medicines'),
 (3,'Antihistamines','Allergy relief medicines'),
 (4,'Cardiovascular','Heart and blood pressure medicines'),
 (5,'Antidiabetics','Diabetes medicines'),
 (6,'Gastrointestinal','Stomach and digestive medicines'),
 (7,'Respiratory','Asthma and respiratory medicines'),
 (8,'Vitamins & Supplements','Vitamins and health supplements');

-- Medicines -----------------------------------------------------------------
INSERT INTO medicines (id, name, generic_name, category_id, manufacturer, unit_price, description, prescription_required) VALUES
 (1,'Paracetamol 500mg Tablets','Paracetamol',1,'Johnson & Johnson',12.00,'Tablet strip of 10. Fever and mild pain relief.',0),
 (2,'Amoxicillin 500mg Capsules','Amoxicillin',2,'GlaxoSmithKline',25.00,'Capsule strip of 10. Broad spectrum antibiotic.',1),
 (3,'Ibuprofen 400mg Tablets','Ibuprofen',1,'Abbott',18.00,'Tablet strip of 10. Anti-inflammatory pain relief.',0),
 (4,'Metformin 850mg Tablets','Metformin HCl',5,'USV Pharma',15.00,'Tablet strip of 10. Type 2 diabetes treatment.',1),
 (5,'Amlodipine 5mg Tablets','Amlodipine besylate',4,'Pfizer',22.00,'Tablet strip of 10. For high blood pressure.',1),
 (6,'Cetirizine 10mg Tablets','Cetirizine dihydrochloride',3,'UCB Pharma',8.50,'Tablet strip of 10. Allergy relief.',0),
 (7,'Omeprazole 20mg Capsules','Omeprazole',6,'AstraZeneca',30.00,'Capsule strip of 10. Acid reflux treatment.',1),
 (8,'Vitamin C 1000mg Effervescent','Ascorbic acid',8,'Bayer',45.00,'Tube of 10 effervescent tablets.',0),
 (9,'Salbutamol Inhaler 100mcg','Salbutamol sulfate',7,'GlaxoSmithKline',550.00,'Metered dose inhaler 200 puffs. Asthma relief.',1),
 (10,'ORS Sachet','Oral Rehydration Salts',8,'Ferozsons',35.00,'Sachet 20.9g. Dehydration treatment.',0);

-- Batches (includes EXPIRED and NEAR-EXPIRY batches to exercise alerts) ------
INSERT INTO medicine_batches (id, medicine_id, batch_number, expiry_date, purchase_price, selling_price) VALUES
 (1 ,1 ,'B-PCM-2401', DATE_ADD(CURDATE(), INTERVAL 14 MONTH), 8.00 , 12.00),
 (2 ,1 ,'B-PCM-2402', DATE_ADD(CURDATE(), INTERVAL 3  MONTH), 8.50 , 12.00),  -- near expiry
 (3 ,2 ,'B-AMX-2401', DATE_ADD(CURDATE(), INTERVAL 10 MONTH), 18.00, 25.00),
 (4 ,3 ,'B-IBU-2401', DATE_ADD(CURDATE(), INTERVAL 16 MONTH), 12.00, 18.00),
 (5 ,4 ,'B-MET-2401', DATE_ADD(CURDATE(), INTERVAL 12 MONTH), 10.00, 15.00),
 (6 ,5 ,'B-AML-2401', DATE_ADD(CURDATE(), INTERVAL 18 MONTH), 15.00, 22.00),
 (7 ,6 ,'B-CTZ-2401', DATE_ADD(CURDATE(), INTERVAL 9  MONTH), 5.50,  8.50),
 (8 ,7 ,'B-OMP-2401', DATE_ADD(CURDATE(), INTERVAL 2  MONTH), 22.00, 30.00),  -- near expiry
 (9 ,8 ,'B-VC-2401' , DATE_ADD(CURDATE(), INTERVAL 20 MONTH), 32.00, 45.00),
 (10,9 ,'B-SAL-2401', DATE_ADD(CURDATE(), INTERVAL 24 MONTH), 420.00,550.00),
 (11,10,'B-ORS-2401', DATE_ADD(CURDATE(), INTERVAL 6  MONTH), 25.00, 35.00),
 (12,2 ,'B-AMX-2305', DATE_SUB(CURDATE(), INTERVAL 2  MONTH), 18.00, 25.00),  -- EXPIRED
 (13,6 ,'B-CTZ-2402', DATE_ADD(CURDATE(), INTERVAL 4  MONTH), 5.50,  8.50),
 (14,1 ,'B-PCM-2312', DATE_SUB(CURDATE(), INTERVAL 1  MONTH), 8.00,  12.00);  -- EXPIRED

-- Price history ---------------------------------------------------------------
INSERT INTO price_history (medicine_id, old_price, new_price, changed_by) VALUES
 (1, 10.00, 12.00, 1),
 (2, 23.00, 25.00, 1),
 (8, 40.00, 45.00, 1);

-- Suppliers -------------------------------------------------------------------
INSERT INTO suppliers (id, name, contact_person, phone, email, address) VALUES
 (1,'Hemas Pharmaceuticals (Pvt) Ltd','Saman Kumara','0112345678','sales@hemaspharma.lk','No 47, Lauries Road, Colombo 04'),
 (2,'George Steuart Health (Pvt) Ltd','Priyantha de Silva','0112555900','orders@gshealth.lk','No 311, Galle Road, Colombo 03'),
 (3,'Sisil Pharma Distributors','Malik Rahuman','0117334455','sisil@sisilpharma.lk','No 12, Main Street, Kandy'),
 (4,'Sunrise Medical Supplies','Nadeesha Gunawardena','0118899001','info@sunrisemed.lk','No 89, Negombo Road, Ja-Ela');

-- Purchase orders -------------------------------------------------------------
INSERT INTO purchase_orders (id, po_number, supplier_id, status, order_date, expected_date, total_amount, notes, created_by) VALUES
 (1,'PO-2024-0001',1,'PARTIALLY_RECEIVED', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 9400.00,'Monthly restock', 4),
 (2,'PO-2024-0002',2,'DRAFT',              DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 26000.00,'Inhaler and ORS replenishment', 4);

INSERT INTO purchase_order_items (id, purchase_order_id, medicine_id, quantity, received_quantity, purchase_price, line_total) VALUES
 (1,1,1 ,500,500, 8.00 ,4000.00),
 (2,1,2 ,300,0  ,18.00,5400.00),
 (3,2,9 ,50 ,0  ,420.00,21000.00),
 (4,2,10,200,0  ,25.00 ,5000.00);

-- GRN (verified GRN for PO-0001 – stock for B-PCM-2403 already in inventory) --
INSERT INTO goods_received_notes (id, grn_number, purchase_order_id, supplier_id, received_date, status, verified_by, verified_at, notes, created_by) VALUES
 (1,'GRN-2024-0001',1,1, DATE_SUB(NOW(), INTERVAL 6 DAY),'VERIFIED',1, DATE_SUB(NOW(), INTERVAL 6 DAY),'Paracetamol received in good condition', 3);

INSERT INTO goods_received_note_items (id, grn_id, medicine_id, po_item_id, batch_number, expiry_date, quantity, purchase_price, selling_price) VALUES
 (1,1,1,1,'B-PCM-2403', DATE_ADD(CURDATE(), INTERVAL 13 MONTH), 500, 8.00, 12.00);

-- Final batch (B-PCM-2403 received via GRN-2024-0001) --
INSERT INTO medicine_batches (id, medicine_id, batch_number, expiry_date, purchase_price, selling_price) VALUES
 (15,1,'B-PCM-2403', DATE_ADD(CURDATE(), INTERVAL 13 MONTH), 8.00, 12.00);

-- Inventory (qty = sellable stock per batch; expired batches kept for records) --
INSERT INTO inventory (id, batch_id, quantity_available, reorder_level) VALUES
 (1 ,1 ,480, 50),
 (2 ,2 ,120, 50),
 (3 ,3 ,200, 40),
 (4 ,4 ,350, 50),
 (5 ,5 ,150, 40),
 (6 ,6 ,220, 40),
 (7 ,7 ,500, 60),
 (8 ,8 ,90 , 30),
 (9 ,9 ,180, 30),
 (10,10,60 , 15),
 (11,11,400, 50),
 (12,12,40 , 0),   -- expired – excluded from sales
 (13,13,300, 60),
 (14,14,25 , 0),   -- expired – excluded from sales
 (15,15,500, 60);  -- batch B-PCM-2403 received via GRN-2024-0001

-- Customers ---------------------------------------------------------------------
INSERT INTO customers (id, name, phone, email, address) VALUES
 (1,'Nimal Gunatilleke','0771234567','nimal.g@gmail.com','No 22, Temple Road, Nugegoda'),
 (2,'Kamala Herath','0719876543','kamala.h@yahoo.com','No 5, Lake Drive, Kandy'),
 (3,'Ashen Bandara','0763334455','ashen.b@outlook.com','No 310, Station Road, Dehiwala'),
 (4,'Fathima Rizwan','0752223344','fathima.r@gmail.com','No 77, Beach Road, Mount Lavinia'),
 (5,'Suresh Peiris','0701112233','suresh.p@gmail.com','No 14, Hill Street, Gampaha');

-- Prescriptions -------------------------------------------------------------------
INSERT INTO prescriptions (id, prescription_number, customer_id, doctor_name, prescription_date, medicine_id, notes, created_by) VALUES
 (1,'PRX-2024-0001',1,'Dr. A. Wijewardena', DATE_SUB(CURDATE(), INTERVAL 7 DAY), 2,'Amoxicillin 1 tab TDS for 5 days', 2),
 (2,'PRX-2024-0002',2,'Dr. S. Rajapaksha', DATE_SUB(CURDATE(), INTERVAL 5 DAY), 5,'Amlodipine 1 tab daily', 2);

-- Sales (spread across recent days so dashboard charts render) ----------------------
INSERT INTO sales (id, invoice_number, customer_id, cashier_id, sale_date, subtotal, discount, total, payment_method, status) VALUES
 (1,'INV-2024-0001',1,5, DATE_SUB(NOW(), INTERVAL 7 DAY), 154.00, 0.00, 154.00,'CASH','COMPLETED'),
 (2,'INV-2024-0002',2,5, DATE_SUB(NOW(), INTERVAL 5 DAY), 153.00,10.00, 143.00,'CARD','COMPLETED'),
 (3,'INV-2024-0003',3,5, DATE_SUB(NOW(), INTERVAL 1 DAY), 224.00, 0.00, 224.00,'CASH','COMPLETED'),
 (4,'INV-2024-0004',NULL,5, NOW(), 138.00, 0.00, 138.00,'CASH','COMPLETED');

INSERT INTO sale_items (id, sale_id, medicine_id, batch_id, quantity, unit_price, line_total) VALUES
 (1,1,1 ,1 ,10, 12.00,120.00),
 (2,1,6 ,7 , 4,  8.50, 34.00),
 (3,2,3 ,4 , 6, 18.00,108.00),
 (4,2,8 ,9 , 1, 45.00, 45.00),
 (5,3,4 ,5 , 5, 15.00, 75.00),
 (6,3,5 ,6 , 2, 22.00, 44.00),
 (7,3,10,11, 3, 35.00,105.00),
 (8,4,1 ,1 , 4, 12.00, 48.00),
 (9,4,8 ,9 , 2, 45.00, 90.00);

INSERT INTO payments (id, sale_id, amount, method, paid_at) VALUES
 (1,1,154.00,'CASH', DATE_SUB(NOW(), INTERVAL 7 DAY)),
 (2,2,143.00,'CARD', DATE_SUB(NOW(), INTERVAL 5 DAY)),
 (3,3,224.00,'CASH', DATE_SUB(NOW(), INTERVAL 1 DAY)),
 (4,4,138.00,'CASH', NOW());

-- Stock adjustments -----------------------------------------------------------------
INSERT INTO stock_adjustments (batch_id, adjustment_type, quantity_change, reason, performed_by) VALUES
 (2,'DAMAGE',-5,'5 strips damaged in transit',3),
 (5,'CORRECTION',2,'Physical count correction',3);

-- Audit log seed --------------------------------------------------------------------
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details) VALUES
 (1,'SYSTEM_INIT','DATABASE','mediflow_db','Sample data initialized'),
 (3,'GRN_VERIFIED','GRN','GRN-2024-0001','Stock increased for batch B-PCM-2403'),
 (5,'SALE_COMPLETED','SALE','INV-2024-0004','Walk-in sale, LKR 138.00');

-- Done. Verify:
-- SELECT COUNT(*) FROM medicines;      -- 10
-- SELECT COUNT(*) FROM medicine_batches; -- 15
-- SELECT COUNT(*) FROM inventory;        -- 15
-- SELECT COUNT(*) FROM users;            -- 7
