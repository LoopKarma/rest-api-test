package app.repository;

import org.jooq.generated.tables.records.TransfersRecord;
import java.util.UUID;

public interface TransferRepository extends AbstractRepository<TransfersRecord> {
    TransfersRecord getTransferByUUID(UUID uuid);
    TransfersRecord getNextTransferForExecution();
}
