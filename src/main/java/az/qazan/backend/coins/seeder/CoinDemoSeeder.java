package az.qazan.backend.coins.seeder;

import az.qazan.backend.coins.domain.CoinTransaction;
import az.qazan.backend.coins.domain.CoinTransactionRepository;
import az.qazan.backend.coins.domain.CoinTransactionType;
import az.qazan.backend.companies.domain.Company;
import az.qazan.backend.companies.domain.CompanyRepository;
import az.qazan.backend.user.domain.User;
import az.qazan.backend.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Seeds a believable coin ledger for the demo customer so the wallet
 * screen shows real numbers. Independent of {@code DevDataSeeder} (which
 * skips when companies already exist) — this keys off the coin ledger
 * being empty, so it also populates an already-seeded production DB.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class CoinDemoSeeder {

    private final CoinTransactionRepository ledger;
    private final UserRepository users;
    private final CompanyRepository companies;

    @Bean
    public ApplicationRunner seedCoinLedger() {
        return args -> {
            if (ledger.count() > 0) {
                log.info("Coin ledger already present — skipping coin seed.");
                return;
            }
            User demo = users.findByEmailIgnoreCase("demo@qazan.az").orElse(null);
            if (demo == null) {
                log.info("Demo customer absent — skipping coin seed.");
                return;
            }
            List<Company> all = companies.findAll();
            if (all.isEmpty()) {
                log.info("No companies — skipping coin seed.");
                return;
            }

            // EARN across the first few businesses, then a couple of spends.
            int[] earns = {540, 420, 360, 250, 180};
            for (int i = 0; i < earns.length && i < all.size(); i++) {
                earn(demo, all.get(i), earns[i], "Ziyarət bonusu");
            }
            // A global welcome bonus not tied to any business.
            earn(demo, null, 100, "Xoş gəldin bonusu");
            // Some spending history.
            spend(demo, all.get(0), 300, "Pulsuz qəhvə");
            spend(demo, null, 150, "Kassada nağd endirim");

            log.info("Coin ledger seeded for demo customer.");
        };
    }

    private void earn(User user, Company company, int amount, String note) {
        ledger.save(CoinTransaction.builder()
                .user(user)
                .company(company)
                .amount(amount)
                .type(CoinTransactionType.EARN)
                .note(note)
                .build());
    }

    private void spend(User user, Company company, int amount, String note) {
        ledger.save(CoinTransaction.builder()
                .user(user)
                .company(company)
                .amount(-amount)
                .type(CoinTransactionType.SPEND)
                .note(note)
                .build());
    }
}
