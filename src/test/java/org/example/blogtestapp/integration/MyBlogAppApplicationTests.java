package org.example.blogtestapp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class MyBlogAppApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private PostgreSQLContainer postgres;

    @Test
    void contextLoads() {
    }

    @Test
    public void testRussianConfigExists() throws SQLException {
        java.sql.Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT cfgname FROM pg_ts_config WHERE cfgname = 'russian'");
        assert resultSet.next() : "Russian configuration should exist";
        connection.close();
    }

}
