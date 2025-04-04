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

package com.hazelcast.jet.impl.connector;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * An adapter to adapt {@code Supplier<Connection>} to a {@link
 * javax.sql.DataSource}.
 */
public class DataSourceFromJdbcUrl implements DataSource {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DataSourceFromJdbcUrl(@Nonnull String jdbcUrl) {
        this(jdbcUrl, null, null);
    }

    public DataSourceFromJdbcUrl(@Nonnull String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.password = password;
        this.username = username;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (username != null || password != null) {
            return getConnection(username, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    @Override
    public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        throw new UnsupportedOperationException();
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
