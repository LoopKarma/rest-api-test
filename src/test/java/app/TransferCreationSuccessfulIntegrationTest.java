package app;

import app.config.DbContextProviderWithPool;
import app.domain.TransferStatus;
import app.dto.TransferRequest;
import org.eclipse.jetty.http.HttpStatus;
import org.jooq.Result;
import org.jooq.generated.tables.records.TransfersRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.UUID;

import static app.dto.TransferRequest.builder;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.jooq.generated.tables.Transfers.TRANSFERS;

class TransferCreationSuccessfulIntegrationTest extends TestCase  {
    private static final UUID SOURCE_ACCOUNT = UUID.fromString("fcfef11f-70ee-4e5e-b4f5-e21e5cd1081d");
    private static final UUID DEST_ACCOUNT = UUID.fromString("c4089f6b-5763-402a-88f4-a9158a1514fe");

    private static DbContextProviderWithPool contextProvider;

    @BeforeAll
    static void setup() {
        contextProvider = new DbContextProviderWithPool("schema.sql", "transferCreationSuccessful.sql");
        startApplication(contextProvider, PORT);
    }

    @AfterAll
    private static void tearDown() {
        stopApplication();
        contextProvider.getContext().execute("SHUTDOWN IMMEDIATELY");
    }

    @Test
    void testThatCorrectRequestWillCauseCreationOfANewTransfer() {
        UUID transferUUID = UUID.fromString("ba4d6e8a-156f-4e92-a79d-fdeb196033dd");
        TransferRequest transferRequest = builder()
                .sourceAccount(SOURCE_ACCOUNT)
                .destAccount(DEST_ACCOUNT)
                .transferIdentifier(transferUUID)
                .amount(new BigDecimal(10.21))
                .build();

        String expectedResponse = "{\"uuid\":\""+transferUUID+"\"}";



        assertThat(contextProvider.getContext()
                .selectFrom(TRANSFERS)
                .where(TRANSFERS.ID.eq(transferUUID))
                .fetchOne()
        ).isNull();

        given()
            .body(transferRequest)
        .when()
                .put(getTransferCreateEndpointUrl())
        .then()
            .statusCode(HttpStatus.ACCEPTED_202)
            .body(is(expectedResponse))
            .header("Content-Type", is("application/json"))
        ;

        TransfersRecord newTransfer = contextProvider.getContext()
                .selectFrom(TRANSFERS)
                .where(TRANSFERS.ID.eq(transferUUID))
                .fetchOne();

        assertThat(newTransfer).isNotNull();

        assertThat(newTransfer.getAmount())
                .isEqualTo(transferRequest.getAmount().round(new MathContext(4)));
        assertThat(newTransfer.getStatus()).isEqualTo(TransferStatus.NEW.getStatus());
    }

    @Test
    void testThatSendingSameRequestWontCreateDuplicatedTransfers() {
        UUID transferUUID = UUID.fromString("273b6d3a-22bf-452f-8c45-8682e7a502dd");
        TransferRequest transferRequest = builder()
                .sourceAccount(SOURCE_ACCOUNT)
                .destAccount(DEST_ACCOUNT)
                .transferIdentifier(transferUUID)
                .amount(new BigDecimal(10.21))
                .build();

        String expectedResponse = "{\"uuid\":\""+transferUUID+"\"}";

        assertThat(contextProvider.getContext()
                .selectFrom(TRANSFERS)
                .where(TRANSFERS.ID.eq(transferUUID))
                .fetchOne()
        ).isNull();

        for (int i = 3; i > 0; i--) {
            given()
                    .body(transferRequest)
                    .when()
                    .put(getTransferCreateEndpointUrl())
                    .then()
                    .statusCode(HttpStatus.ACCEPTED_202)
                    .body(is(expectedResponse))
                    .header("Content-Type", is("application/json"))
            ;

            Result<TransfersRecord> result = contextProvider.getContext()
                    .selectFrom(TRANSFERS)
                    .where(TRANSFERS.ID.eq(transferUUID))
                    .fetch();

            assertThat(result.size() == 1);
            TransfersRecord newTransfer = result.get(0);
            assertThat(newTransfer.getAmount())
                .isEqualTo(transferRequest.getAmount().round(new MathContext(4)));
            assertThat(newTransfer.getStatus()).isEqualTo(TransferStatus.NEW.getStatus());
        }
    }

    private String getTransferCreateEndpointUrl() {
        return getApplicationUrl("/api/v1/transfer");
    }
}
