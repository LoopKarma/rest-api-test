package app;

import app.config.DbContextProviderWithPool;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static io.restassured.RestAssured.given;

class ApplicationIntegrationTest extends TestCase {
    private static DbContextProviderWithPool contextProvider;

    @BeforeAll
    static void setup() {
        contextProvider = new DbContextProviderWithPool("schema.sql");
        startApplication(contextProvider, PORT);
    }

    @AfterAll
    private static void tearDown() {
        stopApplication();
        contextProvider.getContext().execute("SHUTDOWN IMMEDIATELY");
    }

    @Test
    void testNotExistingEndpoint() {
        given()
        .when()
            .get(getApplicationUri())
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .header("Content-Type", is("application/json"))
            .body(is("{}"))
        ;
    }

    @Test
    void testInfoEndpoint() {
        given()
        .when()
            .get(getApplicationUrl("/info"))
        .then()
            .statusCode(HttpStatus.OK_200)
            .header("Content-Type", is("application/json"))
            .body(containsString("apiVersion"))
        ;
    }

    @Test
    void testHealthCheck() {
        given()
        .when()
            .get(getApplicationUrl("/healthcheck"))
        .then()
            .statusCode(HttpStatus.OK_200)
            .header("Content-Type", is("application/json"))
            .body(is("{\"isHealthy\":true}"))
        ;
    }
}
