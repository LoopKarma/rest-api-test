package app.config;

import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.impl.DSL;
import javax.sql.DataSource;

@Slf4j
public class DbContextProviderWithPool implements DatabaseContextProvider {
    //h2 and not postgres because latter requires docker on a running machine
    private final String dbHostUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;";
    private final String defaultDbConfig = "INIT=runscript from 'classpath:schema.sql'\\;runscript from 'classpath:init.sql'";
    private String customDbConfig;

    private DataSource dataSource;
    private DSLContext context;

    public DbContextProviderWithPool(String... scriptsToRunOnDbStart) {
        customDbConfig = "INIT=";
        for (String script: scriptsToRunOnDbStart) {
            customDbConfig += "runscript from 'classpath:" + script + "'\\;";
        }
        dataSource = createDataSourceWithConnectionPool();
    }

    public DbContextProviderWithPool() {
        dataSource = createDataSourceWithConnectionPool();
    }

    @Override
    public boolean isHealthy() {
        return getContext().meta()
                .getCatalog("TEST")
                .getSchema("PUBLIC")
                .getTables()
                .size() == 2;
    }

    @Override
    public DSLContext getContext() {
        if (context != null) {
            return context;
        }
        context = DSL.using(dataSource, SQLDialect.HSQLDB);
        return context;
    }

    private String getDbHostUrl() {
        if (customDbConfig == null) {
            return dbHostUrl + defaultDbConfig;
        }
        return dbHostUrl + customDbConfig;
    }

    private DataSource createDataSourceWithConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getDbHostUrl());
        config.setConnectionTimeout(3000); //ms
        config.setIdleTimeout(60000); //ms
        config.setMaxLifetime(600000);//ms
        config.setAutoCommit(false);
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(10);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }
}
