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

package com.hazelcast.internal.namespace.reliabletopic;

import com.hazelcast.config.ListenerConfig;
import com.hazelcast.internal.namespace.topic.TopicUCDTest;
import org.junit.runners.Parameterized;

public class ReliableTopicMessageListenerUCDTest extends ReliableTopicUCDTest {
    @Override
    public void test() throws Exception {
        topic.publish(Byte.MIN_VALUE);

        assertListenerFired("onMessage");
    }

    @Override
    protected void addClassInstanceToConfig() throws ReflectiveOperationException {
        ListenerConfig listenerConfig = new ListenerConfig();
        listenerConfig.setImplementation(getClassInstance());

        reliableTopicConfig.addMessageListenerConfig(listenerConfig);
    }

    @Override
    protected void addClassNameToConfig() {
        ListenerConfig listenerConfig = new ListenerConfig();
        listenerConfig.setClassName(getUserDefinedClassName());

        reliableTopicConfig.addMessageListenerConfig(listenerConfig);
    }

    @Override
    protected void addClassInstanceToDataStructure() throws ReflectiveOperationException {
        topic.addMessageListener(getClassInstance());
    }

    @Parameterized.Parameters(name = "Connection: {0}, Config: {1}, Class Registration: {2}, Assertion: {3}")
    public static Iterable<Object[]> parameters() {
        return listenerParameters();
    }

    @Override
    protected String getUserDefinedClassName() {
        return "usercodedeployment.MyMessageListener";
    }
}
