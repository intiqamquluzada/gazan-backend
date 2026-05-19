package az.qazan.backend.media.seeder;

import az.qazan.backend.companies.domain.BusinessCategory;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives the seeded demo businesses real cover photos so the discover
 * feed and restaurant profile look populated. Idempotent: only touches
 * known demo companies that still have no photos. Owners replace these
 * with their own uploads via the "Mənim biznesim" screen.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DemoPhotoSeeder {

    /** The 8 companies created by {@code DevDataSeeder}. */
    private static final Set<String> DEMO_NAMES = Set.of(
            "The Bagel Bar", "Coffee Moose", "Saray Burger", "Glow Studio",
            "Aqua Wash", "Fit Lab", "Sweet House", "Barber House");

    private static final String U =
            "https://images.unsplash.com/photo-";
    private static final String Q = "?auto=format&fit=crop&w=1200&q=70";

    private static final Map<BusinessCategory, List<String>> BY_CATEGORY = Map.of(
            BusinessCategory.COFFEE, List.of(
                    U + "1495474472287-4d71bcdd2085" + Q,
                    U + "1453614512568-c4024d13c247" + Q,
                    U + "1442512595331-e89e73853f31" + Q),
            BusinessCategory.RESTAURANT, List.of(
                    U + "1517248135467-4c7edcad34c4" + Q,
                    U + "1414235077428-338989a2e8c0" + Q,
                    U + "1552566626-52f8b828add9" + Q),
            BusinessCategory.BEAUTY, List.of(
                    U + "1560066984-138dadb4c035" + Q,
                    U + "1487412947147-5cebf100ffc2" + Q,
                    U + "1522337360788-8b13dee7a37e" + Q),
            BusinessCategory.BARBER, List.of(
                    U + "1503951914875-452162b0f3f1" + Q,
                    U + "1521590832167-7bcbfaa6381f" + Q,
                    U + "1599351431202-1e0f0137899a" + Q),
            BusinessCategory.CARWASH, List.of(
                    U + "1607860108855-64acf2078ed9" + Q,
                    U + "1605164599901-db7f68c1a1b3" + Q,
                    U + "1520340356584-f9917d1eea6f" + Q),
            BusinessCategory.FITNESS, List.of(
                    U + "1534438327276-14e5300c3a48" + Q,
                    U + "1571902943202-507ec2618e8f" + Q,
                    U + "1517836357463-d25dfeac3438" + Q),
            BusinessCategory.BAKERY, List.of(
                    U + "1509440159596-0249088772ff" + Q,
                    U + "1486427944299-d1955d23e34d" + Q,
                    U + "1555507036-ab1f4038808a" + Q),
            BusinessCategory.OTHER, List.of(
                    U + "1441986300917-64674bd600d8" + Q,
                    U + "1517248135467-4c7edcad34c4" + Q));

    private final CompanyRepository companies;

    @Bean
    public ApplicationRunner seedDemoPhotos() {
        return args -> {
            int touched = 0;
            for (Company c : companies.findAll()) {
                if (!DEMO_NAMES.contains(c.getName())) continue;
                String existing = c.getPhotoUrls();
                if (existing != null && !existing.isBlank()) continue;
                List<String> urls = BY_CATEGORY.getOrDefault(
                        c.getCategory(),
                        BY_CATEGORY.get(BusinessCategory.OTHER));
                c.setPhotoUrls(String.join("\n", urls));
                companies.save(c);
                touched++;
            }
            if (touched > 0) {
                log.info("Demo photos seeded for {} companies.", touched);
            } else {
                log.info("Demo photos already present — skipping photo seed.");
            }
        };
    }
}
