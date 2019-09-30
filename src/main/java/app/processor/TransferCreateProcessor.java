package app.processor;

import app.domain.TransferStatus;
import app.dto.TransferRequest;
import app.repository.AccountRepository;
import app.repository.TransferRepository;
import org.jooq.generated.tables.records.AccountsRecord;
import org.jooq.generated.tables.records.TransfersRecord;

import java.sql.Timestamp;

public class TransferCreateProcessor {
    TransferRepository transferRepository;
    AccountRepository accountRepository;

    public TransferCreateProcessor(TransferRepository transferRepository, AccountRepository accountRepository) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
    }

    public void createTransfer(TransferRequest transferRequest) {
        AccountsRecord srcAcc = accountRepository.getAccountByUUID(transferRequest.getSourceAccount());
        AccountsRecord destAcc = accountRepository.getAccountByUUID(transferRequest.getDestAccount());
        TransfersRecord transfer = new TransfersRecord(
                transferRequest.getTransferIdentifier(),
                srcAcc.getId(),
                destAcc.getId(),
                transferRequest.getAmount(),
                TransferStatus.NEW.getStatus(),
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis())
        );

        transferRepository.save(transfer);
    }
}
