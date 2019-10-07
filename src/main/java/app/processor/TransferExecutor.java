package app.processor;

import app.config.DatabaseContextProvider;
import app.domain.TransferStatus;
import app.dto.ProcessingEventCtx;
import app.exception.ProcessingMismatch;
import app.repository.TransferRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.generated.tables.records.AccountsRecord;
import org.jooq.generated.tables.records.TransfersRecord;
import org.jooq.impl.DSL;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.generated.tables.Accounts.ACCOUNTS;
import static org.jooq.generated.tables.Transfers.TRANSFERS;

@Slf4j
public class TransferExecutor implements Runnable {
    private static long SLEEP_TIME = 500;
    public Thread t;
    private volatile boolean running = true;
    TransferRepository transferRepository;
    DatabaseContextProvider contextProvider;
    EventsProducer eventsProducer;

    public TransferExecutor(TransferRepository transferRepository, DatabaseContextProvider contextProvider) {
        t = new Thread(this, "Transfer-Executor");
        this.transferRepository = transferRepository;
        this.contextProvider = contextProvider;
        eventsProducer = new EventsProducer();
    }

    @Override
    public void run() {
        while (running) {
            Optional<TransfersRecord> transferToExecute = Optional.ofNullable(transferRepository.getNextTransferForExecution());
            if (transferToExecute.isPresent()) {
                TransfersRecord transfer = transferToExecute.get();
                log.info("Found transfer to process {} with status {}",
                        transfer.getId(),
                        transfer.getStatus()
                );
                markTransferAsInProgress(transfer);
                ProcessingResult processingResult = doProcessing(transfer);
                produceEvent(transfer, processingResult);
                log.info("Sleeping for {}ms after processing attempt", SLEEP_TIME);
            }
            sleep(SLEEP_TIME);
        }
    }

    private void produceEvent(TransfersRecord transfer, ProcessingResult processingResult) {
        ProcessingEventCtx context = new ProcessingEventCtx(transfer.getId());
        context.setSourceAccount(transfer.getSourceAccount());
        context.setDestinationAccount(transfer.getDestinationAccount());
        eventsProducer.produceProcessingResultEvent(processingResult, context);
    }

    public void terminate() {
        running = false;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Thread interrupted: {}", e.getMessage());
        }
    }

    private void markTransferAsInProgress(TransfersRecord transfer) {
        contextProvider.getContext().transaction(ctx -> {
            DSL.using(ctx)
                .update(TRANSFERS)
                .set(TRANSFERS.STATUS, TransferStatus.PROCESSING.getStatus())
                .set(TRANSFERS.UPDATED_ON, new Timestamp(System.currentTimeMillis()))
                .where(TRANSFERS.ID.eq(transfer.getId()))
                .execute();
        });
    }

    private ProcessingResult doProcessing(TransfersRecord transfer) {
        return contextProvider.getContext().transactionResult(configuration -> {
            DSLContext ctx = DSL.using(configuration);

            Result<AccountsRecord> recordsBefore = ctx
                    .selectFrom(ACCOUNTS)
                    .where(ACCOUNTS.ID.in(transfer.getSourceAccount(), transfer.getDestinationAccount()))
                    .maxRows(2)
                    .fetch();

            AccountsRecord sourceAccBeforeTransfer = getAccountsRecordFromList(recordsBefore, transfer.getSourceAccount());
            AccountsRecord destAccBeforeTransfer = getAccountsRecordFromList(recordsBefore, transfer.getDestinationAccount());

            BigDecimal srcBalanceExpected = sourceAccBeforeTransfer.getBalance().subtract(transfer.getAmount());
            if (srcBalanceExpected.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Transfer discarded {}", transfer.getId());
                updateTransferStatus(ctx, transfer.getId(), TransferStatus.DISCARD);
                return ProcessingResult.DISCARD_NO_FUNDS;
            }

            try {
                BigDecimal destBalanceExpected = destAccBeforeTransfer.getBalance().add(transfer.getAmount());
                int destInTransfersCounter = destAccBeforeTransfer.getInTransfersCount() + 1;
                updateAccountBalance(ctx, transfer.getSourceAccount(), srcBalanceExpected);
                updateAccountBalance(ctx, transfer.getDestinationAccount(), destBalanceExpected, destInTransfersCounter);


                Result<AccountsRecord> recordsAfter = ctx
                        .selectFrom(ACCOUNTS)
                        .where(ACCOUNTS.ID.in(transfer.getSourceAccount(), transfer.getDestinationAccount()))
                        .maxRows(2)
                        .fetch();

                AccountsRecord sourceAcc = getAccountsRecordFromList(recordsAfter, transfer.getSourceAccount());
                AccountsRecord destAcc = getAccountsRecordFromList(recordsAfter, transfer.getDestinationAccount());


                if (sourceAcc.getBalance().compareTo(srcBalanceExpected) != 0) {
                    throw new ProcessingMismatch("Mismatch of source balance");
                }
                if (destAcc.getBalance().compareTo(destBalanceExpected) != 0) {
                    throw new ProcessingMismatch("Mismatch of dest balance");
                }
                if (!destAcc.getInTransfersCount().equals(destInTransfersCounter)) {
                    throw new ProcessingMismatch("Mismatch of transfers counter");
                }

                //match in balances of source, target accounts and incoming transfers count
                updateTransferStatus(ctx, transfer.getId(), TransferStatus.SUCCESS);

                log.info("Transfer successfully processed {}", transfer.getId());

                return ProcessingResult.SUCCESS;
            } catch (Exception e) {
                log.info("Transfer {} will be returned to status {} due to {}",
                        transfer.getId(),
                        TransferStatus.PROCESSING,
                        e.getMessage()
                );
            }

            return ProcessingResult.RETRY;
        });
    }

    @NotNull
    private AccountsRecord getAccountsRecordFromList(Result<AccountsRecord> accounts, int accountId) {
        return accounts.stream()
            .filter(record -> record.getId().equals(accountId))
            .reduce((a, b) -> {
                throw new RuntimeException("Multiple elements after filtering by accountId = " + accountId);
            })
            .orElseGet(() -> {throw new RuntimeException("Account [id='"+accountId+"'] not found");})
        ;
    }

    private void updateTransferStatus(DSLContext ctx, UUID transferUUID, TransferStatus status) {
        ctx
            .update(TRANSFERS)
            .set(TRANSFERS.STATUS, status.getStatus())
            .set(TRANSFERS.UPDATED_ON, new Timestamp(System.currentTimeMillis()))
            .where(TRANSFERS.ID.eq(transferUUID))
            .execute();
    }

    private void updateAccountBalance(DSLContext ctx, int accountId, BigDecimal balance) {
        ctx
            .update(ACCOUNTS)
            .set(ACCOUNTS.BALANCE, balance)
            .where(ACCOUNTS.ID.eq(accountId))
            .execute();
    }

    private void updateAccountBalance(DSLContext ctx, int accountId, BigDecimal balance, int transactionCount) {
        ctx
                .update(ACCOUNTS)
                .set(ACCOUNTS.BALANCE, balance)
                .set(ACCOUNTS.IN_TRANSFERS_COUNT, transactionCount)
                .where(ACCOUNTS.ID.eq(accountId))
                .execute();
    }

    public enum ProcessingResult {
        SUCCESS(0),
        DISCARD_NO_FUNDS(1),
        RETRY(2);

        private int status;

        ProcessingResult(int status) {
            this.status = status;
        }
    }
}
