package az.qazan.backend.seeder;

import az.qazan.backend.companies.domain.BusinessCategory;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import az.qazan.backend.loyalty.domain.LoyaltyProgramRepository;
import az.qazan.backend.loyalty.domain.LoyaltyRewardType;
import az.qazan.backend.promotions.domain.Promotion;
import az.qazan.backend.promotions.domain.PromotionRepository;
import az.qazan.backend.promotions.domain.Story;
import az.qazan.backend.promotions.domain.StoryRepository;
import az.qazan.backend.user.domain.AppLocale;
import az.qazan.backend.user.domain.Role;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Loads a realistic snapshot of demo data on the {@code dev} profile.
 *
 * <p>Idempotent: skips work entirely if any companies already exist —
 * so restarting the app is safe.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder {

    private final UserRepository users;
    private final CompanyRepository companies;
    private final LoyaltyProgramRepository programs;
    private final StoryRepository stories;
    private final PromotionRepository promotions;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedDemoData() {
        return args -> {
            if (companies.count() > 0) {
                log.info("Demo data already present — skipping seed.");
                return;
            }
            log.info("Seeding demo data ...");

            // ── Demo customer ──────────────────────────────────
            User customer = users.findByEmailIgnoreCase("demo@qazan.az")
                    .orElseGet(() -> users.save(User.builder()
                            .email("demo@qazan.az")
                            .passwordHash(passwordEncoder.encode("password123"))
                            .fullName("Aysel Məmmədova")
                            .phone("+994 55 123 45 67")
                            .role(Role.CUSTOMER)
                            .locale(AppLocale.AZ)
                            .active(true)
                            .build()));

            // ── Demo business owner ────────────────────────────
            User owner = users.findByEmailIgnoreCase("biz@qazan.az")
                    .orElseGet(() -> users.save(User.builder()
                            .email("biz@qazan.az")
                            .passwordHash(passwordEncoder.encode("password123"))
                            .fullName("The Bagel Bar")
                            .businessName("The Bagel Bar")
                            .role(Role.BUSINESS_OWNER)
                            .locale(AppLocale.AZ)
                            .active(true)
                            .build()));

            // ── Companies (mirrors mobile mocks) ───────────────
            Map<String, Company> seeded = Map.ofEntries(
                    seedCompany(owner, "The Bagel Bar", "Təzə qəhvə və ev şirniyyatı",
                            BusinessCategory.COFFEE, "🥯", 0xFF7B3F00L,
                            "Nizami küç. 87, Bakı", 4.8, 412, true),
                    seedCompany(null, "Coffee Moose", "Spesialti qəhvə evi",
                            BusinessCategory.COFFEE, "☕", 0xFF1F2937L,
                            "Səbail, 28 May küç.", 4.7, 268, true),
                    seedCompany(null, "Saray Burger", "Smashed burger və evdə hazırlanan souslar",
                            BusinessCategory.RESTAURANT, "🍔", 0xFFB91C1CL,
                            "Yasamal, Mətbuat pr.", 4.5, 989, false),
                    seedCompany(null, "Glow Studio", "Saç və dırnaq baxımı",
                            BusinessCategory.BEAUTY, "💅", 0xFFDB2777L,
                            "Nəsimi, Rəsul Rza küç.", 4.9, 312, true),
                    seedCompany(null, "Aqua Wash", "Premium avtoyuma və polish",
                            BusinessCategory.CARWASH, "🚗", 0xFF0369A1L,
                            "Xətai r., H.Aslanov küç.", 4.6, 154, false),
                    seedCompany(null, "Fit Lab", "Boutique fitness studiyası",
                            BusinessCategory.FITNESS, "🏋️", 0xFF065F46L,
                            "Nizami, Bakıxanov küç.", 4.8, 96, false),
                    seedCompany(null, "Sweet House", "Tortlar və makaronlar",
                            BusinessCategory.BAKERY, "🧁", 0xFFB45309L,
                            "Səbail, İçərişəhər", 4.7, 201, false),
                    seedCompany(null, "Barber House", "Klassik kişi saç düzümü",
                            BusinessCategory.BARBER, "💈", 0xFF111827L,
                            "Yasamal, A.Salamzadə küç.", 4.9, 432, false)
            );

            // ── Loyalty programs ───────────────────────────────
            seedProgram(seeded.get("The Bagel Bar"), "5 al, 6-cı pulsuz",
                    "Hər qəhvə alanda möhür qazan. 5 möhür topla, 6-cı qəhvə bizdən hədiyyə.",
                    5, LoyaltyRewardType.FREE_ITEM, null, "qəhvə");
            seedProgram(seeded.get("The Bagel Bar"), "10 bagel klubu",
                    "10 bagel topla, sonrakı bagel hədiyyəmizdir.",
                    10, LoyaltyRewardType.FREE_ITEM, null, "bagel");
            seedProgram(seeded.get("Coffee Moose"), "7 spesialti, 8-cisi pulsuz",
                    "V60, espresso, latte — fərqi yoxdur.",
                    7, LoyaltyRewardType.FREE_ITEM, null, "spesialti qəhvə");
            seedProgram(seeded.get("Saray Burger"), "3 al, 4-cüsündə 50% endirim",
                    "3 burger sifariş et, 4-cü burger üzrə yarı qiymət.",
                    3, LoyaltyRewardType.PERCENTAGE_DISCOUNT, new BigDecimal("50"), "burger");
            seedProgram(seeded.get("Glow Studio"), "4 baxımdan sonra hədiyyə",
                    "4 manikürdən sonra 5-ci dəfə pedikür hədiyyə.",
                    4, LoyaltyRewardType.FREE_ITEM, null, "pedikür");
            seedProgram(seeded.get("Aqua Wash"), "5 yumadan sonra polish",
                    "5 standart yumadan sonra premium polish hədiyyə.",
                    5, LoyaltyRewardType.FREE_ITEM, null, "premium polish");
            seedProgram(seeded.get("Fit Lab"), "8-ci məşq evdən",
                    "Hər məşq bir möhür. 7 məşqdən sonra 8-ci məşq pulsuz.",
                    7, LoyaltyRewardType.FREE_ITEM, null, "qrup məşq");
            seedProgram(seeded.get("Sweet House"), "10-cu sifarişdə 5 ₼ cashback",
                    "10 sifarişdən sonra hesabına 5 ₼ keçirilir.",
                    10, LoyaltyRewardType.CASHBACK, new BigDecimal("5"), null);
            seedProgram(seeded.get("Barber House"), "5-ci saç düzümü pulsuz",
                    "4 saç düzümü topla, 5-ci dəfə bizim hesabımıza.",
                    4, LoyaltyRewardType.FREE_ITEM, null, "saç düzümü");

            // ── Stories ────────────────────────────────────────
            seedStory(seeded.get("The Bagel Bar"), "Yeni: Lavanda latte ☕",
                    "Bahar üçün xüsusi resept — sadəcə bu həftə.", "☕",
                    0xFF7B3F00L, 0xFFB45309L, "Sifariş et", 0);
            seedStory(seeded.get("The Bagel Bar"), "Səhər saatlarında 20% endirim",
                    "08:00 — 11:00 arası bütün qəhvə növləri üzrə.", "🌅",
                    0xFFFF8A3CL, 0xFFE85A22L, "Detal", 1);
            seedStory(seeded.get("Coffee Moose"), "Cup of the week: Ethiopia 🇪🇹",
                    "Çiyələk və limon notları ilə filtre qəhvə.", "🇪🇹",
                    0xFF1F2937L, 0xFF374151L, "Dad", 0);
            seedStory(seeded.get("Coffee Moose"), "Yeni proqram: 7 al, 8-cisi pulsuz",
                    "Spesialti qəhvələrin hamısı daxildir.", "🎁",
                    0xFF111827L, 0xFF1F2937L, "Kart al", 1);
            seedStory(seeded.get("Saray Burger"), "3 al, 4-cü 50% endirim 🍔",
                    "Yalnız bu ay üçün xüsusi sadiqlik proqramı.", "🍔",
                    0xFFB91C1CL, 0xFF7F1D1DL, "Götür", 0);
            seedStory(seeded.get("Glow Studio"), "Manikür + dizayn = 25 ₼",
                    "Çərşənbə günləri xüsusi tariflə.", "💅",
                    0xFFDB2777L, 0xFFBE185DL, "Yer ayır", 0);
            seedStory(seeded.get("Aqua Wash"), "Bu həftə — daxili xidmət hədiyyə",
                    "Hər kənar yumaya daxili təmizlik bizdən.", "🚗",
                    0xFF0369A1L, 0xFF075985L, "Görüş", 0);
            seedStory(seeded.get("Fit Lab"), "Yeni səhər yoga sinifi",
                    "Hər səhər 07:00 — ilk 3 məşq pulsuz.", "🧘",
                    0xFF065F46L, 0xFF064E3BL, "Qoş", 0);

            // ── Promotions ─────────────────────────────────────
            seedPromo(seeded.get("The Bagel Bar"), "Yeni",
                    "5 qəhvəyə 6-cı pulsuz", "The Bagel Bar — hər gün",
                    "☕", 0xFF7B3F00L, 0xFFB45309L, "Kart al", 21, 0);
            seedPromo(seeded.get("Saray Burger"), "Hot 🔥",
                    "4-cü burgerin 50% endirimli", "Saray Burger — yalnız bu ay",
                    "🍔", 0xFFB91C1CL, 0xFF7F1D1DL, "Bax", 9, 1);
            seedPromo(seeded.get("Glow Studio"), "Limitli",
                    "Manikür + pedikür = 35 ₼", "Glow Studio — çərşənbə günləri",
                    "💅", 0xFFDB2777L, 0xFFBE185DL, "Yer ayır", 5, 2);
            seedPromo(seeded.get("Fit Lab"), "Yay başlanğıcı",
                    "İlk 3 məşq tamamilə pulsuz", "Fit Lab — yeni qoşulanlar",
                    "🏋️", 0xFF059669L, 0xFF065F46L, "Qoş", 30, 3);
            seedPromo(seeded.get("Aqua Wash"), "Bu həftə",
                    "5-ci yumadan sonra polish hədiyyə", "Aqua Wash — premium",
                    "🚗", 0xFF0369A1L, 0xFF075985L, "Detal", 14, 4);

            log.info("Demo data seeded.");
        };
    }

    // ───────────────────── helpers ─────────────────────

    private Map.Entry<String, Company> seedCompany(
            User owner, String name, String tagline, BusinessCategory cat,
            String emoji, long color, String address, double rating,
            int reviews, boolean featured) {
        Company c = companies.save(Company.builder()
                .name(name)
                .tagline(tagline)
                .category(cat)
                .logoEmoji(emoji)
                .coverColorHex(color)
                .address(address)
                .rating(rating)
                .reviewCount(reviews)
                .featured(featured)
                .owner(owner)
                .build());
        return Map.entry(name, c);
    }

    private void seedProgram(Company company, String title, String description,
                             int stamps, LoyaltyRewardType type,
                             BigDecimal value, String item) {
        if (company == null) return;
        programs.save(LoyaltyProgram.builder()
                .company(company)
                .title(title)
                .description(description)
                .stampsRequired(stamps)
                .rewardType(type)
                .rewardValue(value)
                .rewardItem(item == null ? "məhsul" : item)
                .active(true)
                .build());
    }

    private void seedStory(Company company, String headline, String body,
                           String emoji, long startHex, long endHex,
                           String cta, int order) {
        if (company == null) return;
        stories.save(Story.builder()
                .company(company)
                .headline(headline)
                .body(body)
                .emoji(emoji)
                .gradientStartHex(startHex)
                .gradientEndHex(endHex)
                .cta(cta)
                .durationSeconds(5)
                .active(true)
                .sortOrder(order)
                .build());
    }

    private void seedPromo(Company company, String tag, String title,
                           String subtitle, String emoji,
                           long startHex, long endHex, String cta,
                           int endsInDays, int order) {
        promotions.save(Promotion.builder()
                .company(company)
                .tag(tag)
                .title(title)
                .subtitle(subtitle)
                .emoji(emoji)
                .gradientStartHex(startHex)
                .gradientEndHex(endHex)
                .cta(cta)
                .active(true)
                .endsAt(Instant.now().plus(Duration.ofDays(endsInDays)))
                .sortOrder(order)
                .build());
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        return Map.ofEntries(entries);
    }

    private static List<String> nonNull(List<String> list) {
        return list == null ? List.of() : list;
    }
}
