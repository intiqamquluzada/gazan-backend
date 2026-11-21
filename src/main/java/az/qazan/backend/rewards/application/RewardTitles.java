package az.qazan.backend.rewards.application;

import az.qazan.backend.common.i18n.MessageService;
import az.qazan.backend.loyalty.domain.LoyaltyProgram;
import az.qazan.backend.loyalty.domain.LoyaltyRewardType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * The one place that turns a stamp program's reward into words, so the
 * customer's gift list, the cashier window, the pending list and the
 * notifications all describe the same reward the same way ("Free coffee",
 * "10% off coffee", "5 ₼ off", "5 ₼ cashback").
 *
 * <p>Coin vouchers keep the catalog title the owner typed; they are not
 * built here.
 */
@Component
@RequiredArgsConstructor
public class RewardTitles {

    /** Machine-readable reward description shipped next to the title. */
    public record Info(String rewardType, BigDecimal rewardValue,
                       String rewardItem, String currency) {
        public static final Info COIN = new Info("COIN", null, null, null);
    }

    private final MessageService messages;

    /** Title in the caller's language (the request's Accept-Language). */
    public String forProgram(LoyaltyProgram program) {
        return forProgram(program, LocaleContextHolder.getLocale());
    }

    public String forProgram(LoyaltyProgram program, Locale locale) {
        if (program == null) {
            return messages.get("reward.title.free_generic", locale);
        }
        LoyaltyRewardType type = program.getRewardType() == null
                ? LoyaltyRewardType.FREE_ITEM : program.getRewardType();
        String item = blankToNull(program.getRewardItem());
        String value = number(program.getRewardValue());
        String symbol = currencySymbol(currencyOf(program));
        return switch (type) {
            case FREE_ITEM -> item == null
                    ? messages.get("reward.title.free_generic", locale)
                    : messages.get("reward.title.free_item", locale, item);
            case PERCENTAGE_DISCOUNT -> value == null
                    ? messages.get("reward.title.free_generic", locale)
                    : item == null
                            ? messages.get("reward.title.percent", locale, value)
                            : messages.get("reward.title.percent_item", locale, value, item);
            case FIXED_DISCOUNT -> value == null
                    ? messages.get("reward.title.free_generic", locale)
                    : messages.get("reward.title.fixed", locale, value, symbol);
            case CASHBACK -> value == null
                    ? messages.get("reward.title.free_generic", locale)
                    : messages.get("reward.title.cashback", locale, value, symbol);
        };
    }

    public Info infoOf(LoyaltyProgram program) {
        if (program == null) {
            return new Info(LoyaltyRewardType.FREE_ITEM.name(), null, null, null);
        }
        LoyaltyRewardType type = program.getRewardType() == null
                ? LoyaltyRewardType.FREE_ITEM : program.getRewardType();
        return new Info(type.name(), program.getRewardValue(),
                blankToNull(program.getRewardItem()), currencyOf(program));
    }

    private static String currencyOf(LoyaltyProgram program) {
        try {
            String c = program.getCompany().getCurrency();
            return c == null || c.isBlank() ? "AZN" : c;
        } catch (RuntimeException e) {
            return "AZN";
        }
    }

    static String currencySymbol(String currency) {
        return switch (currency == null ? "" : currency.toUpperCase(Locale.ROOT)) {
            case "AZN" -> "₼";
            case "TRY" -> "₺";
            default -> currency == null ? "" : currency;
        };
    }

    /** Plain digits (no grouping, no trailing zeros) so MessageFormat leaves them alone. */
    private static String number(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
