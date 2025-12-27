package pt.lunasoft.transaction.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.lunasoft.models.Transaction;
import pt.lunasoft.models.enums.TransactionStatus;
import pt.lunasoft.transaction.entity.TransactionEntity;
import pt.lunasoft.transaction.exception.TransactionNotFoundException;
import pt.lunasoft.transaction.mapper.TransactionMapper;
import pt.lunasoft.transaction.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

	private final TransactionRepository transactionRepository;
	private final TransactionMapper transactionMapper;
	private final KafkaProducerService kafkaProducerService;

	@Transactional
	public Transaction createTransaction(Transaction transaction) {
		log.info("Creating transaction for account: {}", transaction.getAccountId());

		// Set initial values
		transaction.setId(UUID.randomUUID());
		transaction.setStatus(TransactionStatus.PENDING);
		transaction.setCreatedAt(Instant.now());

		// Save to database
		TransactionEntity entity = transactionMapper.toEntity(transaction);
		TransactionEntity saved = transactionRepository.save(entity);

		Transaction savedTransaction = transactionMapper.toModel(saved);

		// Publish to Kafka
		kafkaProducerService.sendTransaction(savedTransaction);

		log.info("Transaction created successfully: {}", savedTransaction.getId());
		return savedTransaction;
	}

	@Cacheable(value = "transactions", key = "#id")
	public Transaction getTransactionById(UUID id) {
		log.debug("Fetching transaction: {}", id);
		return transactionRepository.findById(id).map(transactionMapper::toModel).orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));
	}

	public Page<Transaction> getTransactionsByAccount(String accountId, Pageable pageable) {
		log.debug("Fetching transactions for account: {}", accountId);
		return transactionRepository.findByAccountId(accountId, pageable).map(transactionMapper::toModel);
	}

	@Transactional
	public Transaction updateTransactionStatus(UUID id, TransactionStatus status) {
		log.info("Updating transaction {} status to {}", id, status);

		TransactionEntity entity = transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id));

		entity.setStatus(status);
		entity.setUpdatedAt(Instant.now());

		TransactionEntity updated = transactionRepository.save(entity);
		return transactionMapper.toModel(updated);
	}

}