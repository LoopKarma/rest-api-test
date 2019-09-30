package app;

import app.config.DatabaseContextProvider;
import org.jetbrains.annotations.NotNull;
import spark.Spark;

class TestCase {
    static final int PORT = 9999;
    private static Application application;

    @NotNull
    String getApplicationUri() {
        return "http://localhost:" + PORT;
    }

    String getApplicationUrl(String path) {
        return getApplicationUri() + path;
    }

    static void startApplication(DatabaseContextProvider contextProvider, int port) {
        application = new Application(contextProvider, port);
        Spark.awaitInitialization();
    }

    static void stopApplication() {
        application.shutdown();
    }
}
