package app;

import static spark.Spark.*;

import app.config.DatabaseContextProvider;
import app.config.DbContextProviderWithPool;
import app.config.Configuration;
import lombok.extern.slf4j.Slf4j;
import spark.Spark;

@Slf4j
public class Application {
    final static private int defaultPort = 8088;
    private DatabaseContextProvider contextProvider;
    private Configuration configuration;

    public static void main(String[] args) {
        DatabaseContextProvider contextProvider = new DbContextProviderWithPool();
        new Application(contextProvider, defaultPort);
    }

    public Application(DatabaseContextProvider context, int port) {
//        Service service = Service.ignite();
//        service.port(port);
        contextProvider = context;
        port(port);
        log.info("Application is listening on port " + port);
        initApplication();
        log.info("Up and running");
    }

    public void shutdown() {
        configuration.shutdown();
        Spark.stop();
        Spark.awaitStop();
    }

    private void initApplication() {
        log.info("Defining routes..");
        configuration = Configuration.defineRoutesAndDependencies(contextProvider);
    }
}
