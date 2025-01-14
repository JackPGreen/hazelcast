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

package com.hazelcast.internal.jmx;

import com.hazelcast.config.Config;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.topic.ITopic;

/**
 * Management bean for {@link ITopic}
 */
@ManagedDescription("ITopic")
public class TopicMBean extends HazelcastMBean<ITopic> {

    protected TopicMBean(ITopic managedObject, ManagementService service) {
        super(managedObject, service);
        this.objectName = service.createObjectName("ITopic", managedObject.getName());
    }

    @ManagedAnnotation("localCreationTime")
    @ManagedDescription("the creation time of this topic on this member")
    public long getLocalCreationTime() {
        return managedObject.getLocalTopicStats().getCreationTime();
    }

    @ManagedAnnotation("localPublishOperationCount")
    @ManagedDescription(" the total number of published messages of this topic on this member")
    public long getLocalPublishOperationCount() {
        return managedObject.getLocalTopicStats().getPublishOperationCount();
    }

    @ManagedAnnotation("localReceiveOperationCount")
    @ManagedDescription("the total number of received messages of this topic on this member")
    public long getLocalReceiveOperationCount() {
        return managedObject.getLocalTopicStats().getReceiveOperationCount();
    }

    @ManagedAnnotation("name")
    @ManagedDescription("Name of the DistributedObject")
    public String getName() {
        return managedObject.getName();
    }

    @ManagedAnnotation("config")
    public String getConfig() {
        Config config = service.instance.getConfig();
        TopicConfig topicConfig = config.findTopicConfig(managedObject.getName());
        return topicConfig.toString();
    }
}
