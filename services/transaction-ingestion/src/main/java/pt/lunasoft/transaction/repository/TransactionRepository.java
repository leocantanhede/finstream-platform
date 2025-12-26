package pt.lunasoft.transaction.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.lunasoft.models.enums.TransactionStatus;
import pt.lunasoft.transaction.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

	Page<TransactionEntity> findByAccountId(String accountId, Pageable pageable);

    List<TransactionEntity> findByStatus(TransactionStatus status);

    @Query("SELECT t FROM TransactionEntity t WHERE t.accountId = :accountId AND t.timestamp BETWEEN :startTime AND :endTime")
    List<TransactionEntity> findByAccountIdAndTimestampBetween(@Param("accountId") String accountId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.accountId = :accountId AND t.timestamp >= :since")
    Long countRecentTransactionsByAccount(@Param("accountId") String accountId, @Param("since") Instant since);
	
}