package app.controller;

import app.config.DatabaseContextProvider;
import spark.Route;

public class DefaultController {
    private DatabaseContextProvider contextProvider;

    public DefaultController(DatabaseContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public Route getInfo() {
        return (request, response) -> new InfoStructure();
    }

    public Route healthCheck() {
        return (request, response) -> new HealthCheckStructure(contextProvider.isHealthy());
    }

    private static class InfoStructure {
        private String apiVersion = "0.1";
    }

    private class HealthCheckStructure {
        public HealthCheckStructure(Boolean isHealthy) {
            this.isHealthy = isHealthy;
        }
        private Boolean isHealthy;
    }
}
