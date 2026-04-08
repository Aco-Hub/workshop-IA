package com.timesheet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Configuration
public class DatabaseCreator {

    @Value("${spring.datasource.url}")
    private String _datasourceUrl;

    @Value("${spring.datasource.username}")
    private String _username;

    @Value("${spring.datasource.password}")
    private String _password;

    @PostConstruct
    public void createDatabaseIfNotExists() {
        final String dbName = _datasourceUrl.substring(_datasourceUrl.lastIndexOf('/') + 1);
        final String baseUrl = _datasourceUrl.substring(0, _datasourceUrl.lastIndexOf('/') + 1) + "postgres";

        try (Connection connection = DriverManager.getConnection(baseUrl, _username, _password);
             Statement statement = connection.createStatement()) {

            final ResultSet resultSet = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");

            if (!resultSet.next()) {
                statement.execute("CREATE DATABASE " + dbName);
                log.info("Created database: {}", dbName);
            }
        } catch (final Exception e) {
            log.warn("Could not auto-create database '{}': {}", dbName, e.getMessage());
        }
    }
}
