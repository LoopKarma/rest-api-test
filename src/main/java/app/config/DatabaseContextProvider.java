package app.config;

import org.jooq.DSLContext;

public interface DatabaseContextProvider extends HealthCheck {
    DSLContext getContext();
}
