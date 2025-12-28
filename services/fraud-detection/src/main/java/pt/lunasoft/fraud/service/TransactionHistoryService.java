package pt.lunasoft.fraud.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pt.lunasoft.models.Transaction;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

	private final RedisTemplate<String, Transaction> redisTemplate;
    private static final String HISTORY_KEY_PREFIX = "transaction:history:";
    private static final long HISTORY_TTL_HOURS = 24;

    public void addTransaction(Transaction transaction) {
        String key = HISTORY_KEY_PREFIX + transaction.getAccountId();
        redisTemplate.opsForList().leftPush(key, transaction);
        redisTemplate.expire(key, HISTORY_TTL_HOURS, TimeUnit.HOURS);
    }

    public List<Transaction> getRecentTransactions(String accountId, int limit) {
        String key = HISTORY_KEY_PREFIX + accountId;
        List<Transaction> transactions = redisTemplate.opsForList().range(key, 0, limit - 1);
        return transactions != null ? transactions : new ArrayList<>();
    }

    public BigDecimal getTotalAmountSince(String accountId, Instant since) {
        List<Transaction> transactions = getRecentTransactions(accountId, 100);
        return transactions.stream().filter(t -> t.getTimestamp().isAfter(since)).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
	
}