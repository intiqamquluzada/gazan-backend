package az.qazan.backend.coins.domain;

/**
 * A coin ledger entry is either money flowing in ({@link #EARN}) or out
 * ({@link #SPEND}). Balance is always the signed sum of a user's entries.
 */
public enum CoinTransactionType {
    EARN,
    SPEND
}
