package com.mediflow.config;

import com.mediflow.entity.*;
import com.mediflow.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds sample operational data ONLY when mediflow.demo-data=true
 * (used for the sandbox/H2 preview where mediflow.sql cannot be loaded).
 * A normal MySQL deployment uses database/mediflow.sql instead.
 */
@Configuration
public class DemoDataInitializer {

    @Bean
    @org.springframework.core.annotation.Order(2)
    CommandLineRunner demoData(MedicineRepository medicineRepository,
                               CategoryRepository categoryRepository,
                               MedicineBatchRepository batchRepository,
                               InventoryRepository inventoryRepository,
                               SupplierRepository supplierRepository,
                               CustomerRepository customerRepository,
                               SaleRepository saleRepository,
                               PurchaseOrderRepository poRepository,
                               org.springframework.core.env.Environment env,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (!"true".equalsIgnoreCase(env.getProperty("mediflow.demo-data"))) return;
            if (medicineRepository.count() > 0) return;

            Role adminRole = getOrCreateRole(roleRepository, Role.ADMIN);
            Role pharmaRole = getOrCreateRole(roleRepository, Role.PHARMACIST);
            Role cashierRole = getOrCreateRole(roleRepository, Role.CASHIER);
            Role procurementRole = getOrCreateRole(roleRepository, Role.PROCUREMENT_OFFICER);
            Role storeRole = getOrCreateRole(roleRepository, Role.STORE_KEEPER);
            Role financeRole = getOrCreateRole(roleRepository, Role.FINANCE_MANAGER);

            User admin = userRepository.findByEmailIgnoreCase("admin@mediflow.com").orElseGet(() -> {
                User u = new User();
                u.setFirstName("System"); u.setLastName("Admin");
                u.setEmail("admin@mediflow.com");
                u.setPasswordHash(passwordEncoder.encode("Admin@123"));
                u.setRole(adminRole); u.setActive(true);
                return userRepository.save(u);
            });
            User pharmacist = mkUser(userRepository, passwordEncoder, pharmaRole, "Nimali", "Perera", "pharmacist@mediflow.com");
            User storekeeper = mkUser(userRepository, passwordEncoder, storeRole, "Kasun", "Silva", "storekeeper@mediflow.com");
            User procurement = mkUser(userRepository, passwordEncoder, procurementRole, "Ruwan", "Fernando", "procurement@mediflow.com");
            User cashier = mkUser(userRepository, passwordEncoder, cashierRole, "Sanduni", "Jayawardena", "cashier@mediflow.com");
            mkUser(userRepository, passwordEncoder, financeRole, "Tharindu", "Rathnayake", "finance@mediflow.com");

            Category[] cats = {
                    mkCategory(categoryRepository, "Analgesics", "Pain relief"),
                    mkCategory(categoryRepository, "Antibiotics", "Infection treatment"),
                    mkCategory(categoryRepository, "Antihistamines", "Allergy relief"),
                    mkCategory(categoryRepository, "Cardiovascular", "Heart and blood pressure"),
                    mkCategory(categoryRepository, "Antidiabetics", "Diabetes"),
                    mkCategory(categoryRepository, "Gastrointestinal", "Digestive"),
                    mkCategory(categoryRepository, "Respiratory", "Asthma"),
                    mkCategory(categoryRepository, "Vitamins & Supplements", "Supplements"),
            };

            Medicine pcm = mkMedicine(medicineRepository, "Paracetamol 500mg Tablets", "Paracetamol", cats[0], "Johnson & Johnson", "12.00", false);
            Medicine amx = mkMedicine(medicineRepository, "Amoxicillin 500mg Capsules", "Amoxicillin", cats[1], "GlaxoSmithKline", "25.00", true);
            Medicine ibu = mkMedicine(medicineRepository, "Ibuprofen 400mg Tablets", "Ibuprofen", cats[0], "Abbott", "18.00", false);
            Medicine met = mkMedicine(medicineRepository, "Metformin 850mg Tablets", "Metformin HCl", cats[4], "USV Pharma", "15.00", true);
            Medicine aml = mkMedicine(medicineRepository, "Amlodipine 5mg Tablets", "Amlodipine besylate", cats[3], "Pfizer", "22.00", true);
            Medicine ctz = mkMedicine(medicineRepository, "Cetirizine 10mg Tablets", "Cetirizine", cats[2], "UCB Pharma", "8.50", false);
            Medicine omp = mkMedicine(medicineRepository, "Omeprazole 20mg Capsules", "Omeprazole", cats[5], "AstraZeneca", "30.00", true);
            Medicine vc = mkMedicine(medicineRepository, "Vitamin C 1000mg Effervescent", "Ascorbic acid", cats[7], "Bayer", "45.00", false);
            Medicine sal = mkMedicine(medicineRepository, "Salbutamol Inhaler 100mcg", "Salbutamol sulfate", cats[6], "GlaxoSmithKline", "550.00", true);
            mkMedicine(medicineRepository, "ORS Sachet", "Oral Rehydration Salts", cats[7], "Ferozsons", "35.00", false);

            LocalDate today = LocalDate.now();
            MedicineBatch b1 = mkBatch(batchRepository, pcm, "B-PCM-2401", today.plusMonths(14), "8.00", "12.00", 480, 50, inventoryRepository);
            MedicineBatch b2 = mkBatch(batchRepository, pcm, "B-PCM-2402", today.plusMonths(3), "8.50", "12.00", 40, 50, inventoryRepository); // near expiry + low
            MedicineBatch b3 = mkBatch(batchRepository, amx, "B-AMX-2401", today.plusMonths(10), "18.00", "25.00", 200, 40, inventoryRepository);
            MedicineBatch b4 = mkBatch(batchRepository, ibu, "B-IBU-2401", today.plusMonths(16), "12.00", "18.00", 350, 50, inventoryRepository);
            MedicineBatch b5 = mkBatch(batchRepository, met, "B-MET-2401", today.plusMonths(12), "10.00", "15.00", 150, 40, inventoryRepository);
            MedicineBatch b6 = mkBatch(batchRepository, aml, "B-AML-2401", today.plusMonths(18), "15.00", "22.00", 220, 40, inventoryRepository);
            MedicineBatch b7 = mkBatch(batchRepository, ctz, "B-CTZ-2401", today.plusMonths(9), "5.50", "8.50", 500, 60, inventoryRepository);
            MedicineBatch b8 = mkBatch(batchRepository, omp, "B-OMP-2401", today.plusMonths(2), "22.00", "30.00", 90, 30, inventoryRepository); // near expiry
            MedicineBatch b9 = mkBatch(batchRepository, vc, "B-VC-2401", today.plusMonths(20), "32.00", "45.00", 180, 30, inventoryRepository);
            MedicineBatch b10 = mkBatch(batchRepository, sal, "B-SAL-2401", today.plusMonths(24), "420.00", "550.00", 8, 15, inventoryRepository); // low stock
            mkBatch(batchRepository, amx, "B-AMX-2305", today.minusMonths(2), "18.00", "25.00", 40, 0, inventoryRepository); // expired
            MedicineBatch b12 = mkBatch(batchRepository, ctz, "B-CTZ-2402", today.plusMonths(4), "5.50", "8.50", 300, 60, inventoryRepository);

            supplierRepository.saveAll(List.of(
                    mkSupplier("Hemas Pharmaceuticals (Pvt) Ltd", "Saman Kumara", "0112345678", "sales@hemaspharma.lk", "No 47, Lauries Road, Colombo 04"),
                    mkSupplier("George Steuart Health (Pvt) Ltd", "Priyantha de Silva", "0112555900", "orders@gshealth.lk", "No 311, Galle Road, Colombo 03"),
                    mkSupplier("Sisil Pharma Distributors", "Malik Rahuman", "0117334455", "sisil@sisilpharma.lk", "No 12, Main Street, Kandy")));

            Customer c1 = customerRepository.save(mkCustomer("Nimal Gunatilleke", "0771234567", "nimal.g@gmail.com", "No 22, Temple Road, Nugegoda"));
            Customer c2 = customerRepository.save(mkCustomer("Kamala Herath", "0719876543", "kamala.h@yahoo.com", "No 5, Lake Drive, Kandy"));
            customerRepository.save(mkCustomer("Ashen Bandara", "0763334455", "ashen.b@outlook.com", "No 310, Station Road, Dehiwala"));
            customerRepository.save(mkCustomer("Fathima Rizwan", "0752223344", "fathima.r@gmail.com", "No 77, Beach Road, Mount Lavinia"));
            customerRepository.save(mkCustomer("Suresh Peiris", "0701112233", "suresh.p@gmail.com", "No 14, Hill Street, Gampaha"));

            // A few historical sales spread over recent days so charts have data
            record Line(MedicineBatch batch, int qty) {}
            List<List<Line>> salesPlan = List.of(
                    List.of(new Line(b1, 10), new Line(b7, 4)),
                    List.of(new Line(b4, 6), new Line(b9, 1)),
                    List.of(new Line(b5, 5), new Line(b6, 2)),
                    List.of(new Line(b12, 3)),
                    List.of(new Line(b1, 4), new Line(b9, 2)));
            long invoiceSeq = saleRepository.count();
            for (int day = salesPlan.size() - 1; day >= 0; day--) {
                Sale sale = new Sale();
                sale.setInvoiceNumber("INV-2026-" + String.format("%04d", ++invoiceSeq));
                sale.setCashier(cashier);
                sale.setCustomer(day % 2 == 0 ? c1 : c2);
                sale.setSaleDate(LocalDateTime.now().minusDays(day).minusHours(3));
                sale.setPaymentMethod(day % 3 == 0 ? "CARD" : "CASH");
                sale.setStatus(Sale.STATUS_COMPLETED);
                BigDecimal subtotal = BigDecimal.ZERO;
                for (Line line : salesPlan.get(day)) {
                    SaleItem si = new SaleItem();
                    si.setMedicine(line.batch().getMedicine());
                    si.setBatch(line.batch());
                    si.setQuantity(line.qty());
                    si.setUnitPrice(line.batch().getSellingPrice());
                    si.setLineTotal(line.batch().getSellingPrice().multiply(BigDecimal.valueOf(line.qty())));
                    sale.addItem(si);
                    subtotal = subtotal.add(si.getLineTotal());
                }
                sale.setSubtotal(subtotal);
                sale.setDiscount(BigDecimal.ZERO);
                sale.setTotal(subtotal);
                Sale saved = saleRepository.save(sale);
                // reflect the sales in stock levels
                for (SaleItem si : saved.getItems()) {
                    inventoryRepository.findByBatchId(si.getBatch().getId()).ifPresent(inv -> {
                        inv.setQuantityAvailable(Math.max(0, inv.getQuantityAvailable() - si.getQuantity()));
                        inventoryRepository.save(inv);
                    });
                }
            }

            PurchaseOrder po = new PurchaseOrder();
            Supplier sup = supplierRepository.findAll().get(0);
            po.setPoNumber("PO-2026-0001");
            po.setSupplier(sup);
            po.setStatus(PurchaseOrder.STATUS_SENT);
            po.setExpectedDate(today.plusDays(5));
            po.setNotes("Monthly restock (demo)");
            po.setCreatedBy(procurement);
            PurchaseOrderItem poi = new PurchaseOrderItem();
            poi.setMedicine(pcm);
            poi.setQuantity(500);
            poi.setReceivedQuantity(0);
            poi.setPurchasePrice(new BigDecimal("8.00"));
            poi.setLineTotal(new BigDecimal("4000.00"));
            po.addItem(poi);
            po.setTotalAmount(po.getGrandTotal());
            poRepository.save(po);
        };
    }

    private Role getOrCreateRole(RoleRepository repo, String name) {
        return repo.findByName(name).orElseGet(() -> repo.save(new Role(name)));
    }

    private User mkUser(UserRepository repo, PasswordEncoder enc, Role role, String f, String l, String email) {
        return repo.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setFirstName(f); u.setLastName(l); u.setEmail(email);
            u.setPasswordHash(enc.encode("Admin@123"));
            u.setRole(role); u.setActive(true);
            return repo.save(u);
        });
    }

    private Category mkCategory(CategoryRepository repo, String name, String desc) {
        Category c = new Category();
        c.setName(name); c.setDescription(desc); c.setActive(true);
        return repo.save(c);
    }

    private Medicine mkMedicine(MedicineRepository repo, String name, String generic, Category cat,
                                String manufacturer, String price, boolean rx) {
        Medicine m = new Medicine();
        m.setName(name); m.setGenericName(generic); m.setCategory(cat);
        m.setManufacturer(manufacturer); m.setUnitPrice(new BigDecimal(price));
        m.setPrescriptionRequired(rx); m.setActive(true);
        return repo.save(m);
    }

    private MedicineBatch mkBatch(MedicineBatchRepository bRepo, Medicine m, String number, LocalDate expiry,
                                  String purchase, String selling, int qty, int reorder, InventoryRepository iRepo) {
        MedicineBatch b = new MedicineBatch();
        b.setMedicine(m); b.setBatchNumber(number); b.setExpiryDate(expiry);
        b.setPurchasePrice(new BigDecimal(purchase)); b.setSellingPrice(new BigDecimal(selling));
        b = bRepo.save(b);
        Inventory inv = new Inventory();
        inv.setBatch(b); inv.setQuantityAvailable(qty); inv.setReorderLevel(reorder);
        iRepo.save(inv);
        return b;
    }

    private Supplier mkSupplier(String name, String person, String phone, String email, String address) {
        Supplier s = new Supplier();
        s.setName(name); s.setContactPerson(person); s.setPhone(phone);
        s.setEmail(email); s.setAddress(address); s.setActive(true);
        return s;
    }

    private Customer mkCustomer(String name, String phone, String email, String address) {
        Customer c = new Customer();
        c.setName(name); c.setPhone(phone); c.setEmail(email); c.setAddress(address);
        return c;
    }
}
