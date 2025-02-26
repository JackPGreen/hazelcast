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

package com.hazelcast.internal.util.phonehome;

import com.hazelcast.instance.impl.Node;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.impl.JetServiceBackend;


import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static com.hazelcast.internal.util.JsonUtil.toJsonObject;
import static com.hazelcast.internal.util.phonehome.PhoneHomeMetrics.JET_CONNECTOR_COUNTS;
import static com.hazelcast.internal.util.phonehome.PhoneHomeMetrics.JET_ENABLED;
import static com.hazelcast.internal.util.phonehome.PhoneHomeMetrics.JET_JOBS_SUBMITTED;
import static com.hazelcast.internal.util.phonehome.PhoneHomeMetrics.JET_RESOURCE_UPLOAD_ENABLED;

class JetMetricsProvider implements MetricsProvider {

    @Override
    public void provideMetrics(Node node, MetricsCollectionContext context) {
        JetConfig jetConfig = node.getNodeEngine().getConfig().getJetConfig();
        boolean isJetEnabled = jetConfig.isEnabled();
        context.collect(JET_ENABLED, isJetEnabled);
        context.collect(JET_RESOURCE_UPLOAD_ENABLED, jetConfig.isResourceUploadEnabled());
        if (isJetEnabled) {
            JetServiceBackend jetServiceBackend = node.getNodeEngine().getService(JetServiceBackend.SERVICE_NAME);
            long jobSubmittedCount = jetServiceBackend.getJobCoordinationService().getJobSubmittedCount();
            context.collect(JET_JOBS_SUBMITTED, jobSubmittedCount);
            // Connector phone homes
            ConcurrentMap<String, Long> metrics = jetServiceBackend.getConnectorInitializeCounts();
            if (!metrics.isEmpty()) {
                context.collect(JET_CONNECTOR_COUNTS, toJsonObject(metrics));
            }
        }
    }

    private static JsonObject toJsonObject(Map<String, Long> map) {
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, Long> property : map.entrySet()) {
            properties.add(property.getKey(), Json.value(property.getValue()));
        }
        return properties;
    }
}
