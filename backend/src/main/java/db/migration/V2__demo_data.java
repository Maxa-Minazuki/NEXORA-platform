package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Demo data, consistent across modules (the booking confirmation documents reference real bookings,
 * the replenishment request references a real low-stock product, and so on).
 *
 * <p>A Java migration is used instead of SQL because the data is <em>relative to the installation
 * date</em>: appointments around "today", flights for the next days. Flyway runs it exactly once
 * (recorded in {@code flyway_schema_history}), so restarts never duplicate data. It can be disabled
 * with the placeholder {@code demo_data=false}. All people, airlines, bookings and medical records
 * are fictional.</p>
 */
public class V2__demo_data extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, String> placeholders = context.getConfiguration().getPlaceholders();
        if (!"true".equalsIgnoreCase(placeholders.getOrDefault("demo_data", "true"))) {
            return;
        }
        int flightDays = Integer.parseInt(placeholders.getOrDefault("demo_flight_days", "14"));
        new Seeder(context.getConnection(), Instant.now().truncatedTo(ChronoUnit.SECONDS), flightDays).run();
    }

    static final class Seeder {

        private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Almaty");

        private final Connection c;
        private final Instant now;
        private final int flightDays;
        private final Random random = new Random(20261016L);
        private final Map<String, Long> users = new HashMap<>();
        private final Map<String, Long> products = new HashMap<>();
        private final Map<String, BigDecimal> purchasePrices = new HashMap<>();
        private final Map<String, Integer> suggested = new HashMap<>();
        private final Map<String, Long> flights = new HashMap<>();
        private final Map<Long, BigDecimal> flightPrices = new HashMap<>();
        private final Map<Long, String> flightLabels = new HashMap<>();
        private final List<Long> doctors = new ArrayList<>();
        private final List<Long> patients = new ArrayList<>();
        private Long completedAppointmentOfDoctor0;
        private final int year;

        Seeder(Connection c, Instant now, int flightDays) {
            this.c = c;
            this.now = now;
            this.flightDays = flightDays;
            this.year = now.atZone(ZoneOffset.UTC).getYear();
        }

        void run() throws SQLException {
            seedUsers();
            seedInventory();
            seedClinic();
            seedFlights();
            Map<String, Long> bookings = seedBookings();
            Map<String, Long> docs = seedDocuments(bookings);
            seedNotifications(docs, bookings);
            seedAudit(docs);
        }

        // ------------------------------------------------------------------ users
        private void seedUsers() throws SQLException {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            Object[][] rows = {
                    {"admin", "admin@nexora.local", "Aruzhan Sadykova", "Admin#2026", "ADMIN", "Platform Administrator"},
                    {"manager", "manager@nexora.local", "Daniyar Bekov", "Manager#2026", "MANAGER", "Operations Manager"},
                    {"doctor", "doctor@nexora.local", "Dr. Aigerim Nurlanova", "Doctor#2026", "DOCTOR", "Cardiologist"},
                    {"employee", "employee@nexora.local", "Timur Akhmetov", "Employee#2026", "EMPLOYEE", "Warehouse & Reception Specialist"},
                    {"user", "user@nexora.local", "Madina Serikova", "User#2026", "USER", "Customer"},
            };
            Instant created = now.minus(Duration.ofDays(45));
            for (Object[] r : rows) {
                long id = insert("insert into users (username, email, full_name, password_hash, role, active, title, created_at, updated_at) values (?,?,?,?,?,?,?,?,?)",
                        r[0], r[1], r[2], encoder.encode((String) r[3]), r[4], true, r[5], created, created);
                users.put((String) r[0], id);
                exec("insert into notification_preferences (user_id, channel, enabled, min_severity) values (?,?,?,?)", id, "IN_APP", true, "INFO");
                exec("insert into notification_preferences (user_id, channel, enabled, min_severity) values (?,?,?,?)", id, "EMAIL", true, "WARNING");
            }
        }

        // ------------------------------------------------------------------ inventory
        private void seedInventory() throws SQLException {
            Map<String, Long> categories = new LinkedHashMap<>();
            String[][] cats = {
                    {"Electronics", "Scanners, terminals, printers and IT accessories"},
                    {"Medical Supplies", "Consumables and devices for the clinic"},
                    {"Office Supplies", "Paper, stationery and furniture"},
                    {"Packaging", "Boxes, films and protective materials"},
                    {"Cleaning & Hygiene", "Sanitizers, disinfectants and cleaning tools"},
                    {"Lab Equipment", "Laboratory consumables and demo kits"},
            };
            for (String[] cat : cats) {
                categories.put(cat[0], insert("insert into categories (name, description, created_at) values (?,?,?)",
                        cat[0], cat[1], now.minus(Duration.ofDays(40))));
            }
            Map<String, Long> suppliers = new LinkedHashMap<>();
            String[][] sups = {
                    {"Altyn Supply Co. (demo)", "orders@altyn-supply.example", "+7 700 555 0101"},
                    {"Nomad Medical Trading (demo)", "sales@nomad-medical.example", "+7 700 555 0102"},
                    {"Orbita Electronics (demo)", "b2b@orbita-electronics.example", "+7 700 555 0103"},
                    {"Baiterek Office Goods (demo)", "info@baiterek-office.example", "+7 700 555 0104"},
                    {"Tulpar Packaging (demo)", "hello@tulpar-pack.example", "+7 700 555 0105"},
            };
            for (String[] s : sups) {
                suppliers.put(s[0], insert("insert into suppliers (name, contact_email, phone, created_at) values (?,?,?,?)",
                        s[0], s[1], s[2], now.minus(Duration.ofDays(40))));
            }
            // sku, name, description, category, supplier, purchase, retail, finalQty, min, unit, strategy
            Object[][] items = {
                    {"ELC-1001", "Barcode Scanner Pro X2", "Corded 2D barcode scanner for receiving desks", "Electronics", "Orbita Electronics (demo)", "38500.00", "52900.00", 24, 8, "pcs", "TOP_UP_TO_TARGET"},
                    {"ELC-1002", "Thermal Label Printer", "Direct thermal printer, 4 inch labels", "Electronics", "Orbita Electronics (demo)", "61200.00", "84500.00", 6, 6, "pcs", "TOP_UP_TO_TARGET"},
                    {"ELC-1003", "Wireless Handheld Terminal", "Rugged Android terminal for stock counts", "Electronics", "Orbita Electronics (demo)", "142000.00", "189000.00", 11, 5, "pcs", "DEMAND_BASED"},
                    {"ELC-1004", "USB-C Docking Station", "Dual monitor docking station", "Electronics", "Orbita Electronics (demo)", "22800.00", "31900.00", 40, 10, "pcs", "FIXED_LOT"},
                    {"MED-2001", "Nitrile Examination Gloves (box 100)", "Powder-free, size M", "Medical Supplies", "Nomad Medical Trading (demo)", "2900.00", "4200.00", 18, 40, "box", "DEMAND_BASED"},
                    {"MED-2002", "Sterile Gauze Pads 10x10", "Pack of 25 sterile pads", "Medical Supplies", "Nomad Medical Trading (demo)", "1500.00", "2300.00", 260, 80, "pack", "FIXED_LOT"},
                    {"MED-2003", "Digital Thermometer", "Fast-read clinical thermometer", "Medical Supplies", "Nomad Medical Trading (demo)", "4800.00", "7200.00", 32, 15, "pcs", "TOP_UP_TO_TARGET"},
                    {"MED-2004", "Disposable Syringes 5 ml (box 100)", "Luer lock, sterile", "Medical Supplies", "Nomad Medical Trading (demo)", "5400.00", "7900.00", 0, 20, "box", "FIXED_LOT"},
                    {"MED-2005", "Blood Pressure Monitor", "Upper arm automatic monitor", "Medical Supplies", "Nomad Medical Trading (demo)", "18900.00", "26500.00", 14, 6, "pcs", "TOP_UP_TO_TARGET"},
                    {"OFF-3001", "A4 Copy Paper 80g (ream)", "500 sheets per ream", "Office Supplies", "Baiterek Office Goods (demo)", "2100.00", "2950.00", 340, 100, "ream", "FIXED_LOT"},
                    {"OFF-3002", "Ballpoint Pens Blue (pack 50)", "Medium point, blue ink", "Office Supplies", "Baiterek Office Goods (demo)", "3200.00", "4600.00", 9, 12, "pack", "TOP_UP_TO_TARGET"},
                    {"OFF-3003", "Ergonomic Office Chair", "Mesh back, adjustable lumbar support", "Office Supplies", "Baiterek Office Goods (demo)", "64500.00", "89000.00", 7, 3, "pcs", "TOP_UP_TO_TARGET"},
                    {"OFF-3004", "Document Archive Box", "Cardboard archive box A4", "Office Supplies", "Baiterek Office Goods (demo)", "900.00", "1450.00", 120, 50, "pcs", "FIXED_LOT"},
                    {"PKG-4001", "Corrugated Shipping Box M", "40x30x30 cm, double wall", "Packaging", "Tulpar Packaging (demo)", "380.00", "620.00", 900, 300, "pcs", "FIXED_LOT"},
                    {"PKG-4002", "Stretch Wrap Film 500mm", "23 micron machine film", "Packaging", "Tulpar Packaging (demo)", "4100.00", "5800.00", 22, 10, "roll", "DEMAND_BASED"},
                    {"PKG-4003", "Bubble Wrap Roll 1m x 50m", "Protective bubble film", "Packaging", "Tulpar Packaging (demo)", "6700.00", "9400.00", 4, 8, "roll", "TOP_UP_TO_TARGET"},
                    {"CLN-5001", "Hand Sanitizer 1L", "70% alcohol gel with pump", "Cleaning & Hygiene", "Altyn Supply Co. (demo)", "2400.00", "3600.00", 75, 30, "bottle", "DEMAND_BASED"},
                    {"CLN-5002", "Surface Disinfectant 5L", "Hospital-grade surface disinfectant", "Cleaning & Hygiene", "Altyn Supply Co. (demo)", "7800.00", "10900.00", 12, 10, "can", "TOP_UP_TO_TARGET"},
                    {"CLN-5003", "Microfiber Cloth Pack", "Pack of 10 colour-coded cloths", "Cleaning & Hygiene", "Altyn Supply Co. (demo)", "1900.00", "2800.00", 48, 20, "pack", "FIXED_LOT"},
                    {"LAB-6001", "Centrifuge Tubes 15 ml (pack 500)", "Polypropylene, sterile", "Lab Equipment", "Nomad Medical Trading (demo)", "15600.00", "21800.00", 16, 6, "pack", "TOP_UP_TO_TARGET"},
                    {"LAB-6002", "Pipette Tips 200 ul (rack 96)", "Universal fit, racked", "Lab Equipment", "Nomad Medical Trading (demo)", "3300.00", "4700.00", 70, 25, "rack", "FIXED_LOT"},
                    {"LAB-SENSOR-01", "Demo Lab Sensor Kit", "Reserved for the Design Patterns Lab scenarios", "Lab Equipment", "Altyn Supply Co. (demo)", "12000.00", "16500.00", 30, 10, "pcs", "TOP_UP_TO_TARGET"},
            };
            Instant opening = now.minus(Duration.ofDays(30));
            Long employee = users.get("employee");
            Long manager = users.get("manager");
            for (Object[] it : items) {
                String sku = (String) it[0];
                int finalQty = (Integer) it[7];
                int min = (Integer) it[8];
                boolean critical = finalQty <= min;
                boolean lab = sku.startsWith("LAB-SENSOR");
                // Movements of the last 13 days: write-offs for everyone, receipts only for healthy products.
                List<int[]> movements = new ArrayList<>(); // {daysAgo, delta, hour}
                if (!lab) {
                    for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
                        if (random.nextInt(100) < 55) {
                            int out = 1 + random.nextInt(Math.max(min / 3, 2));
                            movements.add(new int[]{daysAgo, -out, 9 + random.nextInt(8)});
                        }
                    }
                    if (!critical) {
                        int receipts = 1 + random.nextInt(2);
                        for (int i = 0; i < receipts; i++) {
                            int in = Math.min(min + random.nextInt(Math.max(min, 1) + 1), finalQty / receipts);
                            if (in > 0) {
                                movements.add(new int[]{1 + random.nextInt(12), in, 10 + random.nextInt(6)});
                            }
                        }
                    }
                }
                movements.sort((a, b) -> a[0] != b[0] ? Integer.compare(b[0], a[0]) : Integer.compare(a[2], b[2]));
                int outs = movements.stream().filter(m -> m[1] < 0).mapToInt(m -> -m[1]).sum();
                int ins = movements.stream().filter(m -> m[1] > 0).mapToInt(m -> m[1]).sum();
                int initial = finalQty + outs - ins;

                long productId = insert("insert into products (sku, name, description, category_id, supplier_id, purchase_price, retail_price, quantity, min_stock, unit, replenishment_strategy, active, version, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        sku, it[1], it[2], categories.get((String) it[3]), suppliers.get((String) it[4]),
                        new BigDecimal((String) it[5]), new BigDecimal((String) it[6]), finalQty, min, it[9], it[10],
                        true, 0, opening, now);
                products.put(sku, productId);
                purchasePrices.put(sku, new BigDecimal((String) it[5]));
                exec("insert into inventory_transactions (product_id, movement_type, quantity_change, quantity_before, quantity_after, reason, performed_by, created_at) values (?,?,?,?,?,?,?,?)",
                        productId, "INITIAL", initial, 0, initial, "Opening balance", manager, opening);
                int running = initial;
                for (int[] m : movements) {
                    int before = running;
                    running += m[1];
                    Instant at = LocalDate.ofInstant(now, BUSINESS_ZONE).minusDays(m[0]).atTime(m[2], random.nextInt(60))
                            .atZone(BUSINESS_ZONE).toInstant();
                    if (at.isAfter(now)) {
                        at = now.minus(Duration.ofMinutes(30 + random.nextInt(120)));
                    }
                    String type = m[1] > 0 ? "RECEIPT" : "WRITE_OFF";
                    String reason = m[1] > 0 ? "Supplier delivery" : (random.nextBoolean() ? "Issued to clinic" : "Issued to warehouse operations");
                    exec("insert into inventory_transactions (product_id, movement_type, quantity_change, quantity_before, quantity_after, reason, performed_by, created_at) values (?,?,?,?,?,?,?,?)",
                            productId, type, m[1], before, running, reason, employee, at);
                }
                if (critical) {
                    int suggestion = switch ((String) it[10]) {
                        case "TOP_UP_TO_TARGET" -> Math.max(min * 3 - finalQty, 1);
                        case "FIXED_LOT" -> ((Math.max(min * 2 - finalQty, 1) + 49) / 50) * 50;
                        default -> Math.max((int) Math.ceil(outs / 30.0 * 21) + min - finalQty, 1);
                    };
                    suggested.put(sku, suggestion);
                }
            }
            for (Map.Entry<String, Integer> e : suggested.entrySet()) {
                Long productId = products.get(e.getKey());
                exec("insert into stock_alerts (product_id, quantity_at_alert, threshold, suggested_quantity, strategy, status, document_id, created_at) values (?,?,?,?,?,?,?,?)",
                        productId, queryInt("select quantity from products where id = ?", productId),
                        queryInt("select min_stock from products where id = ?", productId), e.getValue(),
                        queryString("select replenishment_strategy from products where id = ?", productId), "OPEN", null,
                        now.minus(Duration.ofHours(6 + random.nextInt(60))));
            }
        }

        // ------------------------------------------------------------------ clinic
        private void seedClinic() throws SQLException {
            String[][] specialties = {
                    {"Cardiology", "Heart and blood vessel conditions"},
                    {"General Practice", "Primary care and referrals"},
                    {"Pediatrics", "Care for children and adolescents"},
                    {"Neurology", "Brain, spine and nerve disorders"},
                    {"Dermatology", "Skin, hair and nail conditions"},
                    {"Ophthalmology", "Eye health and vision"},
            };
            Map<String, Long> spec = new HashMap<>();
            for (String[] s : specialties) {
                spec.put(s[0], insert("insert into specialties (name, description) values (?,?)", s[0], s[1]));
            }
            Object[][] docs = {
                    {"Dr. Aigerim Nurlanova", "Cardiology", "204", users.get("doctor"), "Cardiologist with a focus on preventive care."},
                    {"Dr. Nurlan Abenov", "General Practice", "101", null, "Family physician, also on Saturday mornings."},
                    {"Dr. Saule Karimova", "Pediatrics", "112", null, "Pediatrician for children from 0 to 17 years."},
                    {"Dr. Arman Tulegenov", "Neurology", "305", null, "Neurologist, headaches and sleep disorders."},
                    {"Dr. Dana Utepova", "Dermatology", "210", null, "Dermatologist, Monday to Thursday."},
                    {"Dr. Yerlan Mukhamedov", "Ophthalmology", "118", null, "Ophthalmologist, vision checks and follow-ups."},
            };
            for (int i = 0; i < docs.length; i++) {
                Object[] d = docs[i];
                String email = ((String) d[0]).replace("Dr. ", "").toLowerCase().replace(' ', '.') + "@clinic.example";
                long id = insert("insert into doctors (user_id, full_name, specialty_id, email, phone, room, bio, active, created_at) values (?,?,?,?,?,?,?,?,?)",
                        d[3], d[0], spec.get((String) d[1]), email, "+7 700 555 02" + String.format("%02d", i + 1), d[2], d[4], true,
                        now.minus(Duration.ofDays(40)));
                doctors.add(id);
                int lastWeekday = i == 4 ? 4 : 5;
                for (int dow = 1; dow <= lastWeekday; dow++) {
                    exec("insert into doctor_schedules (doctor_id, day_of_week, start_time, end_time) values (?,?,?,?)",
                            id, dow, LocalTime.of(9, 0), LocalTime.of(17, 0));
                }
                if (i == 1) {
                    exec("insert into doctor_schedules (doctor_id, day_of_week, start_time, end_time) values (?,?,?,?)",
                            id, 6, LocalTime.of(10, 0), LocalTime.of(14, 0));
                }
            }
            Object[][] people = {
                    {"Madina", "Serikova", LocalDate.of(1994, 3, 12), "FEMALE", users.get("user")},
                    {"Askar", "Zhumabayev", LocalDate.of(1988, 7, 21), "MALE", null},
                    {"Laura", "Ismailova", LocalDate.of(1979, 11, 2), "FEMALE", null},
                    {"Bekzat", "Ospanov", LocalDate.of(2001, 5, 30), "MALE", null},
                    {"Gulnara", "Tokayeva", LocalDate.of(1965, 1, 18), "FEMALE", null},
                    {"Ruslan", "Kim", LocalDate.of(1990, 9, 9), "MALE", null},
                    {"Aliya", "Nurpeisova", LocalDate.of(2015, 4, 25), "FEMALE", null},
                    {"Ivan", "Petrenko", LocalDate.of(1972, 12, 14), "MALE", null},
                    {"Zarina", "Abdullayeva", LocalDate.of(1998, 6, 3), "FEMALE", null},
                    {"Erik", "Sultanov", LocalDate.of(1984, 2, 27), "MALE", null},
                    {"Kamila", "Rakhimova", LocalDate.of(2010, 8, 19), "FEMALE", null},
                    {"Marat", "Yesenov", LocalDate.of(1959, 10, 5), "MALE", null},
                    {"Aigul", "Baimukhanova", LocalDate.of(1993, 3, 30), "FEMALE", null},
                    {"Timur", "Seitkali", LocalDate.of(2005, 12, 1), "MALE", null},
            };
            for (int i = 0; i < people.length; i++) {
                Object[] p = people[i];
                patients.add(insert("insert into patients (user_id, first_name, last_name, date_of_birth, gender, phone, email, notes, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?)",
                        p[4], p[0], p[1], p[2], p[3], "+7 700 555 03" + String.format("%02d", i + 1),
                        ((String) p[0]).toLowerCase() + "." + ((String) p[1]).toLowerCase() + "@example.com",
                        "Fictional demo patient - not a real person.", now.minus(Duration.ofDays(35 - i)), now.minus(Duration.ofDays(35 - i))));
            }
            String[] reasons = {"Routine check-up", "Follow-up visit", "Persistent headache", "Skin rash consultation",
                    "Blood pressure review", "Vision test", "Vaccination consultation", "Lab results discussion",
                    "Annual physical", "Sleep problems"};
            LocalDate today = LocalDate.ofInstant(now, BUSINESS_ZONE);
            Long employee = users.get("employee");
            for (int offset = -7; offset <= 7; offset++) {
                LocalDate day = today.plusDays(offset);
                for (int d = 0; d < doctors.size(); d++) {
                    LocalTime start = workStart(d, day.getDayOfWeek());
                    if (start == null) {
                        continue;
                    }
                    int slots = (int) (Duration.between(start, workEnd(d, day.getDayOfWeek())).toMinutes() / 30);
                    int perDay = d <= 1 ? 3 : 2;
                    for (int k = 0; k < perDay; k++) {
                        int slot = d + k * 6;
                        if (slot >= Math.min(slots, 14) || random.nextInt(100) < 25) {
                            continue;
                        }
                        Instant s = day.atTime(start.plusMinutes(30L * slot)).atZone(BUSINESS_ZONE).toInstant();
                        int patientIdx = 1 + Math.floorMod(d * 5 + k * 3 + offset * 2, patients.size() - 1);
                        String status = statusFor(s);
                        Long apptId = insertAppointment(patients.get(patientIdx), doctors.get(d), s, status,
                                reasons[random.nextInt(reasons.length)], employee);
                        if (d == 0 && "COMPLETED".equals(status) && completedAppointmentOfDoctor0 == null) {
                            completedAppointmentOfDoctor0 = apptId;
                        }
                    }
                }
            }
            // Madina (USER account): one completed visit and one upcoming appointment, in reserved slots 14/15.
            LocalDate past = previousWorkingDay(today.minusDays(3), 1);
            insertAppointment(patients.getFirst(), doctors.get(1), past.atTime(16, 0).atZone(BUSINESS_ZONE).toInstant(),
                    "COMPLETED", "Annual physical", users.get("user"));
            LocalDate next = nextWorkingDay(today.plusDays(2), 0);
            insertAppointment(patients.getFirst(), doctors.getFirst(), next.atTime(16, 30).atZone(BUSINESS_ZONE).toInstant(),
                    "CONFIRMED", "Blood pressure review", users.get("user"));
        }

        private Long insertAppointment(Long patient, Long doctor, Instant start, String status, String reason, Long createdBy)
                throws SQLException {
            Instant created = start.isBefore(now) ? start.minus(Duration.ofDays(3)) : now.minus(Duration.ofDays(1 + random.nextInt(3)));
            return insert("insert into appointments (patient_id, doctor_id, start_time, end_time, status, reason, notes, cancel_reason, created_by, version, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?)",
                    patient, doctor, start, start.plus(Duration.ofMinutes(30)), status, reason,
                    "COMPLETED".equals(status) ? "Seen by doctor. Demo note." : null,
                    "CANCELLED".equals(status) ? "Patient asked to cancel" : null, createdBy, 0, created, created);
        }

        private String statusFor(Instant start) {
            int roll = random.nextInt(100);
            if (start.isBefore(now)) {
                return roll < 75 ? "COMPLETED" : roll < 88 ? "NO_SHOW" : "CANCELLED";
            }
            return roll < 50 ? "SCHEDULED" : roll < 90 ? "CONFIRMED" : "CANCELLED";
        }

        private static LocalTime workStart(int doctor, DayOfWeek dow) {
            if (dow == DayOfWeek.SATURDAY) {
                return doctor == 1 ? LocalTime.of(10, 0) : null;
            }
            if (dow == DayOfWeek.SUNDAY || (doctor == 4 && dow == DayOfWeek.FRIDAY)) {
                return null;
            }
            return LocalTime.of(9, 0);
        }

        private static LocalTime workEnd(int doctor, DayOfWeek dow) {
            return dow == DayOfWeek.SATURDAY ? LocalTime.of(14, 0) : LocalTime.of(17, 0);
        }

        private static LocalDate nextWorkingDay(LocalDate from, int doctor) {
            LocalDate d = from;
            while (workStart(doctor, d.getDayOfWeek()) == null || d.getDayOfWeek() == DayOfWeek.SATURDAY) {
                d = d.plusDays(1);
            }
            return d;
        }

        private static LocalDate previousWorkingDay(LocalDate from, int doctor) {
            LocalDate d = from;
            while (workStart(doctor, d.getDayOfWeek()) == null || d.getDayOfWeek() == DayOfWeek.SATURDAY) {
                d = d.minusDays(1);
            }
            return d;
        }

        // ------------------------------------------------------------------ flights
        private void seedFlights() throws SQLException {
            Object[][] airports = {
                    {"ALA", "Almaty International Airport", "Almaty", "Kazakhstan", "Asia/Almaty"},
                    {"NQZ", "Astana Nursultan Nazarbayev International", "Astana", "Kazakhstan", "Asia/Almaty"},
                    {"CIT", "Shymkent International Airport", "Shymkent", "Kazakhstan", "Asia/Almaty"},
                    {"AKX", "Aktobe International Airport", "Aktobe", "Kazakhstan", "Asia/Aqtobe"},
                    {"TAS", "Tashkent International Airport", "Tashkent", "Uzbekistan", "Asia/Tashkent"},
                    {"IST", "Istanbul Airport", "Istanbul", "Turkiye", "Europe/Istanbul"},
                    {"DXB", "Dubai International Airport", "Dubai", "United Arab Emirates", "Asia/Dubai"},
            };
            Map<String, Long> ap = new HashMap<>();
            Map<String, ZoneId> zones = new HashMap<>();
            for (Object[] a : airports) {
                ap.put((String) a[0], insert("insert into airports (code, name, city, country, timezone) values (?,?,?,?,?)",
                        a[0], a[1], a[2], a[3], a[4]));
                zones.put((String) a[0], ZoneId.of((String) a[4]));
            }
            // Fictional airlines. number, airline, from, to, local departure, minutes, base price (KZT)
            Object[][] routes = {
                    {"SN101", "SkyNomad Air", "ALA", "NQZ", LocalTime.of(7, 10), 105, "32500.00"},
                    {"SW205", "Steppe Wings", "ALA", "NQZ", LocalTime.of(12, 40), 95, "41800.00"},
                    {"AJ310", "Aurora Jet", "ALA", "NQZ", LocalTime.of(19, 25), 120, "27900.00"},
                    {"SN102", "SkyNomad Air", "NQZ", "ALA", LocalTime.of(9, 30), 110, "33900.00"},
                    {"SW206", "Steppe Wings", "NQZ", "ALA", LocalTime.of(17, 50), 100, "38700.00"},
                    {"SN120", "SkyNomad Air", "ALA", "CIT", LocalTime.of(8, 15), 75, "21500.00"},
                    {"AJ330", "Aurora Jet", "CIT", "ALA", LocalTime.of(14, 5), 80, "19900.00"},
                    {"SR501", "Silk Route Airways", "ALA", "IST", LocalTime.of(6, 45), 400, "189000.00"},
                    {"AJ510", "Aurora Jet", "ALA", "IST", LocalTime.of(23, 30), 430, "156000.00"},
                    {"SR502", "Silk Route Airways", "IST", "ALA", LocalTime.of(15, 20), 380, "194000.00"},
                    {"SW610", "Steppe Wings", "ALA", "DXB", LocalTime.of(10, 20), 300, "142000.00"},
                    {"SR620", "Silk Route Airways", "ALA", "DXB", LocalTime.of(2, 10), 290, "167500.00"},
                    {"SW611", "Steppe Wings", "DXB", "ALA", LocalTime.of(13, 45), 275, "149000.00"},
                    {"SN140", "SkyNomad Air", "ALA", "TAS", LocalTime.of(11, 0), 70, "45900.00"},
                    {"SN150", "SkyNomad Air", "NQZ", "AKX", LocalTime.of(13, 20), 125, "36400.00"},
            };
            int rows = 10;
            int perRow = 6;
            for (Object[] r : routes) {
                String number = (String) r[0];
                long routeId = insert("insert into flight_routes (flight_number, airline, origin_id, destination_id, departure_local, duration_minutes, base_price, seat_rows, seats_per_row, active) values (?,?,?,?,?,?,?,?,?,?)",
                        number, r[1], ap.get((String) r[2]), ap.get((String) r[3]), r[4], r[5], new BigDecimal((String) r[6]), rows, perRow, true);
                ZoneId zone = zones.get((String) r[2]);
                LocalDate originToday = LocalDate.ofInstant(now, zone);
                for (int d = 0; d <= flightDays; d++) {
                    Instant departure = originToday.plusDays(d).atTime((LocalTime) r[4]).atZone(zone).toInstant();
                    if (departure.isBefore(now)) {
                        continue;
                    }
                    long flightId = insert("insert into flights (route_id, flight_number, airline, origin_id, destination_id, departure_time, arrival_time, base_price, total_seats, available_seats, status, version) values (?,?,?,?,?,?,?,?,?,?,?,?)",
                            routeId, number, r[1], ap.get((String) r[2]), ap.get((String) r[3]), departure,
                            departure.plus(Duration.ofMinutes((Integer) r[5])), new BigDecimal((String) r[6]), rows * perRow,
                            rows * perRow, "SCHEDULED", 0);
                    flights.put(number + "#" + d, flightId);
                    flightPrices.put(flightId, new BigDecimal((String) r[6]));
                    flightLabels.put(flightId, number + " " + r[2] + " -> " + r[3]);
                    try (PreparedStatement ps = c.prepareStatement(
                            "insert into flight_seats (flight_id, seat_number, status, booking_id, version) values (?,?,?,?,?)")) {
                        for (int row = 1; row <= rows; row++) {
                            for (int s = 0; s < perRow; s++) {
                                ps.setLong(1, flightId);
                                ps.setString(2, row + String.valueOf((char) ('A' + s)));
                                ps.setString(3, "AVAILABLE");
                                ps.setObject(4, null);
                                ps.setLong(5, 0);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                }
            }
        }

        private Long flight(String number, int day) {
            Long id = flights.get(number + "#" + day);
            if (id == null) {
                id = flights.get(number + "#" + (day + 1));
            }
            return id;
        }

        private Map<String, Long> seedBookings() throws SQLException {
            Map<String, Long> result = new LinkedHashMap<>();
            // reference, user, flight, day, status, passengers {name, seat}
            Object[][] bookings = {
                    {"NXR7K2", "user", "SN101", 3, "CONFIRMED", new String[][]{{"Madina Serikova", "3A"}, {"Arman Serikov", "3B"}}},
                    {"NXR4P9", "manager", "SW610", 5, "CONFIRMED", new String[][]{{"Daniyar Bekov", "5C"}}},
                    {"NXR2D6", "employee", "SN120", 2, "CONFIRMED", new String[][]{{"Timur Akhmetov", "7F"}}},
                    {"NXR8M3", "user", "SR501", 6, "CANCELLED", new String[][]{{"Madina Serikova", "12A"}}},
                    {"NXR5T8", "user", "AJ310", 1, "EXPIRED", new String[][]{{"Madina Serikova", "9D"}}},
            };
            for (Object[] b : bookings) {
                Long flightId = flight((String) b[2], (Integer) b[3]);
                if (flightId == null) {
                    continue; // horizon too short (tests)
                }
                String[][] pax = (String[][]) b[5];
                String status = (String) b[4];
                Instant created = now.minus(Duration.ofHours(20 + random.nextInt(40)));
                BigDecimal total = flightPrices.get(flightId).multiply(BigDecimal.valueOf(pax.length));
                long id = insert("insert into flight_bookings (reference, flight_id, user_id, passenger_count, total_price, contact_email, status, expires_at, confirmed_at, cancelled_at, cancel_reason, version, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        b[0], flightId, users.get((String) b[1]), pax.length, total, b[1] + "@nexora.local", status,
                        created.plus(Duration.ofMinutes(15)),
                        "CONFIRMED".equals(status) ? created.plus(Duration.ofMinutes(4)) : null,
                        "CONFIRMED".equals(status) ? null : created.plus(Duration.ofMinutes("EXPIRED".equals(status) ? 15 : 7)),
                        "CANCELLED".equals(status) ? "Change of plans" : "EXPIRED".equals(status) ? "Seat hold expired before confirmation" : null,
                        0, created, created);
                result.put((String) b[0], id);
                for (String[] p : pax) {
                    exec("insert into booking_passengers (booking_id, full_name, seat_number) values (?,?,?)", id, p[0], p[1]);
                    if ("CONFIRMED".equals(status)) {
                        exec("update flight_seats set status = 'BOOKED', booking_id = ? where flight_id = ? and seat_number = ?", id, flightId, p[1]);
                    }
                }
                if ("CONFIRMED".equals(status)) {
                    exec("update flights set available_seats = available_seats - ? where id = ?", pax.length, flightId);
                }
                flightLabels.put(-id, flightLabels.get(flightId));
            }
            return result;
        }

        // ------------------------------------------------------------------ workflow
        private Map<String, Long> seedDocuments(Map<String, Long> bookings) throws SQLException {
            Map<String, Long> docs = new LinkedHashMap<>();
            Long admin = users.get("admin");
            Long manager = users.get("manager");
            Long employee = users.get("employee");
            Long doctor = users.get("doctor");

            // D1 replenishment request for the gloves alert, waiting for the manager
            int gloves = suggested.getOrDefault("MED-2001", 60);
            BigDecimal glovesAmount = purchasePrices.get("MED-2001").multiply(BigDecimal.valueOf(gloves));
            long d1 = document("INVENTORY_REPLENISHMENT", "Replenish Nitrile Examination Gloves (box 100)",
                    "Replenishment request generated from a stock alert.\nProduct: Nitrile Examination Gloves (box 100) (MED-2001)\n"
                            + "Suggested order: " + gloves + " box (strategy DEMAND_BASED)\nUnit purchase price: 2900.00",
                    "PENDING_APPROVAL", employee, glovesAmount, "PRODUCT", products.get("MED-2001"), hoursAgo(30), hoursAgo(5), null, null);
            step(d1, 1, "Manager Review", "MANAGER", "ACTIVE", null, null, null);
            actionsFor(d1, employee, hoursAgo(30), hoursAgo(5), "ROUTE: 1 approval step(s), first: Manager Review");
            exec("update stock_alerts set status = 'REQUEST_CREATED', document_id = ? where product_id = ?", d1, products.get("MED-2001"));
            docs.put("replenishment", d1);

            // D2 large purchase request: manager approved, final approval by admin pending
            long d2 = document("PURCHASE_REQUEST", "Purchase of 16 wireless handheld terminals",
                    "Request to purchase 16 Wireless Handheld Terminals (ELC-1003) for the Q4 stock count project.\nSupplier: Orbita Electronics (demo)\nUnit price 142000.00 KZT.",
                    "IN_REVIEW", employee, new BigDecimal("2272000.00"), null, null, hoursAgo(72), hoursAgo(50), null, null);
            step(d2, 1, "Manager Review", "MANAGER", "APPROVED", manager, hoursAgo(26), "Budget available in Q4");
            step(d2, 2, "Final Approval", "ADMIN", "ACTIVE", null, null, null);
            actionsFor(d2, employee, hoursAgo(72), hoursAgo(50), "ROUTE: 2 approval step(s), first: Manager Review");
            action(d2, "APPROVE", manager, "PENDING_APPROVAL", "IN_REVIEW", "Manager Review: Budget available in Q4", hoursAgo(26));
            docs.put("purchaseLarge", d2);

            // D3 small purchase request, approved
            long d3 = document("PURCHASE_REQUEST", "Office paper restock for Q4",
                    "90 reams of A4 copy paper (OFF-3001) for all departments until the end of the year.",
                    "APPROVED", employee, new BigDecimal("189000.00"), null, null, hoursAgo(140), hoursAgo(130), hoursAgo(100), null);
            step(d3, 1, "Manager Review", "MANAGER", "APPROVED", manager, hoursAgo(100), "Approved, standard consumables");
            actionsFor(d3, employee, hoursAgo(140), hoursAgo(130), "ROUTE: 1 approval step(s), first: Manager Review");
            action(d3, "APPROVE", manager, "PENDING_APPROVAL", "APPROVED", "Manager Review: Approved, standard consumables", hoursAgo(100));
            docs.put("purchaseSmall", d3);

            // D4 doctor's draft
            long d4 = document("GENERAL_ADMINISTRATIVE", "Request: cardiology room equipment check",
                    "Please schedule the yearly calibration of the ECG device and the blood pressure monitors in room 204.",
                    "DRAFT", doctor, null, null, null, hoursAgo(8), null, null, null);
            action(d4, "CREATE", doctor, null, "DRAFT", "General Administrative Document", hoursAgo(8));
            revision(d4, doctor, hoursAgo(8));
            docs.put("draft", d4);

            // D5 rejected medical report (references a real completed appointment)
            if (completedAppointmentOfDoctor0 == null) {
                Object any = query("select min(id) from appointments where status = ?", "COMPLETED");
                completedAppointmentOfDoctor0 = any == null ? null : ((Number) any).longValue();
            }
            long d5 = document("MEDICAL_APPOINTMENT_REPORT", "Visit report: follow-up cardiology consultation",
                    "Summary of a completed follow-up visit (fictional demo record). Blood pressure stable, continue current plan.",
                    "REJECTED", doctor, null, "APPOINTMENT", completedAppointmentOfDoctor0, hoursAgo(90), hoursAgo(88), hoursAgo(60),
                    "Please attach the follow-up plan and file a new report.");
            step(d5, 1, "Manager Review", "MANAGER", "REJECTED", manager, hoursAgo(60), "Please attach the follow-up plan and file a new report.");
            step(d5, 2, "Final Approval", "ADMIN", "SKIPPED", null, null, null);
            actionsFor(d5, doctor, hoursAgo(90), hoursAgo(88), "ROUTE: 2 approval step(s), first: Manager Review");
            action(d5, "REJECT", manager, "PENDING_APPROVAL", "REJECTED", "Manager Review: Please attach the follow-up plan and file a new report.", hoursAgo(60));
            docs.put("rejected", d5);

            // D6 manager's own document: another approver (admin) must decide
            long d6 = document("GENERAL_ADMINISTRATIVE", "Updated shift rota for November",
                    "New warehouse and reception shift rota for November, including weekend coverage for the clinic front desk.",
                    "PENDING_APPROVAL", manager, null, null, null, hoursAgo(20), hoursAgo(19), null, null);
            step(d6, 1, "Manager Review", "MANAGER", "ACTIVE", null, null, null);
            actionsFor(d6, manager, hoursAgo(20), hoursAgo(19), "ROUTE: 1 approval step(s), first: Manager Review");
            docs.put("rota", d6);

            // Confirmation documents of the confirmed bookings (system issued, auto-approved)
            for (Map.Entry<String, Long> b : bookings.entrySet()) {
                String status = queryString("select status from flight_bookings where id = ?", b.getValue());
                if (!"CONFIRMED".equals(status)) {
                    continue;
                }
                Long owner = queryLong("select user_id from flight_bookings where id = ?", b.getValue());
                Instant confirmed = queryInstant("select confirmed_at from flight_bookings where id = ?", b.getValue());
                String content = "EDUCATIONAL DEMO - NOT A REAL TICKET. No airline reservation or payment was made.\n"
                        + "Booking reference: " + b.getKey() + "\nFlight: " + flightLabels.get(-b.getValue());
                long doc = document("FLIGHT_BOOKING_CONFIRMATION", "Flight booking confirmation " + b.getKey(), content,
                        "APPROVED", owner, null, "FLIGHT_BOOKING", b.getValue(), confirmed, confirmed, confirmed, null);
                exec("update documents set approval_round = 1 where id = ?", doc);
                action(doc, "CREATE", owner, null, "DRAFT", "Flight Booking Confirmation", confirmed);
                action(doc, "SUBMIT", null, "DRAFT", "APPROVED", "AUTO_APPROVE: system document, no approval required", confirmed);
                revision(doc, owner, confirmed);
                exec("insert into booking_documents (booking_id, document_number, document_id, content, issued_at) values (?,?,?,?,?)",
                        b.getValue(), "CNF-" + b.getKey(), doc, content, confirmed);
                docs.put("confirmation-" + b.getKey(), doc);
            }
            return docs;
        }

        private long document(String type, String title, String content, String status, Long author, BigDecimal amount,
                              String relatedType, Long relatedId, Instant created, Instant submitted, Instant completed,
                              String rejection) throws SQLException {
            int round = submitted == null ? 0 : 1;
            long id = insert("insert into documents (number, title, doc_type, status, author_id, amount, content, related_entity_type, related_entity_id, approval_round, revision, submitted_at, completed_at, rejection_reason, version, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    null, title, type, status, author, amount, content, relatedType, relatedId, round, 1, submitted, completed,
                    rejection, 0, created, completed != null ? completed : submitted != null ? submitted : created);
            exec("update documents set number = ? where id = ?", "DOC-" + year + "-" + String.format("%05d", id), id);
            return id;
        }

        private void step(long doc, int order, String name, String role, String status, Long decidedBy, Instant at,
                          String comment) throws SQLException {
            exec("insert into approval_steps (document_id, approval_round, step_order, name, required_role, status, decided_by, decided_at, decision_comment) values (?,?,?,?,?,?,?,?,?)",
                    doc, 1, order, name, role, status, decidedBy, at, comment);
        }

        private void actionsFor(long doc, Long author, Instant created, Instant submitted, String decision) throws SQLException {
            action(doc, "CREATE", author, null, "DRAFT", null, created);
            revision(doc, author, created);
            action(doc, "SUBMIT", author, "DRAFT", "PENDING_APPROVAL", decision, submitted);
        }

        private void action(long doc, String action, Long actor, String from, String to, String note, Instant at) throws SQLException {
            exec("insert into document_actions (document_id, action, actor_id, from_status, to_status, note, created_at) values (?,?,?,?,?,?,?)",
                    doc, action, actor, from, to, note, at);
        }

        private void revision(long doc, Long author, Instant at) throws SQLException {
            exec("insert into document_revisions (document_id, revision, title, content, amount, changed_by, created_at) select id, revision, title, content, amount, ?, ? from documents where id = ?",
                    author, at, doc);
        }

        // ------------------------------------------------------------------ notifications & audit
        private void seedNotifications(Map<String, Long> docs, Map<String, Long> bookings) throws SQLException {
            String n1 = number(docs.get("replenishment"));
            String n2 = number(docs.get("purchaseLarge"));
            String n3 = number(docs.get("purchaseSmall"));
            String n5 = number(docs.get("rejected"));
            String n6 = number(docs.get("rota"));
            Object[][] rows = {
                    {"employee", "INVENTORY", "CRITICAL", "Out of stock: Disposable Syringes 5 ml (box 100)", "Disposable Syringes 5 ml (box 100) (MED-2004) is at 0 units, critical level 20.", "/inventory?tab=alerts", 50, false},
                    {"employee", "INVENTORY", "WARNING", "Low stock: Bubble Wrap Roll 1m x 50m", "Bubble Wrap Roll 1m x 50m (PKG-4003) is at 4 units, critical level 8.", "/inventory?tab=alerts", 70, false},
                    {"employee", "WORKFLOW", "INFO", "Submitted: " + n1, "\"Replenish Nitrile Examination Gloves (box 100)\" passed validation and was sent to Manager Review.", "/documents/" + docs.get("replenishment"), 5, false},
                    {"employee", "WORKFLOW", "INFO", "Step approved: " + n2, "\"Purchase of 16 wireless handheld terminals\" moved on to Final Approval.", "/documents/" + docs.get("purchaseLarge"), 26, true},
                    {"employee", "WORKFLOW", "SUCCESS", "Approved: " + n3, "\"Office paper restock for Q4\" completed its approval route.", "/documents/" + docs.get("purchaseSmall"), 100, true},
                    {"manager", "WORKFLOW", "INFO", "Approval needed: " + n1, "\"Replenish Nitrile Examination Gloves (box 100)\" is waiting for Manager Review.", "/documents/" + docs.get("replenishment"), 5, false},
                    {"manager", "INVENTORY", "CRITICAL", "Out of stock: Disposable Syringes 5 ml (box 100)", "Disposable Syringes 5 ml (box 100) (MED-2004) is at 0 units, critical level 20.", "/inventory?tab=alerts", 50, false},
                    {"manager", "INVENTORY", "WARNING", "Low stock: Nitrile Examination Gloves (box 100)", "Nitrile Examination Gloves (box 100) (MED-2001) is at 18 units, critical level 40.", "/inventory?tab=alerts", 34, true},
                    {"manager", "FLIGHT", "SUCCESS", "Booking confirmed: NXR4P9", "Demo booking NXR4P9 on SW610 is confirmed. Confirmation CNF-NXR4P9.", "/flights?tab=bookings", 40, true},
                    {"admin", "WORKFLOW", "INFO", "Approval needed: " + n2, "\"Purchase of 16 wireless handheld terminals\" is waiting for Final Approval.", "/documents/" + docs.get("purchaseLarge"), 26, false},
                    {"admin", "WORKFLOW", "INFO", "Approval needed: " + n6, "\"Updated shift rota for November\" is waiting for Manager Review.", "/documents/" + docs.get("rota"), 19, false},
                    {"admin", "INVENTORY", "CRITICAL", "Out of stock: Disposable Syringes 5 ml (box 100)", "Disposable Syringes 5 ml (box 100) (MED-2004) is at 0 units, critical level 20.", "/inventory?tab=alerts", 50, false},
                    {"doctor", "CLINIC", "INFO", "New appointment", "Madina Serikova booked a blood pressure review.", "/clinic", 30, false},
                    {"doctor", "CLINIC", "INFO", "New appointment", "Askar Zhumabayev booked a follow-up visit.", "/clinic", 80, true},
                    {"doctor", "WORKFLOW", "WARNING", "Rejected: " + n5, "\"Visit report: follow-up cardiology consultation\" was rejected. Reason: Please attach the follow-up plan and file a new report.", "/documents/" + docs.get("rejected"), 60, false},
                    {"user", "FLIGHT", "SUCCESS", "Booking confirmed: NXR7K2", "Demo booking NXR7K2 on SN101 is confirmed. Confirmation CNF-NXR7K2.", "/flights?tab=bookings", 44, false},
                    {"user", "FLIGHT", "WARNING", "Booking cancelled: NXR8M3", "Booking NXR8M3 on SR501 was cancelled. Change of plans", "/flights?tab=bookings", 52, true},
                    {"user", "FLIGHT", "WARNING", "Booking expired: NXR5T8", "Booking NXR5T8 on AJ310 expired. Seat hold expired before confirmation", "/flights?tab=bookings", 58, true},
                    {"user", "CLINIC", "SUCCESS", "Appointment booked", "Your appointment with Dr. Aigerim Nurlanova is confirmed.", "/clinic", 30, false},
            };
            for (Object[] r : rows) {
                Instant created = hoursAgo((Integer) r[6]);
                long id = insert("insert into notifications (recipient_id, category, severity, title, message, link, source_module, read_at, created_at) values (?,?,?,?,?,?,?,?,?)",
                        users.get((String) r[0]), r[1], r[2], r[3], r[4], r[5], r[1], (Boolean) r[7] ? created.plus(Duration.ofHours(1)) : null, created);
                exec("insert into delivery_attempts (notification_id, channel, status, attempt_no, detail, created_at) values (?,?,?,?,?,?)",
                        id, "IN_APP", "DELIVERED", 1, "Stored in inbox (seeded demo data)", created);
                if ("WARNING".equals(r[2]) || "CRITICAL".equals(r[2])) {
                    exec("insert into delivery_attempts (notification_id, channel, status, attempt_no, detail, created_at) values (?,?,?,?,?,?)",
                            id, "EMAIL", "SKIPPED", 1, "SMTP not configured (demo mode) - e-mail not sent", created);
                }
            }
        }

        private void seedAudit(Map<String, Long> docs) throws SQLException {
            Object[][] rows = {
                    {null, "system", "SEED_DEMO_DATA", "Platform", null, "Demo data installed by Flyway migration V2", 0},
                    {users.get("employee"), "employee", "ADJUST_INVENTORY", "Product", products.get("MED-2001"), "WRITE_OFF 6 (Issued to clinic)", 34},
                    {null, "system", "EVENT_LowStockEvent", "INVENTORY", null, "Low stock: Nitrile Examination Gloves (box 100) (MED-2001) at 18, threshold 40", 34},
                    {users.get("employee"), "employee", "CREATE_REPLENISHMENT_REQUEST", "Document", docs.get("replenishment"), "From stock alert", 30},
                    {users.get("employee"), "employee", "SUBMIT_DOCUMENT", "Document", docs.get("replenishment"), "Submitted for approval", 5},
                    {null, "system", "EVENT_DocumentSubmittedEvent", "WORKFLOW", null, "Document " + number(docs.get("replenishment")) + " submitted for Manager Review", 5},
                    {users.get("manager"), "manager", "APPROVE_DOCUMENT", "Document", docs.get("purchaseLarge"), "Budget available in Q4", 26},
                    {null, "system", "EVENT_DocumentApprovedEvent", "WORKFLOW", null, "Document " + number(docs.get("purchaseLarge")) + " moved to Final Approval", 26},
                    {users.get("manager"), "manager", "REJECT_DOCUMENT", "Document", docs.get("rejected"), "Reason: Please attach the follow-up plan", 60},
                    {users.get("user"), "user", "CONFIRM_FLIGHT_BOOKING", "FlightBooking", null, "Demo confirmation (no payment)", 44},
                    {null, "system", "EVENT_FlightBookingConfirmedEvent", "FLIGHT", null, "Booking NXR7K2 confirmed on SN101", 44},
                    {users.get("employee"), "employee", "BOOK_APPOINTMENT", "Appointment", null, "Reception booking for a fictional patient", 30},
                    {null, "system", "EVENT_AppointmentBookedEvent", "CLINIC", null, "Appointment booked: Madina Serikova with Dr. Aigerim Nurlanova", 30},
            };
            for (Object[] r : rows) {
                exec("insert into audit_logs (actor_id, actor_username, action, entity_type, entity_id, outcome, details, created_at) values (?,?,?,?,?,?,?,?)",
                        r[0], r[1], r[2], r[3], r[4] == null ? null : String.valueOf(r[4]), "SUCCESS", r[5], hoursAgo((Integer) r[6]));
            }
        }

        // ------------------------------------------------------------------ JDBC helpers
        private Instant hoursAgo(int hours) {
            return now.minus(Duration.ofHours(hours));
        }

        private String number(Long docId) throws SQLException {
            return queryString("select number from documents where id = ?", docId);
        }

        private long insert(String sql, Object... params) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        }

        private void exec(String sql, Object... params) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, params);
                ps.executeUpdate();
            }
        }

        private Object query(String sql, Object param) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, param);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getObject(1) : null;
                }
            }
        }

        private int queryInt(String sql, Object param) throws SQLException {
            return ((Number) query(sql, param)).intValue();
        }

        private long queryLong(String sql, Object param) throws SQLException {
            return ((Number) query(sql, param)).longValue();
        }

        private String queryString(String sql, Object param) throws SQLException {
            return (String) query(sql, param);
        }

        private Instant queryInstant(String sql, Object param) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, param);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC);
                }
            }
        }

        private static void bind(PreparedStatement ps, Object... params) throws SQLException {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p instanceof Instant instant) {
                    ps.setObject(i + 1, LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
                } else {
                    ps.setObject(i + 1, p);
                }
            }
        }
    }
}
