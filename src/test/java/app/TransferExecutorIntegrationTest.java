package app;

import app.config.DatabaseContextProvider;
import app.config.DbContextProviderWithPool;
import app.domain.TransferStatus;
import org.jooq.generated.tables.records.AccountsRecord;
import org.jooq.generated.tables.records.TransfersRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.UUID;

import static org.jooq.generated.tables.Accounts.ACCOUNTS;
import static org.jooq.generated.tables.Transfers.TRANSFERS;

class TransferExecutorIntegrationTest extends TestCase {
    private DatabaseContextProvider contextProvider;

    @AfterEach
    private void afterEach() {
        stopApplication();
        contextProvider.getContext().execute("SHUTDOWN IMMEDIATELY");

    }

    @Test
    void transferInStatusNewWillBeMovedToSuccess() {
        UUID transferUuid = UUID.fromString("3c8bd761-2d7d-4e60-a9f1-cb8d455597f7");
        double sourceAccountBalance = 5754.09;
        double destAccountBalance = 90.61;

        contextProvider = new DbContextProviderWithPool("schema.sql", "transferCreationSuccessful.sql");
        TransfersRecord transfersBefore = getTransfer(transferUuid);
        AccountsRecord sourceBefore = getAccountsRecord(transfersBefore.getSourceAccount());
        AccountsRecord destBefore = getAccountsRecord(transfersBefore.getDestinationAccount());

        assertThat(sourceBefore.getBalance()).isEqualTo(BigDecimal.valueOf(sourceAccountBalance));
        assertThat(destBefore.getBalance()).isEqualTo(BigDecimal.valueOf(destAccountBalance));


        startApplication(contextProvider, PORT);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TransfersRecord transfersAfter = getTransfer(transferUuid);
        AccountsRecord sourceAfter = getAccountsRecord(transfersAfter.getSourceAccount());
        AccountsRecord destAfter = getAccountsRecord(transfersAfter.getDestinationAccount());

        assertThat(transfersAfter.getStatus()).isEqualTo(TransferStatus.SUCCESS.getStatus());
        assertThat(transfersAfter.getUpdatedOn()).isAfter(transfersAfter.getCreatedOn());

        assertThat(sourceAfter.getBalance()).isEqualTo(
                new BigDecimal(sourceAccountBalance).subtract(transfersBefore.getAmount()).round(new MathContext(6))
        );
        assertThat(destAfter.getBalance()).isEqualTo(
                new BigDecimal(destAccountBalance).add(transfersBefore.getAmount()).round(new MathContext(5))
        );
    }

    @Test
    void invalidTransferWillBeMarkedAsDiscard()
    {
        UUID invalidTransferUuid = UUID.fromString("7c3bdf7d-8d1d-4aa3-b737-47f53f3b995a");
        double sourceAccountBalance = 4.09;
        double destAccountBalance = 96.45;
        contextProvider = new DbContextProviderWithPool(
                "schema.sql",
                "invalidTransferCreated.sql"
        );

        TransfersRecord transfersBefore = getTransfer(invalidTransferUuid);
        AccountsRecord sourceBefore = getAccountsRecord(transfersBefore.getSourceAccount());
        AccountsRecord destBefore = getAccountsRecord(transfersBefore.getDestinationAccount());

        assertThat(sourceBefore.getBalance()).isEqualTo(BigDecimal.valueOf(sourceAccountBalance));
        assertThat(destBefore.getBalance()).isEqualTo(BigDecimal.valueOf(destAccountBalance));

        startApplication(contextProvider, PORT);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TransfersRecord transfersAfter = getTransfer(invalidTransferUuid);
        AccountsRecord sourceAfter = getAccountsRecord(transfersAfter.getSourceAccount());
        AccountsRecord destAfter = getAccountsRecord(transfersAfter.getDestinationAccount());

        assertThat(transfersAfter.getStatus()).isEqualTo(TransferStatus.DISCARD.getStatus());
        assertThat(transfersAfter.getUpdatedOn()).isAfter(transfersAfter.getCreatedOn());

        assertThat(sourceAfter.getBalance()).isEqualTo(BigDecimal.valueOf(sourceAccountBalance));
        assertThat(destAfter.getBalance()).isEqualTo(BigDecimal.valueOf(destAccountBalance));
    }

    private TransfersRecord getTransfer(UUID transferUUID) {
        return contextProvider.getContext()
                .selectFrom(TRANSFERS)
                .where(TRANSFERS.ID.eq(transferUUID))
                .fetchOne();
    }

    private AccountsRecord getAccountsRecord(int accountId) {
        return contextProvider.getContext()
                    .selectFrom(ACCOUNTS)
                    .where(ACCOUNTS.ID.eq(accountId))
                    .fetchOne();
    }
}
