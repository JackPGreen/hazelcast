/*
 * Copyright (c) 2008-2023, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.config;

import com.hazelcast.internal.config.ServicesConfig;
import com.hazelcast.jet.config.ResourceConfig;
import com.hazelcast.spi.annotation.PrivateApi;

import java.util.Collection;
import java.util.Map;

/**
 * Private API for accessing configuration at runtime
 *
 * @since 3.12
 */
@PrivateApi
public final class ConfigAccessor {

    private ConfigAccessor() {
    }

    public static NetworkConfig getActiveMemberNetworkConfig(Config config) {
        if (config.getAdvancedNetworkConfig().isEnabled()) {
            return new AdvancedNetworkConfig.MemberNetworkingView(config.getAdvancedNetworkConfig());
        }

        return config.getNetworkConfig();
    }

    public static ServicesConfig getServicesConfig(Config config) {
        return config.getServicesConfig();
    }

    public static boolean isInstanceTrackingEnabledSet(Config config) {
        return config.getInstanceTrackingConfig().isEnabledSet;
    }

    public static Map<String, NamespaceConfig> getNamespaceConfigs(Config config) {
       return config.getNamespacesConfig().getNamespaceConfigs();
    }

    public static void setNamespaceConfigs(Config config, Map<String, NamespaceConfig> namespaceConfigs) {
       config.getNamespacesConfig().setNamespaceConfigs(namespaceConfigs);
    }

    public static Collection<ResourceConfig> getResourceConfigs(NamespaceConfig nsConfig) {
        return nsConfig.getResourceConfigs();
    }
}
