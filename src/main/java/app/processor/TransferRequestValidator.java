package app.processor;

import app.dto.TransferRequest;
import app.dto.TransferRequestValidationResult;
import app.exception.DestinationAccountNotFound;
import app.exception.InsufficientFunds;
import app.exception.SourceAccountNotFound;
import app.exception.TransferAlreadyCreated;
import app.repository.AccountRepository;
import app.repository.TransferRepository;
import org.jooq.generated.tables.records.AccountsRecord;
import org.jooq.generated.tables.records.TransfersRecord;

import java.math.BigDecimal;
import java.util.Optional;

public class TransferRequestValidator {
    AccountRepository accountRepository;
    private TransferRepository transferRepository;

    public TransferRequestValidator(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    public TransferRequestValidationResult validateRequest(TransferRequest request) {
        Optional<TransfersRecord> transfer = Optional.ofNullable(transferRepository.getTransferByUUID(request.getTransferIdentifier()));
        if (transfer.isPresent()) {
            return new TransferRequestValidationResult(new TransferAlreadyCreated());
        }
        Optional<AccountsRecord> sourceAccount = Optional.ofNullable(accountRepository.getAccountByUUID(request.getSourceAccount()));
        if (sourceAccount.isEmpty()) {
            return new TransferRequestValidationResult(new SourceAccountNotFound());
        }
        if (sourceAccount.get().getBalance().subtract(request.getAmount()).compareTo(BigDecimal.ZERO) < 0) {
            return new TransferRequestValidationResult(new InsufficientFunds());
        }
        Optional<AccountsRecord> destAccount = Optional.ofNullable(accountRepository.getAccountByUUID(request.getDestAccount()));
        if (destAccount.isEmpty()) {
            return new TransferRequestValidationResult(new DestinationAccountNotFound());
        }

        return new TransferRequestValidationResult(true);
    }
}
