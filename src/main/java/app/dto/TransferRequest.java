package app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class TransferRequest {
    @NonNull
    private UUID sourceAccount;

    @NonNull
    private UUID destAccount;

    //BigDecimal in case of hyperinflation
    @NonNull
    private BigDecimal amount;

    //unique for any transfer
    @NonNull
    private UUID transferIdentifier;

    public boolean isEmpty() {
        return sourceAccount == null ||
                destAccount == null  ||
                amount == null       ||
                transferIdentifier == null;
    }
}
