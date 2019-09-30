package app.repository;

import app.config.DatabaseContextProvider;
import org.jooq.generated.tables.Accounts;
import org.jooq.generated.tables.records.AccountsRecord;

import java.util.UUID;

public class AccountRepositoryImpl implements AccountRepository {
    DatabaseContextProvider databaseContextProvider;

    public AccountRepositoryImpl(DatabaseContextProvider databaseContextProvider) {
        this.databaseContextProvider = databaseContextProvider;
    }

    @Override
    public AccountsRecord getAccountByUUID(UUID uuid) {
        return databaseContextProvider.getContext()
                .selectFrom(Accounts.ACCOUNTS)
                .where(Accounts.ACCOUNTS.PUBLIC_IDENTIFIER.eq(uuid))
                .fetchOne();
    }

    @Override
    public <S extends AccountsRecord> S save(S entity) {
        throw new RuntimeException("Not implemented");
    }
}
