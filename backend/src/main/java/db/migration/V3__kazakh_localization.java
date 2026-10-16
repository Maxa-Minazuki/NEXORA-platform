package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Translates the demo data created by V2 (and English texts produced by earlier versions of the
 * application) into Kazakh. The migration only UPDATEs text columns: no rows are deleted, ids and
 * relations are untouched, enum/status codes stay as they are. Every statement is idempotent -
 * already translated rows simply no longer match the English source text.
 */
public class V3__kazakh_localization extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new Translator(context.getConnection()).run();
    }

    static final class Translator {

        private final Connection c;

        Translator(Connection c) {
            this.c = c;
        }

        void run() throws SQLException {
            people();
            inventory();
            clinic();
            flights();
            workflow();
            notifications();
            audit();
        }

        // ------------------------------------------------------------------ people
        private static final String[][] PERSON_NAMES = {
                {"Dr. Aigerim Nurlanova", "Айгерім Нұрланова"}, {"Dr. Nurlan Abenov", "Нұрлан Әбенов"},
                {"Dr. Saule Karimova", "Сәуле Кәрімова"}, {"Dr. Arman Tulegenov", "Арман Төлегенов"},
                {"Dr. Dana Utepova", "Дана Өтепова"}, {"Dr. Yerlan Mukhamedov", "Ерлан Мұхамедов"},
                {"Aruzhan Sadykova", "Аружан Садықова"}, {"Daniyar Bekov", "Данияр Беков"},
                {"Timur Akhmetov", "Тимур Ахметов"}, {"Madina Serikova", "Мадина Серікова"},
                {"Arman Serikov", "Арман Серіков"}, {"Askar Zhumabayev", "Асқар Жұмабаев"},
                {"Laura Ismailova", "Лаура Исмаилова"}, {"Bekzat Ospanov", "Бекзат Оспанов"},
                {"Gulnara Tokayeva", "Гүлнара Тоқаева"}, {"Ruslan Kim", "Руслан Ким"},
                {"Aliya Nurpeisova", "Әлия Нұрпейісова"}, {"Ivan Petrenko", "Иван Петренко"},
                {"Zarina Abdullayeva", "Зарина Абдуллаева"}, {"Erik Sultanov", "Ерік Сұлтанов"},
                {"Kamila Rakhimova", "Камила Рахимова"}, {"Marat Yesenov", "Марат Есенов"},
                {"Aigul Baimukhanova", "Айгүл Баймұханова"}, {"Timur Seitkali", "Тимур Сейітқали"},
                {"Lab Passenger", "Зертхана жолаушысы"},
        };

        private void people() throws SQLException {
            exact("users", "full_name", PERSON_NAMES);
            exact("users", "title", new String[][]{
                    {"Platform Administrator", "Платформа әкімшісі"}, {"Operations Manager", "Операциялық менеджер"},
                    {"Cardiologist", "Кардиолог-дәрігер"}, {"Warehouse & Reception Specialist", "Қойма және тіркеу маманы"},
                    {"Customer", "Клиент"}});
            exact("doctors", "full_name", PERSON_NAMES);
            exact("booking_passengers", "full_name", PERSON_NAMES);
            exact("patients", "first_name", new String[][]{
                    {"Madina", "Мадина"}, {"Askar", "Асқар"}, {"Laura", "Лаура"}, {"Bekzat", "Бекзат"}, {"Gulnara", "Гүлнара"},
                    {"Ruslan", "Руслан"}, {"Aliya", "Әлия"}, {"Ivan", "Иван"}, {"Zarina", "Зарина"}, {"Erik", "Ерік"},
                    {"Kamila", "Камила"}, {"Marat", "Марат"}, {"Aigul", "Айгүл"}, {"Timur", "Тимур"}});
            exact("patients", "last_name", new String[][]{
                    {"Serikova", "Серікова"}, {"Zhumabayev", "Жұмабаев"}, {"Ismailova", "Исмаилова"}, {"Ospanov", "Оспанов"},
                    {"Tokayeva", "Тоқаева"}, {"Kim", "Ким"}, {"Nurpeisova", "Нұрпейісова"}, {"Petrenko", "Петренко"},
                    {"Abdullayeva", "Абдуллаева"}, {"Sultanov", "Сұлтанов"}, {"Rakhimova", "Рахимова"}, {"Yesenov", "Есенов"},
                    {"Baimukhanova", "Баймұханова"}, {"Seitkali", "Сейітқали"}});
            exact("patients", "notes", new String[][]{
                    {"Fictional demo patient - not a real person.", "Ойдан шығарылған демо-пациент - нақты адам емес."}});
        }

        // ------------------------------------------------------------------ inventory
        private static final String[][] PRODUCTS = {
                // sku, name, description
                {"ELC-1001", "Штрихкод сканері Pro X2", "Қабылдау үстелдеріне арналған сымды 2D сканер"},
                {"ELC-1002", "Термоэтикетка принтері", "4 дюймдік этикеткаларға арналған термобасып шығару"},
                {"ELC-1003", "Сымсыз қол терминалы", "Қор түгендеуге арналған берік Android терминалы"},
                {"ELC-1004", "USB-C қондыру станциясы", "Екі мониторға арналған қондыру станциясы"},
                {"MED-2001", "Нитрил тексеру қолғаптары (100 дана)", "Ұнтақсыз, M өлшемі"},
                {"MED-2002", "Стерильді дәке салфеткалар 10x10", "25 стерильді салфетка бумасы"},
                {"MED-2003", "Цифрлық термометр", "Жылдам өлшейтін клиникалық термометр"},
                {"MED-2004", "Бір реттік шприцтер 5 мл (100 дана)", "Luer lock, стерильді"},
                {"MED-2005", "Қан қысымын өлшегіш", "Иыққа арналған автоматты тонометр"},
                {"OFF-3001", "A4 кеңсе қағазы 80 г (десте)", "Бір дестеде 500 парақ"},
                {"OFF-3002", "Көк шарикті қаламдар (50 дана)", "Орташа ұш, көк сия"},
                {"OFF-3003", "Эргономикалық кеңсе орындығы", "Торлы арқалық, белді реттеу"},
                {"OFF-3004", "Құжаттарға арналған мұрағат қорабы", "A4 картон мұрағат қорабы"},
                {"PKG-4001", "Гофрленген жөнелту қорабы M", "40x30x30 см, екі қабатты"},
                {"PKG-4002", "Стретч-үлдір 500 мм", "23 микрондық машиналық үлдір"},
                {"PKG-4003", "Көпіршікті үлдір 1 м x 50 м", "Қорғаныш көпіршікті үлдір"},
                {"CLN-5001", "Қол антисептигі 1 л", "Сорғышы бар 70% спирт гелі"},
                {"CLN-5002", "Беттерге арналған дезинфектант 5 л", "Аурухана деңгейіндегі дезинфекциялық құрал"},
                {"CLN-5003", "Микрофибра шүберектер жиынтығы", "Түрлі түсті 10 шүберек"},
                {"LAB-6001", "Центрифуга түтікшелері 15 мл (500 дана)", "Полипропилен, стерильді"},
                {"LAB-6002", "Пипетка ұштықтары 200 мкл (96 дана)", "Әмбебап, штативте"},
                {"LAB-SENSOR-01", "Демо зертхана сенсор жиынтығы", "Паттерндер зертханасының сценарийлеріне арналған"},
        };

        /** Original English product names, in the same order as {@link #PRODUCTS}. */
        private static final String[] PRODUCT_EN = {
                "Barcode Scanner Pro X2", "Thermal Label Printer", "Wireless Handheld Terminal", "USB-C Docking Station",
                "Nitrile Examination Gloves (box 100)", "Sterile Gauze Pads 10x10", "Digital Thermometer",
                "Disposable Syringes 5 ml (box 100)", "Blood Pressure Monitor", "A4 Copy Paper 80g (ream)",
                "Ballpoint Pens Blue (pack 50)", "Ergonomic Office Chair", "Document Archive Box", "Corrugated Shipping Box M",
                "Stretch Wrap Film 500mm", "Bubble Wrap Roll 1m x 50m", "Hand Sanitizer 1L", "Surface Disinfectant 5L",
                "Microfiber Cloth Pack", "Centrifuge Tubes 15 ml (pack 500)", "Pipette Tips 200 ul (rack 96)", "Demo Lab Sensor Kit",
        };

        private void inventory() throws SQLException {
            String[][] cats = {
                    {"Electronics", "Электроника", "Сканерлер, терминалдар, принтерлер және IT керек-жарақтары"},
                    {"Medical Supplies", "Медициналық керек-жарақтар", "Емханаға арналған шығын материалдары мен құрылғылар"},
                    {"Office Supplies", "Кеңсе тауарлары", "Қағаз, кеңсе заттары және жиһаз"},
                    {"Packaging", "Қаптама", "Қораптар, үлдірлер және қорғаныс материалдары"},
                    {"Cleaning & Hygiene", "Тазалық және гигиена", "Антисептиктер, дезинфекциялық және тазалау құралдары"},
                    {"Lab Equipment", "Зертхана жабдықтары", "Зертханалық шығын материалдары және демо жиынтықтар"},
            };
            for (String[] cat : cats) {
                exec("update categories set name = ?, description = ? where name = ?", cat[1], cat[2], cat[0]);
            }
            exact("suppliers", "name", new String[][]{
                    {"Altyn Supply Co. (demo)", "Алтын Жабдықтау (демо)"}, {"Nomad Medical Trading (demo)", "Көшпенді Медицина Сауда (демо)"},
                    {"Orbita Electronics (demo)", "Орбита Электроника (демо)"}, {"Baiterek Office Goods (demo)", "Бәйтерек Кеңсе Тауарлары (демо)"},
                    {"Tulpar Packaging (demo)", "Тұлпар Қаптама (демо)"}});
            for (int i = 0; i < PRODUCTS.length; i++) {
                exec("update products set name = ?, description = ? where sku = ? and name = ?",
                        PRODUCTS[i][1], PRODUCTS[i][2], PRODUCTS[i][0], PRODUCT_EN[i]);
            }
            exact("products", "unit", new String[][]{
                    {"pcs", "дана"}, {"box", "қорап"}, {"pack", "бума"}, {"ream", "десте"}, {"roll", "орам"},
                    {"bottle", "бөтелке"}, {"can", "канистр"}, {"rack", "штатив"}});
            exact("inventory_transactions", "reason", new String[][]{
                    {"Opening balance", "Бастапқы қалдық"}, {"Supplier delivery", "Жеткізушіден түсім"},
                    {"Issued to clinic", "Емханаға берілді"}, {"Issued to warehouse operations", "Қойма жұмыстарына берілді"},
                    {"Lab: restock before demo", "Зертхана: демо алдында толықтыру"},
                    {"Lab: consume stock below minimum", "Зертхана: қорды ең төменгі деңгейден төмен жұмсау"}});
        }

        // ------------------------------------------------------------------ clinic
        private void clinic() throws SQLException {
            String[][] specs = {
                    {"Cardiology", "Кардиология", "Жүрек және қан тамырлары аурулары"},
                    {"General Practice", "Жалпы тәжірибе", "Алғашқы медициналық көмек және жолдамалар"},
                    {"Pediatrics", "Педиатрия", "Балалар мен жасөспірімдерді емдеу"},
                    {"Neurology", "Неврология", "Ми, жұлын және жүйке аурулары"},
                    {"Dermatology", "Дерматология", "Тері, шаш және тырнақ аурулары"},
                    {"Ophthalmology", "Офтальмология", "Көз денсаулығы және көру"},
            };
            for (String[] s : specs) {
                exec("update specialties set name = ?, description = ? where name = ?", s[1], s[2], s[0]);
            }
            exact("doctors", "bio", new String[][]{
                    {"Cardiologist with a focus on preventive care.", "Профилактикалық кардиологияға маманданған дәрігер."},
                    {"Family physician, also on Saturday mornings.", "Отбасылық дәрігер, сенбі күні таңертең де қабылдайды."},
                    {"Pediatrician for children from 0 to 17 years.", "0-ден 17 жасқа дейінгі балаларға арналған педиатр."},
                    {"Neurologist, headaches and sleep disorders.", "Невролог: бас ауруы және ұйқының бұзылуы."},
                    {"Dermatologist, Monday to Thursday.", "Дерматолог, дүйсенбіден бейсенбіге дейін."},
                    {"Ophthalmologist, vision checks and follow-ups.", "Офтальмолог: көруді тексеру және бақылау."}});
            exact("appointments", "reason", new String[][]{
                    {"Routine check-up", "Жоспарлы тексеру"}, {"Follow-up visit", "Қайта қаралу"},
                    {"Persistent headache", "Ұзаққа созылған бас ауруы"}, {"Skin rash consultation", "Тері бөртпесі бойынша кеңес"},
                    {"Blood pressure review", "Қан қысымын бақылау"}, {"Vision test", "Көруді тексеру"},
                    {"Vaccination consultation", "Вакцинация бойынша кеңес"}, {"Lab results discussion", "Талдау нәтижелерін талқылау"},
                    {"Annual physical", "Жылдық медициналық тексеру"}, {"Sleep problems", "Ұйқы мәселелері"},
                    {"Checkup", "Тексеру"}});
            exact("appointments", "notes", new String[][]{{"Seen by doctor. Demo note.", "Дәрігер қабылдады. Демо жазба."}});
            exact("appointments", "cancel_reason", new String[][]{{"Patient asked to cancel", "Пациент болдырмауды сұрады"}});
        }

        // ------------------------------------------------------------------ flights
        private void flights() throws SQLException {
            String[][] airlines = {
                    {"SkyNomad Air", "Аспан Көшпенді"}, {"Steppe Wings", "Дала Қанаттары"},
                    {"Aurora Jet", "Шапақ Әуе"}, {"Silk Route Airways", "Жібек Жолы Әуе"}};
            exact("flight_routes", "airline", airlines);
            exact("flights", "airline", airlines);
            String[][] airports = {
                    {"ALA", "Алматы халықаралық әуежайы", "Алматы", "Қазақстан"},
                    {"NQZ", "Астана халықаралық әуежайы", "Астана", "Қазақстан"},
                    {"CIT", "Шымкент халықаралық әуежайы", "Шымкент", "Қазақстан"},
                    {"AKX", "Ақтөбе халықаралық әуежайы", "Ақтөбе", "Қазақстан"},
                    {"TAS", "Ташкент халықаралық әуежайы", "Ташкент", "Өзбекстан"},
                    {"IST", "Стамбұл әуежайы", "Стамбұл", "Түркия"},
                    {"DXB", "Дубай халықаралық әуежайы", "Дубай", "Біріккен Араб Әмірліктері"},
            };
            for (String[] a : airports) {
                exec("update airports set name = ?, city = ?, country = ? where code = ?", a[1], a[2], a[3], a[0]);
            }
            exact("flight_bookings", "cancel_reason", new String[][]{
                    {"Change of plans", "Жоспар өзгерді"},
                    {"Seat hold expired before confirmation", "Орындарды ұстау мерзімі растаудан бұрын аяқталды"},
                    {"Design Patterns Lab demo", "Паттерндер зертханасының демосы"}});
            replace(new String[]{"booking_documents.content", "documents.content", "document_revisions.content"}, TICKET_PHRASES);
        }

        private static final String[][] TICKET_PHRASES = {
                {"EDUCATIONAL DEMO - NOT A REAL TICKET. No airline reservation or payment was made.",
                        "ОҚУ ДЕМОНСТРАЦИЯСЫ - НАҚТЫ БИЛЕТ ЕМЕС. Әуе компаниясында брондау немесе төлем жасалмады."},
                {"Booking reference: ", "Брондау коды: "}, {"Total (demo price): ", "Барлығы (демо баға): "},
                {"Departure: ", "Ұшу: "}, {"Arrival: ", "Келу: "}, {"Passengers:", "Жолаушылар:"}, {"Route: ", "Бағыт: "},
                {"Flight: ", "Рейс: "}, {" local time", " жергілікті уақыт"}, {"(seat ", "(орын "},
        };

        // ------------------------------------------------------------------ workflow
        private void workflow() throws SQLException {
            exact("documents", "title", new String[][]{
                    {"Purchase of 16 wireless handheld terminals", "16 сымсыз қол терминалын сатып алу"},
                    {"Office paper restock for Q4", "IV тоқсанға кеңсе қағазын толықтыру"},
                    {"Request: cardiology room equipment check", "Өтініш: кардиология кабинетінің жабдығын тексеру"},
                    {"Visit report: follow-up cardiology consultation", "Қабылдау есебі: кардиологтың қайта кеңесі"},
                    {"Updated shift rota for November", "Қарашаға арналған жаңартылған ауысым кестесі"},
                    {"Lab: equipment budget note", "Зертхана: жабдық бюджеті туралы жазба"}});
            exact("document_revisions", "title", new String[][]{
                    {"Purchase of 16 wireless handheld terminals", "16 сымсыз қол терминалын сатып алу"},
                    {"Office paper restock for Q4", "IV тоқсанға кеңсе қағазын толықтыру"},
                    {"Request: cardiology room equipment check", "Өтініш: кардиология кабинетінің жабдығын тексеру"},
                    {"Visit report: follow-up cardiology consultation", "Қабылдау есебі: кардиологтың қайта кеңесі"},
                    {"Updated shift rota for November", "Қарашаға арналған жаңартылған ауысым кестесі"},
                    {"Lab: equipment budget note", "Зертхана: жабдық бюджеті туралы жазба"}});
            String[][] contents = {
                    {"Request to purchase 16 Wireless Handheld Terminals (ELC-1003) for the Q4 stock count project.\nSupplier: Orbita Electronics (demo)\nUnit price 142000.00 KZT.",
                            "IV тоқсандағы қор түгендеу жобасына 16 сымсыз қол терминалын (ELC-1003) сатып алуға өтінім.\nЖеткізуші: Орбита Электроника (демо)\nБірлік бағасы 142000.00 KZT."},
                    {"90 reams of A4 copy paper (OFF-3001) for all departments until the end of the year.",
                            "Жыл соңына дейін барлық бөлімдерге 90 десте A4 кеңсе қағазы (OFF-3001)."},
                    {"Please schedule the yearly calibration of the ECG device and the blood pressure monitors in room 204.",
                            "204-кабинеттегі ЭКГ аппараты мен қан қысымын өлшегіштердің жылдық калибрлеуін жоспарлауды сұраймын."},
                    {"Summary of a completed follow-up visit (fictional demo record). Blood pressure stable, continue current plan.",
                            "Аяқталған қайта қабылдаудың қорытындысы (ойдан шығарылған демо жазба). Қан қысымы тұрақты, қазіргі ем жоспарын жалғастыру."},
                    {"New warehouse and reception shift rota for November, including weekend coverage for the clinic front desk.",
                            "Қарашаға арналған қойма мен тіркеу бөлімінің жаңа ауысым кестесі, емхана тіркеуінің демалыс күндерін қоса алғанда."},
                    {"Design Patterns Lab scenario: request to approve an equipment budget line for the training room.",
                            "Паттерндер зертханасының сценарийі: оқу сыныбына арналған жабдық бюджетін бекіту туралы өтініш."},
            };
            exact("documents", "content", contents);
            exact("document_revisions", "content", contents);
            String rejection = "Please attach the follow-up plan and file a new report.";
            String rejectionKk = "Бақылау жоспарын қосып, жаңа есеп тапсырыңыз.";
            exact("documents", "rejection_reason", new String[][]{{rejection, rejectionKk}});
            exact("approval_steps", "name", new String[][]{{"Manager Review", "Менеджер тексеруі"}, {"Final Approval", "Соңғы бекіту"}});
            exact("approval_steps", "decision_comment", new String[][]{
                    {"Budget available in Q4", "IV тоқсанда бюджет бар"},
                    {"Approved, standard consumables", "Бекітілді, стандартты шығын материалдары"}, {rejection, rejectionKk}});
            replace(new String[]{"documents.title", "document_revisions.title", "documents.content", "document_revisions.content",
                    "document_actions.note"}, workflowPhrases(rejection, rejectionKk));
        }

        private String[][] workflowPhrases(String rejection, String rejectionKk) {
            List<String[]> list = new ArrayList<>();
            for (int i = 0; i < PRODUCTS.length; i++) {
                list.add(new String[]{PRODUCT_EN[i], PRODUCTS[i][1]});
            }
            list.addAll(List.of(new String[][]{
                    {"Replenishment request generated from a stock alert.", "Қор ескертуі негізінде жасалған толықтыру өтінімі."},
                    {"Replenishment request generated from stock alert #", "Қор ескертуі негізінде жасалған толықтыру өтінімі, ескерту #"},
                    {"Flight booking confirmation ", "Әуе билетін брондауды растау "},
                    {"Replenish ", "Толықтыру: "}, {"Product: ", "Тауар: "}, {"Suggested order: ", "Ұсынылған тапсырыс: "},
                    {" (strategy ", " (стратегия "}, {"Unit purchase price: ", "Бірлігінің сатып алу бағасы: "},
                    {"Current quantity: ", "Ағымдағы қор: "}, {", critical threshold: ", ", шекті деңгей: "},
                    {", estimated total: ", ", болжамды сома: "},
                    {"AUTO_APPROVE: system document, no approval required", "АВТОБЕКІТУ: жүйелік құжат, бекіту қажет емес"},
                    {"ROUTE: ", "МАРШРУТ: бекіту қадамдары - "}, {" approval step(s), first: ", ", біріншісі: "},
                    {"General Administrative Document", "Жалпы әкімшілік құжат"}, {"Flight Booking Confirmation", "Брондауды растау құжаты"},
                    {"Purchase Request", "Сатып алуға өтінім"}, {"Inventory Replenishment", "Қорды толықтыру"},
                    {"Medical Appointment Report", "Медициналық қабылдау есебі"},
                    {"Manager Review", "Менеджер тексеруі"}, {"Final Approval", "Соңғы бекіту"},
                    {"Budget available in Q4", "IV тоқсанда бюджет бар"},
                    {"Approved, standard consumables", "Бекітілді, стандартты шығын материалдары"},
                    {"Revision ", "Нұсқа "}, {rejection, rejectionKk},
                    {"Lab: equipment budget note", "Зертхана: жабдық бюджеті туралы жазба"},
            }));
            return list.toArray(String[][]::new);
        }

        // ------------------------------------------------------------------ notifications
        private void notifications() throws SQLException {
            List<String[]> list = new ArrayList<>();
            list.addAll(List.of(PERSON_NAMES));
            for (int i = 0; i < PRODUCTS.length; i++) {
                list.add(new String[]{PRODUCT_EN[i], PRODUCTS[i][1]});
            }
            list.addAll(List.of(new String[][]{
                    // titles
                    {"Out of stock: ", "Тауар таусылды: "}, {"Low stock: ", "Қор аз: "}, {"Step approved: ", "Қадам бекітілді: "},
                    {"Approval needed: ", "Бекіту қажет: "}, {"Submitted: ", "Жіберілді: "}, {"Approved: ", "Бекітілді: "},
                    {"Booking confirmed: ", "Брондау расталды: "}, {"Booking cancelled: ", "Брондау болдырылмады: "},
                    {"Booking expired: ", "Брондау мерзімі өтті: "}, {"Seats on hold: ", "Орындар ұсталды: "},
                    {"Returned for revision: ", "Пысықтауға қайтарылды: "}, {"Rejected: ", "Қабылданбады: "},
                    {"Appointment booked", "Қабылдауға жазылдыңыз"}, {"New appointment", "Жаңа қабылдау"},
                    {"Appointment rescheduled", "Қабылдау уақыты өзгерді"}, {"Appointment cancelled", "Қабылдау болдырылмады"},
                    {"Test info notification", "Сынақ ақпараттық хабарландыру"}, {"Test success notification", "Сынақ сәтті хабарландыру"},
                    {"Test warning notification", "Сынақ ескерту хабарландыруы"}, {"Test critical notification", "Сынақ сыни хабарландыру"},
                    // messages
                    {"Sent from the notification center to demonstrate channel selection.", "Арнаны таңдауды көрсету үшін хабарландыру орталығынан жіберілді."},
                    {"Madina Serikova booked a blood pressure review.", "Мадина Серікова қан қысымын бақылауға жазылды."},
                    {"Askar Zhumabayev booked a follow-up visit.", "Асқар Жұмабаев қайта қаралуға жазылды."},
                    {"Your appointment with Dr. Aigerim Nurlanova is confirmed.", "Айгерім Нұрланова дәрігерге жазылуыңыз расталды."},
                    {" passed validation and was sent to ", " тексеруден өтіп, келесі қадамға жіберілді: "},
                    {" completed its approval route.", " бекіту маршрутын толық өтті."}, {" is waiting for ", " келесі қадамды күтуде: "},
                    {" moved on to ", " келесі қадамға өтті: "}, {" was rejected. Reason: ", " қабылданбады. Себебі: "},
                    {" needs changes: ", " түзетуді қажет етеді: "}, {"Your appointment with ", "Дәрігерге жазылуыңыз: "},
                    {". Suggested reorder: ", ". Ұсынылған тапсырыс: "}, {" units (", " бірлік ("}, {", critical level ", ", шекті деңгей "},
                    {" is at ", " қалдығы: "}, {"Demo booking ", "Демо-брондау "}, {" is confirmed. Confirmation ", " расталды. Растау құжаты "},
                    {" is confirmed.", " расталды."}, {" was cancelled. ", " болдырылмады. "}, {" expired. ", " мерзімі өтті. "},
                    {" is held until ", " ұсталады, мерзімі: "}, {". Confirm it before then (demo, no payment).", ". Осы уақытқа дейін растаңыз (демо, төлемсіз)."},
                    {" departing ", ", ұшу уақыты "}, {"Booking ", "Брондау "}, {" booked ", " жазылды: "},
                    {" units, critical level ", ", шекті деңгей "}, {" on ", " - "},
                    {"Change of plans", "Жоспар өзгерді"},
                    {"Seat hold expired before confirmation", "Орындарды ұстау мерзімі растаудан бұрын аяқталды"},
                    {"Manager Review", "Менеджер тексеруі"}, {"Final Approval", "Соңғы бекіту"},
                    {"Purchase of 16 wireless handheld terminals", "16 сымсыз қол терминалын сатып алу"},
                    {"Office paper restock for Q4", "IV тоқсанға кеңсе қағазын толықтыру"},
                    {"Updated shift rota for November", "Қарашаға арналған жаңартылған ауысым кестесі"},
                    {"Visit report: follow-up cardiology consultation", "Қабылдау есебі: кардиологтың қайта кеңесі"},
                    {"Please attach the follow-up plan and file a new report.", "Бақылау жоспарын қосып, жаңа есеп тапсырыңыз."},
                    {"Lab: equipment budget note", "Зертхана: жабдық бюджеті туралы жазба"},
                    {"Replenish ", "Толықтыру: "},
            }));
            replace(new String[]{"notifications.title", "notifications.message"}, list.toArray(String[][]::new));
            replace(new String[]{"delivery_attempts.detail"}, new String[][]{
                    {"Stored in inbox (seeded demo data)", "Кіріс жәшігіне сақталды (демо деректер)"},
                    {"SMTP not configured (demo mode) - e-mail not sent", "SMTP бапталмаған (демо режимі) - хат жіберілмеді"},
                    {"SMTP not configured (demo mode)", "SMTP бапталмаған (демо режимі)"},
                    {"Stored in inbox; live push to ", "Кіріс жәшігіне сақталды; ашық сеанстарға жіберілді: "},
                    {" open session(s)", ""}});
        }

        // ------------------------------------------------------------------ audit
        private void audit() throws SQLException {
            List<String[]> list = new ArrayList<>();
            list.addAll(List.of(PERSON_NAMES));
            for (int i = 0; i < PRODUCTS.length; i++) {
                list.add(new String[]{PRODUCT_EN[i], PRODUCTS[i][1]});
            }
            list.addAll(List.of(new String[][]{
                    {"Demo data installed by Flyway migration V2", "Демо деректерді Flyway V2 миграциясы орнатты"},
                    {"Reception booking for a fictional patient", "Ойдан шығарылған пациентті тіркеу бөлімі жазды"},
                    {"Submitted for approval", "Бекітуге жіберілді"}, {"Demo confirmation (no payment)", "Демо растау (төлемсіз)"},
                    {"Signed in", "Жүйеге кірді"}, {"Signed out", "Жүйеден шықты"}, {"Invalid credentials from ", "Қате тіркелгі деректері, IP "},
                    {"Seat hold expired", "Орындарды ұстау мерзімі өтті"}, {"Patient record updated", "Пациент картасы жаңартылды"},
                    {"Comment added", "Пікір қосылды"}, {"From stock alert", "Қор ескертуінен"}, {"(submitted)", "(жіберілді)"},
                    {"Budget available in Q4", "IV тоқсанда бюджет бар"}, {"Reason: ", "Себебі: "},
                    {"Please attach the follow-up plan", "Бақылау жоспарын қосыңыз"},
                    {"Issued to clinic", "Емханаға берілді"}, {"Opening balance", "Бастапқы қалдық"},
                    {"Low stock: ", "Қор аз: "}, {", threshold ", ", шегі "},
                    {"Appointment booked: ", "Қабылдауға жазылу: "}, {"Appointment cancelled: ", "Қабылдау болдырылмады: "},
                    {"Appointment rescheduled: ", "Қабылдау ауыстырылды: "}, {" with ", " - "},
                    {" submitted for Manager Review", " «Менеджер тексеруі» қадамына жіберілді"},
                    {" moved to Final Approval", " «Соңғы бекіту» қадамына өтті"},
                    {" confirmed on ", " расталды: "}, {" cancelled on ", " болдырылмады: "}, {" expired on ", " мерзімі өтті: "},
                    {" created on ", " жасалды: "}, {"Document ", "Құжат "}, {"Booking ", "Брондау "},
                    {"Manager Review", "Менеджер тексеруі"}, {"Final Approval", "Соңғы бекіту"},
            }));
            replace(new String[]{"audit_logs.details"}, list.toArray(String[][]::new));
        }

        // ------------------------------------------------------------------ helpers
        private void exact(String table, String column, String[][] pairs) throws SQLException {
            for (String[] p : pairs) {
                exec("update " + table + " set " + column + " = ? where " + column + " = ?", p[1], p[0]);
            }
        }

        /** Substring replacement, longest source first so that longer phrases win over their parts. */
        private void replace(String[] columns, String[][] pairs) throws SQLException {
            List<String[]> sorted = new ArrayList<>(List.of(pairs));
            sorted.sort(Comparator.comparingInt((String[] p) -> p[0].length()).reversed());
            for (String target : columns) {
                String[] tc = target.split("\\.");
                for (String[] p : sorted) {
                    exec("update " + tc[0] + " set " + tc[1] + " = replace(" + tc[1] + ", ?, ?) where locate(?, " + tc[1] + ") > 0",
                            p[0], p[1], p[0]);
                }
            }
        }

        private void exec(String sql, Object... params) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.executeUpdate();
            }
        }
    }
}
