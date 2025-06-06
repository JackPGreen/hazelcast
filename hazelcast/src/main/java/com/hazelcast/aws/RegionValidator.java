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

package com.hazelcast.aws;

import com.hazelcast.config.InvalidConfigurationException;

import java.util.regex.Pattern;

/**
 * Helper class used to validate AWS Region.
 */
final class RegionValidator {
    private static final Pattern AWS_REGION_PATTERN =
        Pattern.compile("aws(?:-(?:cn|us-gov|iso(?:-b)?))?-global|(?:af|ap|ca|cn|eu(?:-isoe)?|eusc-de|il|me|mx|sa|us"
                + "(?:-(?:gov|iso|isob|isof))?)-(?:north(?:east|west)?|south(?:east|west)?|east|west|central)-(?:[1-5]|7)");

    private RegionValidator() {
    }

    static void validateRegion(String region) {
        if (region == null) {
            throw new InvalidConfigurationException("The provided region is null.");
        }
        if (!AWS_REGION_PATTERN.matcher(region).matches()) {
            String message = String.format("The provided region %s is not a valid AWS region.", region);
            throw new InvalidConfigurationException(message);
        }
    }
}
