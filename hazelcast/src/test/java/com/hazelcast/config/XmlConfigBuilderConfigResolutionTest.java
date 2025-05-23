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

package com.hazelcast.config;

import com.hazelcast.config.helpers.DeclarativeConfigFileHelper;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.File;

import static com.hazelcast.internal.config.DeclarativeConfigUtil.SYSPROP_MEMBER_CONFIG;
import static com.hazelcast.internal.config.DeclarativeConfigUtil.XML_ACCEPTED_SUFFIXES_STRING;
import static org.junit.Assert.assertEquals;

@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class XmlConfigBuilderConfigResolutionTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final DeclarativeConfigFileHelper helper = new DeclarativeConfigFileHelper();

    @Before
    @After
    public void beforeAndAfter() {
        System.clearProperty(SYSPROP_MEMBER_CONFIG);
        helper.ensureTestConfigDeleted();
    }

    @Test
    public void testResolveSystemProperty_file_xml() throws Exception {
        helper.givenXmlConfigFileInWorkDir("foo.xml", "cluster-xml-file");
        System.setProperty(SYSPROP_MEMBER_CONFIG, "foo.xml");

        Config config = new XmlConfigBuilder().build();
        assertEquals("cluster-xml-file", config.getInstanceName());
    }

    @Test
    public void testResolveSystemProperty_classpath_xml() throws Exception {
        helper.givenXmlConfigFileOnClasspath("foo.xml", "cluster-xml-classpath");
        System.setProperty(SYSPROP_MEMBER_CONFIG, "classpath:foo.xml");

        Config config = new XmlConfigBuilder().build();
        assertEquals("cluster-xml-classpath", config.getInstanceName());
    }

    @Test
    public void testResolveSystemProperty_classpath_nonExistentXml_throws() {
        System.setProperty(SYSPROP_MEMBER_CONFIG, "classpath:idontexist.xml");

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage("classpath");
        expectedException.expectMessage("idontexist.xml");

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_file_nonExistentXml_throws() {
        System.setProperty(SYSPROP_MEMBER_CONFIG, "idontexist.xml");

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage("idontexist.xml");

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_file_nonXml_throws() throws Exception {
        File file = helper.givenXmlConfigFileInWorkDir("foo.yaml", "irrelevant");
        System.setProperty(SYSPROP_MEMBER_CONFIG, file.getAbsolutePath());

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage(SYSPROP_MEMBER_CONFIG);
        expectedException.expectMessage("suffix");
        expectedException.expectMessage("foo.yaml");
        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_classpath_nonXml_throws() throws Exception {
        helper.givenXmlConfigFileOnClasspath("foo.yaml", "irrelevant");
        System.setProperty(SYSPROP_MEMBER_CONFIG, "classpath:foo.yaml");

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage(SYSPROP_MEMBER_CONFIG);
        expectedException.expectMessage("suffix");
        expectedException.expectMessage("foo.yaml");
        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_classpath_nonExistentNonXml_throws() {
        System.setProperty(SYSPROP_MEMBER_CONFIG, "classpath:idontexist.yaml");

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage(SYSPROP_MEMBER_CONFIG);
        expectedException.expectMessage("suffix");
        expectedException.expectMessage("idontexist.yaml");
        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_file_nonExistentNonXml_throws() {
        System.setProperty(SYSPROP_MEMBER_CONFIG, "foo.yaml");

        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);
        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage("foo.yaml");
        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_file_noSuffix_throws() throws Exception {
        File file = helper.givenXmlConfigFileInWorkDir("foo", "irrelevant");
        System.setProperty(SYSPROP_MEMBER_CONFIG, file.getAbsolutePath());

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage(SYSPROP_MEMBER_CONFIG);
        expectedException.expectMessage("suffix");
        expectedException.expectMessage("foo");
        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveSystemProperty_classpath_nosuffix_throws() throws Exception {
        helper.givenXmlConfigFileOnClasspath("foo", "irrelevant");
        System.setProperty(SYSPROP_MEMBER_CONFIG, "classpath:foo");

        expectedException.expect(HazelcastException.class);
        expectedException.expectMessage(SYSPROP_MEMBER_CONFIG);
        expectedException.expectMessage("suffix");
        expectedException.expectMessage("foo");
        expectedException.expectMessage(XML_ACCEPTED_SUFFIXES_STRING);

        new XmlConfigBuilder().build();
    }

    @Test
    public void testResolveFromWorkDir() throws Exception {
        helper.givenXmlConfigFileInWorkDir("cluster-xml-workdir");

        Config config = new XmlConfigBuilder().build();

        assertEquals("cluster-xml-workdir", config.getInstanceName());
    }

    @Test
    public void testResolveFromClasspath() throws Exception {
        helper.givenXmlConfigFileOnClasspath("cluster-xml-classpath");

        Config config = new XmlConfigBuilder().build();

        assertEquals("cluster-xml-classpath", config.getInstanceName());
    }

    @Test
    public void testResolveDefault() {
        Config config = new XmlConfigBuilder().build();
        assertEquals("dev", config.getClusterName());
    }
}
