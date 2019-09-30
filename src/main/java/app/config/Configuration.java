package app.config;

import app.controller.TransferController;
import app.controller.DefaultController;
import app.processor.TransferCreateProcessor;
import app.processor.TransferExecutor;
import app.processor.TransferRequestValidator;
import app.repository.AccountRepository;
import app.repository.AccountRepositoryImpl;
import app.repository.TransferRepository;
import app.repository.TransferRepositoryImpl;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;

import static spark.Spark.*;

@Slf4j
public class Configuration {
    private DatabaseContextProvider contextProvider;
    private TransferExecutor transferExecutor;

    public static Configuration defineRoutesAndDependencies(DatabaseContextProvider contextProvider) {
        Configuration configuration = new Configuration(contextProvider);
        configuration.initialize();
        return configuration;
    }

    Configuration(DatabaseContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public void shutdown() {
        if (transferExecutor.t.isAlive()) {
            transferExecutor.terminate();
        }
    }

    private void initialize() {
        TransferRepository transferRepository = new TransferRepositoryImpl(contextProvider);
        AccountRepository accountRepository = new AccountRepositoryImpl(contextProvider);
        TransferCreateProcessor transferCreateProcessor = new TransferCreateProcessor(transferRepository, accountRepository);
        TransferRequestValidator transferRequestValidator = new TransferRequestValidator(accountRepository, transferRepository);

        DefaultController defaultController = new DefaultController(contextProvider);
        TransferController transferController = new TransferController(transferRequestValidator, transferCreateProcessor);

        setUpFilters();
        defineRoutes(defaultController, transferController);

        //here should be some mature thread pool
        transferExecutor = new TransferExecutor(transferRepository, contextProvider);
        transferExecutor.t.start();
    }

    private void defineRoutes(DefaultController defaultController, TransferController transferController) {
        put("/api/v1/transfer", transferController.createTransfer(), new Gson()::toJson);
        get("/info", defaultController.getInfo(), new Gson()::toJson);
        get("/healthcheck", defaultController.healthCheck(), new Gson()::toJson);
        notFound((req, res) -> {
            res.status(HttpStatus.NOT_FOUND_404);
            return "{}";
        });
    }

    private void setUpFilters() {
        before((q, a) -> log.debug("Call to :" + q.uri()));
        after((request, response) -> response.type("application/json"));
        after((request, response) -> response.header("Content-Encoding","gzip"));
    }
}
