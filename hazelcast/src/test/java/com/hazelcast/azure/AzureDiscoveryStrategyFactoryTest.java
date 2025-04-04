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

package com.hazelcast.azure;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.impl.DefaultDiscoveryService;
import com.hazelcast.spi.discovery.integration.DiscoveryServiceSettings;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AzureDiscoveryStrategyFactoryTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Test
    public void validConfiguration() {
        DiscoveryStrategy strategy = createStrategy("azure/test-azure-config.xml");
        assertTrue(strategy instanceof AzureDiscoveryStrategy);
    }

    @Test(expected = RuntimeException.class)
    public void invalidConfiguration() {
        createStrategy("azure/test-azure-config-invalid.xml");
    }

    private static DiscoveryStrategy createStrategy(String xmlFileName) {
        final InputStream xmlResource = AzureDiscoveryStrategyFactoryTest.class.getClassLoader().getResourceAsStream(xmlFileName);
        Config config = new XmlConfigBuilder(xmlResource).build();

        DiscoveryConfig discoveryConfig = config.getNetworkConfig().getJoin().getDiscoveryConfig();
        DiscoveryServiceSettings settings = new DiscoveryServiceSettings().setDiscoveryConfig(discoveryConfig);
        DefaultDiscoveryService service = new DefaultDiscoveryService(settings);
        Iterator<DiscoveryStrategy> strategies = service.getDiscoveryStrategies().iterator();
        return strategies.next();
    }

    @Test
    public void isEndpointAvailable() {
        // given
        String endpoint = "/some-endpoint";
        String url = String.format("http://localhost:%d%s", wireMockRule.port(), endpoint);
        stubFor(get(urlEqualTo(endpoint)).willReturn(aResponse().withStatus(HttpURLConnection.HTTP_OK).withBody("some-body")));

        // when
        boolean isAvailable = AzureDiscoveryStrategyFactory.isEndpointAvailable(url);

        // then
        assertTrue(isAvailable);
    }

    @Test
    public void readFileContents()
            throws IOException {
        // given
        String expectedContents = "Hello, world!\nThis is a test with Unicode ✓.";
        String testFile = createTestFile(expectedContents);

        // when
        String actualContents = AzureDiscoveryStrategyFactory.readFileContents(testFile);

        // then
        assertEquals(expectedContents, actualContents);
    }

    private static String createTestFile(String expectedContents)
            throws IOException {
        File temp = File.createTempFile("test", ".tmp");
        temp.deleteOnExit();
        Files.writeString(temp.toPath(), expectedContents, StandardCharsets.UTF_8);
        return temp.getAbsolutePath();
    }
}
