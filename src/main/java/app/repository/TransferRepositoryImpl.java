package app.repository;

import app.config.DatabaseContextProvider;
import app.domain.TransferStatus;
import org.jooq.generated.tables.Transfers;
import org.jooq.generated.tables.records.TransfersRecord;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.jooq.generated.tables.Transfers.TRANSFERS;

public class TransferRepositoryImpl implements TransferRepository {
    DatabaseContextProvider contextProvider;

    public TransferRepositoryImpl(DatabaseContextProvider databaseContextProvider) {
        this.contextProvider = databaseContextProvider;
    }

    @Override
    public TransfersRecord getTransferByUUID(UUID uuid) {
        return contextProvider.getContext()
                .selectFrom(Transfers.TRANSFERS)
                .where(Transfers.TRANSFERS.ID.eq(uuid))
                .fetchOne();
    }

    @Override
    public TransfersRecord getNextTransferForExecution() {
        return contextProvider.getContext()
                .selectFrom(Transfers.TRANSFERS)
                .where(TRANSFERS.STATUS.eq(TransferStatus.NEW.getStatus()))
                .orderBy(TRANSFERS.CREATED_ON.asc())
                .fetchOne();
    }

    @Override
    public <S extends TransfersRecord> S save(S entity) {
        contextProvider.getContext().transaction(ctx -> {
            DSL.using(ctx)
                    .insertInto(TRANSFERS, entity.fields())
                    .values(
                            entity.getId(),
                            entity.getSourceAccount(),
                            entity.getDestinationAccount(),
                            entity.getAmount(),
                            entity.getStatus(),
                            entity.getCreatedOn(),
                            entity.getUpdatedOn()
                    )
                    .execute();
        });

        return entity;
    }
}
