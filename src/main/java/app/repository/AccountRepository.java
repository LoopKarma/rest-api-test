package app.repository;

import org.jooq.generated.tables.records.AccountsRecord;

import java.util.UUID;

public interface AccountRepository extends AbstractRepository<AccountsRecord> {
    AccountsRecord getAccountByUUID(UUID uuid);
}
