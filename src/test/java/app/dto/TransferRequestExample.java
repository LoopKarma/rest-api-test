package app.dto;

import java.math.BigDecimal;
import java.util.UUID;

import static java.util.UUID.fromString;

public class TransferRequestExample {
    private BigDecimal amount = new BigDecimal(Math.random());
    private UUID sourceAccount = fromString("31e26d4d-7cae-40e6-a1c4-d94b956f2fa7");
    private UUID destAccount = fromString("3eff02b4-d12a-445e-bf02-c124873c11ea");
    private UUID transferIdentifier = fromString("524ad4f6-0f79-442a-bfa5-a8470420277a");

    public TransferRequestExample setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public  TransferRequestExample setSourceAccount(UUID sourceAccount) {
        this.sourceAccount = sourceAccount;
        return this;
    }

    public TransferRequestExample setDestAccount(UUID destAccount) {
        this.destAccount = destAccount;
        return this;
    }

    public TransferRequestExample setTransferIdentifier(UUID transferIdentifier) {
        this.transferIdentifier = transferIdentifier;
        return this;
    }

}
