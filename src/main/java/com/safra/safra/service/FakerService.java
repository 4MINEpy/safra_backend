package com.safra.safra.service;

import com.safra.safra.entity.*;
import com.safra.safra.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FakerService {

    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final TripRepository tripRepository;
    private final RideRequestRepository rideRequestRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StripePaymentRepository stripePaymentRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;

    private final Faker faker = new Faker(new Locale("en"));
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final Random random = new Random();

    // Tunisia-centric coordinates for realistic carpooling routes
    private static final double[][] TUNISIAN_CITIES = {
            {36.8065, 10.1815},  // Tunis
            {36.8002, 10.1659},  // La Marsa
            {36.7333, 10.2167},  // Ariana
            {36.7994, 10.1283},  // Sidi Bou Said
            {36.8511, 10.3241},  // Carthage
            {35.8245, 10.6346},  // Sousse
            {35.6712, 10.1008},  // Kairouan
            {34.7478, 10.7600},  // Sfax
            {36.4513, 10.7308},  // Hammamet
            {33.8869, 10.0982},  // Gabès
            {36.8927, 10.1877},  // La Goulette
            {36.7650, 10.2800},  // Manouba
            {35.7643, 10.8113},  // Monastir
            {35.5039, 11.0469},  // Mahdia
            {36.4333, 9.7500},   // Béja
            {36.1667, 8.7000},   // Jendouba
            {36.0833, 9.3667},   // Le Kef
            {34.4311, 8.7757},   // Gafsa
            {33.7072, 8.9692},   // Tozeur
            {32.9211, 10.4517},  // Djerba
    };

    // Car brands and models
    private static final Map<String, String[]> CAR_MODELS = Map.of(
            "Peugeot", new String[]{"208", "308", "3008", "2008", "508", "Partner"},
            "Renault", new String[]{"Clio", "Megane", "Captur", "Kadjar", "Symbol", "Duster"},
            "Volkswagen", new String[]{"Golf", "Polo", "Passat", "Tiguan", "T-Roc", "Caddy"},
            "Toyota", new String[]{"Yaris", "Corolla", "RAV4", "C-HR", "Hilux", "Prius"},
            "Hyundai", new String[]{"i10", "i20", "i30", "Tucson", "Santa Fe", "Accent"},
            "Kia", new String[]{"Picanto", "Rio", "Sportage", "Ceed", "Sorento", "Stonic"},
            "Fiat", new String[]{"500", "Panda", "Tipo", "Punto", "Doblo", "500X"},
            "Dacia", new String[]{"Sandero", "Logan", "Duster", "Lodgy", "Dokker", "Spring"},
            "Citroën", new String[]{"C3", "C4", "C5", "Berlingo", "C3 Aircross", "C5 Aircross"},
            "Seat", new String[]{"Ibiza", "Leon", "Arona", "Ateca", "Tarraco", "Alhambra"}
    );

    private static final String[] COLORS = {
            "Blanc", "Noir", "Gris", "Argent", "Bleu", "Rouge", "Vert", "Beige", 
            "Marron", "Orange", "Jaune", "Bordeaux", "Anthracite", "Bleu Marine"
    };

    private static final String[] TRIP_DESCRIPTIONS = {
            "🚗 Trajet régulier, musique chill acceptée",
            "🎵 Ambiance détendue, discussion sympa bienvenue",
            "📱 Silence apprécié, besoin de me concentrer",
            "☕ Arrêt café possible si besoin",
            "🐕 Petit animal de compagnie accepté",
            "🧳 Grand coffre disponible pour bagages",
            "❄️ Climatisation disponible",
            "🎒 Trajet étudiant, prix négociable",
            "💼 Trajet professionnel, ponctualité assurée",
            "🌅 Départ matinal, café offert !",
            "🌙 Retour tardif possible",
            "👨‍👩‍👧 Famille bienvenue, siège enfant disponible",
            "🚭 Véhicule non-fumeur",
            "♿ Accessible aux personnes à mobilité réduite",
            "📶 WiFi disponible à bord"
    };

    private static final String[] RIDE_COMMENTS = {
            "J'ai un sac à dos et une petite valise",
            "Je serai à l'heure exacte",
            "Merci de confirmer le point de rendez-vous",
            "Est-ce que vous pouvez me déposer près de la gare?",
            "Je voyage léger, pas de bagage encombrant",
            "Première fois que j'utilise l'app, hâte de voyager!",
            "Je peux contribuer aux frais d'autoroute",
            "Besoin d'un trajet urgent, merci!",
            "Étudiant, budget serré 😅",
            "Super profil, au plaisir de voyager ensemble!",
            "Je préfère l'avant si possible",
            "Flexible sur l'heure de départ",
            "Trajet pour aller voir la famille",
            "Retour de vacances, beaucoup de souvenirs!",
            ""
    };

    @Transactional
    public Map<String, Object> populateDatabase(int userCount, int tripsPerDriver) {
        log.info("🚀 Starting SAFRA Database Population...");
        log.info("═══════════════════════════════════════════════════════════════");
        
        Map<String, Object> stats = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Clear existing data (optional - be careful in prod!)
            clearExistingData();
            
            // Step 2: Ensure subscription plans exist
            List<SubscriptionPlan> plans = ensureSubscriptionPlans();
            stats.put("subscriptionPlans", plans.size());
            
            // Step 3: Generate users
            List<User> users = generateUsers(userCount);
            stats.put("usersCreated", users.size());
            
            // Step 4: Generate cars for drivers (60% of users get cars)
            List<Car> cars = generateCarsForUsers(users);
            stats.put("carsCreated", cars.size());
            
            // Step 5: Generate subscriptions for some users
            List<Subscription> subscriptions = generateSubscriptions(users, plans);
            stats.put("subscriptionsCreated", subscriptions.size());
            
            // Step 6: Generate trips
            List<Trip> trips = generateTrips(users, tripsPerDriver);
            stats.put("tripsCreated", trips.size());
            
            // Step 7: Generate ride requests
            List<RideRequest> rideRequests = generateRideRequests(trips, users);
            stats.put("rideRequestsCreated", rideRequests.size());
            
            // Step 8: Generate payments
            List<StripePayment> payments = generatePayments(users, plans, subscriptions);
            stats.put("paymentsCreated", payments.size());

            long duration = System.currentTimeMillis() - startTime;
            stats.put("executionTimeMs", duration);
            stats.put("status", "SUCCESS");
            
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("✅ Database population completed in {} ms", duration);
            logSummary(stats);
            
        } catch (Exception e) {
            log.error("❌ Database population failed: {}", e.getMessage(), e);
            stats.put("status", "FAILED");
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    private void clearExistingData() {
        log.info("🧹 Clearing existing data using native SQL...");
        
        // Use native SQL to handle all foreign key constraints properly
        // This handles any tables that might exist in the database
        
        try {
            // Disable foreign key checks temporarily and clear all tables
            entityManager.createNativeQuery("DELETE FROM trip_passengers").executeUpdate();
            log.info("   ✓ Trip passengers cleared");
        } catch (Exception e) {
            log.warn("   ⚠ trip_passengers table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM ride_requests").executeUpdate();
            log.info("   ✓ Ride requests cleared");
        } catch (Exception e) {
            log.warn("   ⚠ ride_requests table not found or empty");
        }
        
        try {
            // Clear any legacy payments table that might exist
            entityManager.createNativeQuery("DELETE FROM payments").executeUpdate();
            log.info("   ✓ Legacy payments cleared");
        } catch (Exception e) {
            log.warn("   ⚠ payments table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM stripe_payments").executeUpdate();
            log.info("   ✓ Stripe payments cleared");
        } catch (Exception e) {
            log.warn("   ⚠ stripe_payments table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM subscriptions").executeUpdate();
            log.info("   ✓ Subscriptions cleared");
        } catch (Exception e) {
            log.warn("   ⚠ subscriptions table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM trips").executeUpdate();
            log.info("   ✓ Trips cleared");
        } catch (Exception e) {
            log.warn("   ⚠ trips table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM cars").executeUpdate();
            log.info("   ✓ Cars cleared");
        } catch (Exception e) {
            log.warn("   ⚠ cars table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM email_verification_tokens").executeUpdate();
            log.info("   ✓ Email verification tokens cleared");
        } catch (Exception e) {
            log.warn("   ⚠ email_verification_tokens table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM password_reset_tokens").executeUpdate();
            log.info("   ✓ Password reset tokens cleared");
        } catch (Exception e) {
            log.warn("   ⚠ password_reset_tokens table not found or empty");
        }
        
        try {
            entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
            log.info("   ✓ Users cleared");
        } catch (Exception e) {
            log.warn("   ⚠ users table not found or empty");
        }
        
        // Flush changes
        entityManager.flush();
        entityManager.clear();
        
        log.info("🧹 Existing data cleared successfully");
    }

    private List<SubscriptionPlan> ensureSubscriptionPlans() {
        log.info("📋 Ensuring subscription plans exist...");
        
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        
        if (plans.isEmpty()) {
            log.info("   Creating default subscription plans...");
            
            plans = new ArrayList<>();
            plans.add(subscriptionPlanRepository.save(SubscriptionPlan.builder()
                    .name("silver")
                    .price(8.0)
                    .tripLimit(20)
                    .durationDays(30)
                    .requiresStudentVerification(false)
                    .isArchived(false)
                    .build()));
            
            plans.add(subscriptionPlanRepository.save(SubscriptionPlan.builder()
                    .name("gold")
                    .price(10.0)
                    .tripLimit(30)
                    .durationDays(30)
                    .requiresStudentVerification(false)
                    .isArchived(false)
                    .build()));
            
            plans.add(subscriptionPlanRepository.save(SubscriptionPlan.builder()
                    .name("diamond")
                    .price(40.0)
                    .tripLimit(null) // unlimited
                    .durationDays(30)
                    .requiresStudentVerification(false)
                    .isArchived(false)
                    .build()));
            
            plans.add(subscriptionPlanRepository.save(SubscriptionPlan.builder()
                    .name("student")
                    .price(5.0)
                    .tripLimit(40)
                    .durationDays(30)
                    .requiresStudentVerification(true)
                    .isArchived(false)
                    .build()));
            
            log.info("   ✓ Created {} subscription plans", plans.size());
        } else {
            log.info("   ✓ Found {} existing subscription plans", plans.size());
        }
        
        return plans;
    }

    private List<User> generateUsers(int count) {
        log.info("👥 Generating {} users...", count);
        List<User> users = new ArrayList<>();
        String encodedPassword = passwordEncoder.encode("password");
        
        Set<String> usedEmails = new HashSet<>();
        Set<String> usedPhones = new HashSet<>();
        
        // Progress tracking
        int milestone = count / 10;
        
        for (int i = 0; i < count; i++) {
            // Generate unique email
            String email;
            do {
                String firstName = faker.name().firstName().toLowerCase().replaceAll("[^a-z]", "");
                String lastName = faker.name().lastName().toLowerCase().replaceAll("[^a-z]", "");
                int randomNum = random.nextInt(999);
                email = firstName + "." + lastName + randomNum + "@" + 
                        faker.options().option("gmail.com", "yahoo.fr", "hotmail.com", "outlook.com", "safra.tn");
            } while (usedEmails.contains(email));
            usedEmails.add(email);
            
            // Generate unique phone (Tunisian format)
            String phone;
            do {
                int prefix = faker.options().option(20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 
                        50, 51, 52, 53, 54, 55, 56, 58, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99);
                phone = String.format("+216 %d %03d %03d", prefix, random.nextInt(1000), random.nextInt(1000));
            } while (usedPhones.contains(phone));
            usedPhones.add(phone);
            
            Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
            String name = gender == Gender.MALE ? 
                    faker.name().firstName() + " " + faker.name().lastName() :
                    faker.name().femaleFirstName() + " " + faker.name().lastName();
            
            // Birth date between 18-65 years ago
            LocalDate birthDate = faker.date().birthday(18, 65)
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            
            // Join date between 2 years ago and now
            LocalDateTime joinDate = faker.date()
                    .past(730, TimeUnit.DAYS)
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            
            // Student verification for ~15% of users
            boolean isStudent = random.nextDouble() < 0.15;
            String studentEmail = null;
            if (isStudent) {
                studentEmail = email.split("@")[0] + "@" + 
                        faker.options().option("enit.utm.tn", "fst.utm.tn", "insat.rnu.tn", 
                                "isi.utm.tn", "ensi.rnu.tn", "supcom.tn", "ihec.rnu.tn");
            }
            
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .phoneNumber(phone)
                    .password(encodedPassword)
                    .gender(gender)
                    .birthDate(birthDate)
                    .role(Role.CLIENT)
                    .isBanned(random.nextDouble() < 0.02) // 2% banned
                    .joinDate(joinDate)
                    .is_archived(random.nextDouble() < 0.03) // 3% archived
                    .isEmailVerified(true) // All verified as requested
                    .studentEmail(studentEmail)
                    .studentVerified(isStudent)
                    .profilePicture(null)
                    .build();
            
            users.add(userRepository.save(user));
            
            if (milestone > 0 && (i + 1) % milestone == 0) {
                log.info("   ... {} users created ({}%)", i + 1, ((i + 1) * 100 / count));
            }
        }
        
        log.info("   ✓ Created {} users (all with password: 'password')", users.size());
        return users;
    }

    private List<Car> generateCarsForUsers(List<User> users) {
        log.info("🚗 Generating cars for drivers...");
        List<Car> cars = new ArrayList<>();
        
        // 60% of users get cars
        int driversCount = (int) (users.size() * 0.6);
        List<User> shuffledUsers = new ArrayList<>(users);
        Collections.shuffle(shuffledUsers);
        
        // Track which users are drivers (have cars)
        driversWithCars.clear();
        
        Set<String> usedRegNumbers = new HashSet<>();
        
        for (int i = 0; i < driversCount; i++) {
            User user = shuffledUsers.get(i);
            
            // Pick random brand and model
            String brand = faker.options().option(CAR_MODELS.keySet().toArray(new String[0]));
            String model = faker.options().option(CAR_MODELS.get(brand));
            String color = faker.options().option(COLORS);
            
            // Tunisian registration number format
            String regNumber;
            do {
                int num = random.nextInt(9000) + 1000;
                String region = faker.options().option("TUN", "SFA", "SOU", "NAB", "MON", "GAB", 
                        "KAI", "BIZ", "GAF", "TOZ", "KEB", "JEN", "BEJ", "SIL", "KAS", "MEH", "TAT");
                regNumber = String.format("%d %s %d", num, region, random.nextInt(900) + 100);
            } while (usedRegNumbers.contains(regNumber));
            usedRegNumbers.add(regNumber);
            
            Car car = Car.builder()
                    .registrationNumber(regNumber)
                    .brand(brand)
                    .model(model)
                    .color(color)
                    .owner(user)
                    .build();
            
            cars.add(carRepository.save(car));
            driversWithCars.add(user); // Track this user as a driver
        }
        
        log.info("   ✓ Created {} cars for {} drivers", cars.size(), driversCount);
        return cars;
    }
    
    // Track users who have cars (drivers)
    private final Set<User> driversWithCars = new HashSet<>();

    private List<Subscription> generateSubscriptions(List<User> users, List<SubscriptionPlan> plans) {
        log.info("💳 Generating subscriptions...");
        List<Subscription> subscriptions = new ArrayList<>();
        
        // Get student plan
        SubscriptionPlan studentPlan = plans.stream()
                .filter(p -> "student".equals(p.getName()))
                .findFirst().orElse(null);
        
        // Non-student plans
        List<SubscriptionPlan> regularPlans = plans.stream()
                .filter(p -> !"student".equals(p.getName()))
                .toList();
        
        // 40% of users get subscriptions
        int subscriberCount = (int) (users.size() * 0.4);
        List<User> shuffledUsers = new ArrayList<>(users);
        Collections.shuffle(shuffledUsers);
        
        for (int i = 0; i < subscriberCount; i++) {
            User user = shuffledUsers.get(i);
            
            // Choose plan based on student status
            SubscriptionPlan plan;
            if (user.getStudentVerified() && studentPlan != null && random.nextDouble() < 0.7) {
                plan = studentPlan;
            } else {
                plan = regularPlans.get(random.nextInt(regularPlans.size()));
            }
            
            // Random subscription scenarios
            LocalDateTime startDate;
            LocalDateTime endDate;
            boolean isActive;
            int tripsUsed;
            
            double scenario = random.nextDouble();
            
            if (scenario < 0.4) {
                // Active subscription (40%)
                int daysAgo = random.nextInt(20);
                startDate = LocalDateTime.now().minusDays(daysAgo);
                endDate = startDate.plusDays(plan.getDurationDays());
                isActive = true;
                tripsUsed = plan.getTripLimit() != null ? 
                        random.nextInt(Math.min(plan.getTripLimit(), daysAgo * 2 + 1)) : 
                        random.nextInt(50);
            } else if (scenario < 0.7) {
                // Expired subscription (30%)
                int daysAgo = random.nextInt(60) + 35;
                startDate = LocalDateTime.now().minusDays(daysAgo);
                endDate = startDate.plusDays(plan.getDurationDays());
                isActive = false;
                tripsUsed = plan.getTripLimit() != null ? 
                        random.nextInt(plan.getTripLimit() + 1) : 
                        random.nextInt(100);
            } else if (scenario < 0.9) {
                // Almost exhausted subscription (20%)
                int daysAgo = random.nextInt(25) + 5;
                startDate = LocalDateTime.now().minusDays(daysAgo);
                endDate = startDate.plusDays(plan.getDurationDays());
                isActive = true;
                tripsUsed = plan.getTripLimit() != null ? 
                        plan.getTripLimit() - random.nextInt(3) : 
                        random.nextInt(150);
            } else {
                // Brand new subscription (10%)
                startDate = LocalDateTime.now().minusHours(random.nextInt(48));
                endDate = startDate.plusDays(plan.getDurationDays());
                isActive = true;
                tripsUsed = random.nextInt(3);
            }
            
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .pricePaid(plan.getPrice())
                    .tripLimit(plan.getTripLimit())
                    .tripsUsed(tripsUsed)
                    .isActive(isActive)
                    .startDate(startDate)
                    .endDate(endDate)
                    .isArchived(!isActive && scenario >= 0.5)
                    .build();
            
            subscriptions.add(subscriptionRepository.save(subscription));
        }
        
        log.info("   ✓ Created {} subscriptions", subscriptions.size());
        return subscriptions;
    }

    private List<Trip> generateTrips(List<User> users, int tripsPerDriver) {
        log.info("🛣️ Generating trips...");
        List<Trip> trips = new ArrayList<>();
        
        // Use the tracked drivers who have cars
        List<User> drivers = new ArrayList<>(driversWithCars);
        
        // Get potential passengers
        List<User> passengers = new ArrayList<>(users);
        
        log.info("   Found {} drivers with cars", drivers.size());
        
        int tripCount = 0;
        
        for (User driver : drivers) {
            int numTrips = random.nextInt(tripsPerDriver) + 1;
            
            for (int t = 0; t < numTrips; t++) {
                // Pick random start and end cities (different ones)
                int startIdx = random.nextInt(TUNISIAN_CITIES.length);
                int endIdx;
                do {
                    endIdx = random.nextInt(TUNISIAN_CITIES.length);
                } while (endIdx == startIdx);
                
                // Add some randomness to coordinates
                double startLat = TUNISIAN_CITIES[startIdx][0] + (random.nextDouble() - 0.5) * 0.05;
                double startLon = TUNISIAN_CITIES[startIdx][1] + (random.nextDouble() - 0.5) * 0.05;
                double endLat = TUNISIAN_CITIES[endIdx][0] + (random.nextDouble() - 0.5) * 0.05;
                double endLon = TUNISIAN_CITIES[endIdx][1] + (random.nextDouble() - 0.5) * 0.05;
                
                Point startPoint = geometryFactory.createPoint(new Coordinate(startLon, startLat));
                Point endPoint = geometryFactory.createPoint(new Coordinate(endLon, endLat));
                
                // Generate diverse trip times and statuses
                LocalDateTime startTime;
                String status;
                int availableSeats;
                List<User> tripPassengers = new ArrayList<>();
                
                double scenario = random.nextDouble();
                
                if (scenario < 0.15) {
                    // Completed trips (15%)
                    startTime = faker.date().past(30, TimeUnit.DAYS)
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    status = "COMPLETED";
                    availableSeats = random.nextInt(2);
                    tripPassengers = pickRandomPassengers(passengers, driver, random.nextInt(4));
                    
                } else if (scenario < 0.25) {
                    // Cancelled trips (10%)
                    startTime = faker.date().past(14, TimeUnit.DAYS)
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    status = "CANCELLED";
                    availableSeats = random.nextInt(4) + 1;
                    
                } else if (scenario < 0.35) {
                    // Live/ongoing trips (10%)
                    startTime = LocalDateTime.now().minusMinutes(random.nextInt(120));
                    status = "LIVE";
                    availableSeats = random.nextInt(2);
                    tripPassengers = pickRandomPassengers(passengers, driver, random.nextInt(3) + 1);
                    
                } else if (scenario < 0.50) {
                    // Expired/past open trips (15%)
                    startTime = faker.date().past(7, TimeUnit.DAYS)
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    status = "OPEN";
                    availableSeats = random.nextInt(4) + 1;
                    
                } else {
                    // Future/upcoming trips (50%)
                    startTime = faker.date().future(30, TimeUnit.DAYS)
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    status = "OPEN";
                    availableSeats = random.nextInt(4) + 1;
                    
                    // Some future trips already have passengers
                    if (random.nextDouble() < 0.3) {
                        int seatsTaken = random.nextInt(Math.min(3, availableSeats)) + 1;
                        tripPassengers = pickRandomPassengers(passengers, driver, seatsTaken);
                        availableSeats -= tripPassengers.size();
                    }
                }
                
                // Price based on estimated distance (rough approximation)
                double distanceEstimate = Math.sqrt(
                        Math.pow(endLat - startLat, 2) + Math.pow(endLon - startLon, 2)
                ) * 111; // rough km conversion
                float price = (float) (5 + distanceEstimate * (0.3 + random.nextDouble() * 0.2));
                price = Math.round(price * 2) / 2.0f; // Round to nearest 0.5
                
                String description = random.nextDouble() < 0.8 ? 
                        faker.options().option(TRIP_DESCRIPTIONS) : "";
                
                Trip trip = Trip.builder()
                        .driver(driver)
                        .passengers(tripPassengers)
                        .startLocation(startPoint)
                        .endLocation(endPoint)
                        .startTime(startTime)
                        .description(description)
                        .is_archived(status.equals("CANCELLED") || 
                                (status.equals("COMPLETED") && random.nextDouble() < 0.3))
                        .availableSeats(availableSeats)
                        .price(price)
                        .status(status)
                        .build();
                
                trips.add(tripRepository.save(trip));
                tripCount++;
            }
        }
        
        // Log trip status distribution
        Map<String, Long> statusCounts = new HashMap<>();
        trips.forEach(t -> statusCounts.merge(t.getStatus(), 1L, Long::sum));
        log.info("   Trip distribution: {}", statusCounts);
        log.info("   ✓ Created {} trips", tripCount);
        
        return trips;
    }

    private List<User> pickRandomPassengers(List<User> allUsers, User driver, int count) {
        List<User> available = allUsers.stream()
                .filter(u -> !u.getId().equals(driver.getId()))
                .toList();
        
        List<User> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled);
        
        return shuffled.stream().limit(count).toList();
    }

    private List<RideRequest> generateRideRequests(List<Trip> trips, List<User> users) {
        log.info("📝 Generating ride requests...");
        List<RideRequest> requests = new ArrayList<>();
        
        // Get open trips for new requests
        List<Trip> openTrips = trips.stream()
                .filter(t -> "OPEN".equals(t.getStatus()) && t.getAvailableSeats() > 0)
                .toList();
        
        // Get all trips for historical requests
        List<Trip> allTrips = new ArrayList<>(trips);
        
        // Generate requests for open trips
        for (Trip trip : openTrips) {
            // 0-5 requests per open trip
            int numRequests = random.nextInt(6);
            List<User> requesters = pickRandomPassengers(users, trip.getDriver(), numRequests);
            
            for (User requester : requesters) {
                // Skip if already a passenger
                if (trip.getPassengers().stream().anyMatch(p -> p.getId().equals(requester.getId()))) {
                    continue;
                }
                
                RequestStatus status;
                double scenario = random.nextDouble();
                
                if (trip.getStartTime().isAfter(LocalDateTime.now())) {
                    // Future trip
                    if (scenario < 0.5) {
                        status = RequestStatus.PENDING;
                    } else if (scenario < 0.75) {
                        status = RequestStatus.ACCEPTED;
                    } else if (scenario < 0.9) {
                        status = RequestStatus.REJECTED;
                    } else {
                        status = RequestStatus.CANCELLED;
                    }
                } else {
                    // Past trip
                    if (scenario < 0.1) {
                        status = RequestStatus.PENDING; // forgotten request
                    } else if (scenario < 0.6) {
                        status = RequestStatus.ACCEPTED;
                    } else if (scenario < 0.85) {
                        status = RequestStatus.REJECTED;
                    } else {
                        status = RequestStatus.CANCELLED;
                    }
                }
                
                LocalDateTime createdAt = trip.getStartTime().minusDays(random.nextInt(7) + 1);
                if (createdAt.isAfter(LocalDateTime.now())) {
                    createdAt = LocalDateTime.now().minusHours(random.nextInt(48) + 1);
                }
                
                LocalDateTime updatedAt = createdAt;
                if (status != RequestStatus.PENDING) {
                    updatedAt = createdAt.plusHours(random.nextInt(24) + 1);
                    if (updatedAt.isAfter(LocalDateTime.now())) {
                        updatedAt = LocalDateTime.now();
                    }
                }
                
                String comment = random.nextDouble() < 0.7 ? 
                        faker.options().option(RIDE_COMMENTS) : "";
                
                RideRequest request = RideRequest.builder()
                        .trip(trip)
                        .passenger(requester)
                        .status(status)
                        .comment(comment)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .build();
                
                requests.add(rideRequestRepository.save(request));
            }
        }
        
        // Log request status distribution
        Map<RequestStatus, Long> statusCounts = new HashMap<>();
        requests.forEach(r -> statusCounts.merge(r.getStatus(), 1L, Long::sum));
        log.info("   Request distribution: {}", statusCounts);
        log.info("   ✓ Created {} ride requests", requests.size());
        
        return requests;
    }

    private List<StripePayment> generatePayments(List<User> users, List<SubscriptionPlan> plans, 
                                                   List<Subscription> subscriptions) {
        log.info("💰 Generating payment records...");
        List<StripePayment> payments = new ArrayList<>();
        
        // Create payments for existing subscriptions
        for (Subscription subscription : subscriptions) {
            PaymentStatus status;
            LocalDateTime completedAt = null;
            String failureMessage = null;
            
            if (subscription.getIsActive()) {
                status = PaymentStatus.SUCCEEDED;
                completedAt = subscription.getStartDate().plusMinutes(random.nextInt(10) + 1);
            } else if (random.nextDouble() < 0.1) {
                status = PaymentStatus.REFUNDED;
                completedAt = subscription.getStartDate().plusMinutes(random.nextInt(10) + 1);
            } else {
                status = PaymentStatus.SUCCEEDED;
                completedAt = subscription.getStartDate().plusMinutes(random.nextInt(10) + 1);
            }
            
            StripePayment payment = StripePayment.builder()
                    .stripeSessionId("cs_fake_" + UUID.randomUUID().toString().substring(0, 24))
                    .stripePaymentIntentId("pi_fake_" + UUID.randomUUID().toString().substring(0, 24))
                    .stripeCustomerId("cus_fake_" + UUID.randomUUID().toString().substring(0, 14))
                    .user(subscription.getUser())
                    .plan(subscription.getPlan())
                    .subscription(subscription)
                    .amount(subscription.getPricePaid())
                    .currency("usd")
                    .status(status)
                    .checkoutUrl(null) // Already completed
                    .successUrl("http://localhost:4200/subscription/success")
                    .cancelUrl("http://localhost:4200/subscription/cancel")
                    .paymentMethod("card")
                    .receiptEmail(subscription.getUser().getEmail())
                    .receiptUrl("https://pay.stripe.com/receipts/fake_" + UUID.randomUUID().toString().substring(0, 10))
                    .failureMessage(failureMessage)
                    .createdAt(subscription.getStartDate().minusMinutes(random.nextInt(5) + 1))
                    .completedAt(completedAt)
                    .expiresAt(subscription.getStartDate().plusMinutes(30))
                    .isArchived(false)
                    .build();
            
            payments.add(stripePaymentRepository.save(payment));
        }
        
        // Add some failed/pending/cancelled payments for realism
        int additionalPayments = (int) (subscriptions.size() * 0.15);
        List<User> shuffledUsers = new ArrayList<>(users);
        Collections.shuffle(shuffledUsers);
        
        for (int i = 0; i < additionalPayments && i < shuffledUsers.size(); i++) {
            User user = shuffledUsers.get(i);
            SubscriptionPlan plan = plans.get(random.nextInt(plans.size()));
            
            PaymentStatus status;
            String failureMessage = null;
            LocalDateTime completedAt = null;
            
            double scenario = random.nextDouble();
            if (scenario < 0.4) {
                status = PaymentStatus.FAILED;
                failureMessage = faker.options().option(
                        "Your card was declined.",
                        "Insufficient funds.",
                        "Your card's expiration year is invalid.",
                        "Your card's security code is incorrect.",
                        "An error occurred while processing your card."
                );
            } else if (scenario < 0.7) {
                status = PaymentStatus.EXPIRED;
            } else {
                status = PaymentStatus.CANCELLED;
            }
            
            LocalDateTime createdAt = faker.date().past(60, TimeUnit.DAYS)
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            
            StripePayment payment = StripePayment.builder()
                    .stripeSessionId("cs_fake_" + UUID.randomUUID().toString().substring(0, 24))
                    .stripePaymentIntentId(status == PaymentStatus.FAILED ? 
                            "pi_fake_" + UUID.randomUUID().toString().substring(0, 24) : null)
                    .stripeCustomerId(null)
                    .user(user)
                    .plan(plan)
                    .subscription(null)
                    .amount(plan.getPrice())
                    .currency("usd")
                    .status(status)
                    .checkoutUrl(null)
                    .successUrl("http://localhost:4200/subscription/success")
                    .cancelUrl("http://localhost:4200/subscription/cancel")
                    .paymentMethod(status == PaymentStatus.FAILED ? "card" : null)
                    .receiptEmail(null)
                    .receiptUrl(null)
                    .failureMessage(failureMessage)
                    .createdAt(createdAt)
                    .completedAt(completedAt)
                    .expiresAt(createdAt.plusMinutes(30))
                    .isArchived(true)
                    .build();
            
            payments.add(stripePaymentRepository.save(payment));
        }
        
        // Log payment status distribution
        Map<PaymentStatus, Long> statusCounts = new HashMap<>();
        payments.forEach(p -> statusCounts.merge(p.getStatus(), 1L, Long::sum));
        log.info("   Payment distribution: {}", statusCounts);
        log.info("   ✓ Created {} payment records", payments.size());
        
        return payments;
    }

    private void logSummary(Map<String, Object> stats) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           🎉 SAFRA DATABASE POPULATION SUMMARY 🎉            ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  👥 Users created:          {}                              ║", String.format("%6s", stats.get("usersCreated")));
        log.info("║  🚗 Cars created:           {}                              ║", String.format("%6s", stats.get("carsCreated")));
        log.info("║  💳 Subscriptions created:  {}                              ║", String.format("%6s", stats.get("subscriptionsCreated")));
        log.info("║  🛣️  Trips created:          {}                              ║", String.format("%6s", stats.get("tripsCreated")));
        log.info("║  📝 Ride requests created:  {}                              ║", String.format("%6s", stats.get("rideRequestsCreated")));
        log.info("║  💰 Payments created:       {}                              ║", String.format("%6s", stats.get("paymentsCreated")));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  ⏱️  Execution time:         {} ms                           ║", String.format("%6s", stats.get("executionTimeMs")));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  🔑 Default password for all users: 'password'              ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Quick stats about current database state
     */
    public Map<String, Long> getDatabaseStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("users", userRepository.count());
        stats.put("cars", carRepository.count());
        stats.put("trips", tripRepository.count());
        stats.put("rideRequests", rideRequestRepository.count());
        stats.put("subscriptions", subscriptionRepository.count());
        stats.put("subscriptionPlans", subscriptionPlanRepository.count());
        stats.put("payments", stripePaymentRepository.count());
        return stats;
    }
}
