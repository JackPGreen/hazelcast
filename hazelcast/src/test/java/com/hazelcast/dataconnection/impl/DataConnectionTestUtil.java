/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.dataconnection.impl;

import com.hazelcast.config.Config;
import com.hazelcast.config.DataConnectionConfig;
import com.hazelcast.dataconnection.DataConnection;
import com.hazelcast.dataconnection.DataConnectionBase;
import com.hazelcast.dataconnection.DataConnectionRegistration;
import com.hazelcast.dataconnection.DataConnectionResource;

import javax.annotation.Nonnull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public final class DataConnectionTestUtil {

    public static final String DUMMY_TYPE = "DUMMY";

    private DataConnectionTestUtil() {
    }

    public static void configureJdbcDataConnection(String name, String jdbcUrl, Config config) {
        Properties properties = new Properties();
        properties.setProperty("jdbcUrl", jdbcUrl);
        DataConnectionConfig dataConnectionConfig = new DataConnectionConfig()
                .setName(name)
                .setType("jdbc")
                .setProperties(properties);
        config.getDataConnectionConfigs().put(name, dataConnectionConfig);
    }

    public static void configureJdbcDataConnection(String name, String jdbcUrl, String username, String password, Config config) {
        Properties properties = new Properties();
        properties.setProperty("jdbcUrl", jdbcUrl);
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        DataConnectionConfig dataConnectionConfig = new DataConnectionConfig()
                .setName(name)
                .setType("jdbc")
                .setProperties(properties);
        config.getDataConnectionConfigs().put(name, dataConnectionConfig);
    }

    public static void configureMongoDataConnection(String name, String connectionString, Config config) {
        Properties properties = new Properties();
        properties.setProperty("connectionString", connectionString);
        DataConnectionConfig dataConnectionConfig = new DataConnectionConfig()
                .setName(name)
                .setType("mongo")
                .setProperties(properties);
        config.getDataConnectionConfigs().put(name, dataConnectionConfig);
    }

    public static void configureDummyDataConnection(String name, Config config) {
        DataConnectionConfig dataConnectionConfig = new DataConnectionConfig()
                .setName(name)
                .setType("dummy");
        config.getDataConnectionConfigs().put(name, dataConnectionConfig);
    }

    public static class DummyDataConnection extends DataConnectionBase {

        private volatile boolean closed;

        public DummyDataConnection(DataConnectionConfig config) {
            super(config);
        }

        @Nonnull
        @Override
        public Collection<DataConnectionResource> listResources() {
            return Arrays.asList(
                    new DataConnectionResource("testType1", "testName1"),
                    new DataConnectionResource("testType2", "testPrefix1", "testName2")
            );
        }

        @Nonnull
        @Override
        public Collection<String> resourceTypes() {
            return Arrays.asList("testType1", "testType2");
        }

        @Override
        public void destroy() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    public static class DummyDataConnectionRegistration implements DataConnectionRegistration {

        @Override
        public String type() {
            return DUMMY_TYPE;
        }

        @Override
        public Class<? extends DataConnection> clazz() {
            return DummyDataConnection.class;
        }
    }

    public static void executeJdbc(String jdbcUrl, String sql) throws SQLException {
        assertNotNull(jdbcUrl, "jdbdUrl must be set");
        assertNotNull(sql, "sql query must be provided");

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()
        ) {
            stmt.execute(sql);
        }
    }
}
