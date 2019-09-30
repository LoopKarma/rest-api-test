package app;

import app.config.DatabaseContextProvider;
import app.config.DbContextProviderWithPool;
import app.dto.TransferRequest;
import app.dto.TransferRequestExample;
import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static app.dto.TransferRequest.builder;
import static io.restassured.RestAssured.given;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;

class TransferCreationFailedIntegrationTest extends TestCase {
    private static final UUID SOURCE_ACCOUNT_UUID = fromString("fa11d9b8-7a19-4805-a53e-d3dfdc87584c");
    private static UUID transferUUID = fromString("ede9bdd4-10ec-41b1-85fc-2831585f6352");

    private static DatabaseContextProvider contextProvider;

    @BeforeAll
    static void setup() {
        contextProvider = new DbContextProviderWithPool("schema.sql", "transferCreationUnsuccessful.sql");
        startApplication(contextProvider, PORT);
    }

    @AfterAll
    private static void tearDown() {
        stopApplication();
        contextProvider.getContext().execute("SHUTDOWN IMMEDIATELY");
    }

    @ParameterizedTest(name = "Invalid Request: {1}")
    @MethodSource("createInvalidRequests")
    void createTransactionFailedInvalidRequest(String request, String nameOfCase) {
        given()
            .body(request)
        .when()
            .put(getTransferCreateEndpointUrl())
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .header("Content-Type", is("application/json"))
        ;
    }

    @ParameterizedTest(name="Executing case with {3}")
    @MethodSource("createValidationFailsRequests")
    void transferRequestValidationFailed(TransferRequest request, String expectedBody, int expectedStatus, String displayName) {
        given()
            .body(request)
        .when()
            .put(getTransferCreateEndpointUrl())
        .then()
            .statusCode(expectedStatus)
            .header("Content-Type", is("application/json"))
            .body(is(expectedBody))
        ;
    }

    private static Stream<Arguments> createValidationFailsRequests() {
        TransferRequest invalidSrcUUID = builder()
                .sourceAccount(fromString("1dcf08c8-317c-4320-a662-7c4621c5b034"))
                .destAccount(fromString("40562d7d-eb12-4f5d-98ae-a18fb0beb72d"))
                .transferIdentifier(fromString("fadaa4ad-8a3a-49ef-8f29-5a690e498bd8"))
                .amount(new BigDecimal(20.00))
                .build();

        TransferRequest notEnoughFunds = builder()
                .sourceAccount(SOURCE_ACCOUNT_UUID)
                .destAccount(fromString("fadaa4ad-8a3a-49ef-8f29-5a690e498bd8"))
                .transferIdentifier(fromString("9bcf7d8c-4ba9-4e7b-8a04-652c247ca8ef"))
                .amount(new BigDecimal(59.00))
                .build();

        TransferRequest invalidDesTUUID = builder()
                .sourceAccount(SOURCE_ACCOUNT_UUID)
                .destAccount(fromString("aa8aa8c3-7ed9-4e9e-b573-40937031bd1a"))
                .transferIdentifier(fromString("30dd6697-cd82-471d-8bc3-cd7f6c4d7a7c"))
                .amount(new BigDecimal(20.00))
                .build();

        TransferRequest existingTransfer = builder()
                .sourceAccount(fromString("a81a58ab-30c1-489f-8fb8-7ea53c9aecad"))
                .destAccount(fromString("e357154e-61cb-417d-a704-58bfd07d21dc"))
                .transferIdentifier(transferUUID)
                .amount(new BigDecimal(10.00))
                .build();

        return Stream.of(
                Arguments.of(invalidSrcUUID, "{\"error\":\"SourceAccountNotFound\"}", HttpStatus.BAD_REQUEST_400, "SourceAccountNotFound"),
                Arguments.of(notEnoughFunds, "{\"error\":\"InsufficientFunds\"}", HttpStatus.BAD_REQUEST_400, "InsufficientFunds"),
                Arguments.of(invalidDesTUUID, "{\"error\":\"DestinationAccountNotFound\"}", HttpStatus.BAD_REQUEST_400, "DestinationAccountNotFound"),
                Arguments.of(existingTransfer, "{\"uuid\":\""+transferUUID+"\"}", HttpStatus.ACCEPTED_202, "TransferAlreadyCreated")
        );
    }

    private static Stream<Arguments> createInvalidRequests() {
        Gson gson = new Gson();
        Object oneFiled = new TransferRequestExample().setDestAccount(null).setSourceAccount(null).setTransferIdentifier(null);
        Object twoFields = new TransferRequestExample().setSourceAccount(null).setTransferIdentifier(null);
        Object threeFields = new TransferRequestExample().setSourceAccount(null);

        return Stream.of(
            Arguments.of(gson.toJson(oneFiled), "One field present only"),
            Arguments.of(gson.toJson(twoFields), "Two fields present only"),
            Arguments.of(gson.toJson(threeFields), "Three fields present only")
        );
    }

    private String getTransferCreateEndpointUrl() {
        return getApplicationUrl("/api/v1/transfer");
    }
}
