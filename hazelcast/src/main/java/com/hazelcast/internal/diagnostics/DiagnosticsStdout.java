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

package com.hazelcast.internal.diagnostics;

import com.hazelcast.logging.ILogger;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Represents the DiagnosticsStdout.
 * <p>
 * Should only be called from the {@link Diagnostics}.
 */
final class DiagnosticsStdout implements DiagnosticsLog {
    private final Diagnostics diagnostics;
    private final ILogger logger;
    private final DiagnosticsLogWriterImpl logWriter;
    private final PrintWriter printWriter;
    private boolean staticPluginsRendered;

    DiagnosticsStdout(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
        this.logger = diagnostics.logger;
        this.logWriter = new DiagnosticsLogWriterImpl(diagnostics.isIncludeEpochTime(), diagnostics.logger);
        this.printWriter = newWriter();
        logWriter.init(printWriter);
        logger.info("Sending diagnostics logs to the stdout");
    }

    @Override
    public void write(DiagnosticsPlugin plugin) {
        try {
            if (!staticPluginsRendered) {
                renderStaticPlugins();
                staticPluginsRendered = true;
            }

            renderPlugin(plugin);
            printWriter.flush();
        } catch (RuntimeException e) {
            logger.warning("Failed to write stdout: ", e);
        }
    }

    @Override
    public void close() {
        printWriter.flush();
    }

    private void renderStaticPlugins() {
        for (DiagnosticsPlugin plugin : diagnostics.staticTasks.get()) {
            renderPlugin(plugin);
        }
    }

    private void renderPlugin(DiagnosticsPlugin plugin) {
        logWriter.resetSectionLevel();
        plugin.run(logWriter);
    }

    private PrintWriter newWriter() {
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, encoder), Short.MAX_VALUE));
    }
}
