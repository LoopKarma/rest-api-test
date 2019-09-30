package app.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProcessingEventCtx {
    public ProcessingEventCtx(UUID transferUUID) {
        this.transferUUID = transferUUID;
    }
    private UUID transferUUID;
    private int sourceAccount;
    private int destinationAccount;
}
