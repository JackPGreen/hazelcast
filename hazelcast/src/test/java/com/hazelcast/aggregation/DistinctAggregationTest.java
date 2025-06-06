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

package com.hazelcast.aggregation;

import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.DefaultSerializationServiceBuilder;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import static com.hazelcast.aggregation.TestSamples.createEntryWithValue;
import static com.hazelcast.aggregation.TestSamples.createExtractableEntryWithValue;
import static com.hazelcast.aggregation.TestSamples.sampleStrings;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class DistinctAggregationTest {

    private final InternalSerializationService ss = new DefaultSerializationServiceBuilder().build();

    @Test(timeout = TimeoutInMillis.MINUTE)
    public void testCountAggregator() {
        List<String> values = repeatTimes(3, sampleStrings());
        Set<String> expectation = new HashSet<>(values);

        Aggregator<Entry<String, String>, Set<String>> aggregation = Aggregators.distinct();
        for (String value : values) {
            aggregation.accumulate(createEntryWithValue(value));
        }

        Aggregator<Entry<String, String>, Set<String>> resultAggregation = Aggregators.distinct();
        resultAggregation.combine(aggregation);
        Set<String> result = resultAggregation.aggregate();

        assertThat(result).isEqualTo(expectation);
    }

    @Test(timeout = TimeoutInMillis.MINUTE)
    public void testCountAggregator_withNull() {
        List<String> values = repeatTimes(3, sampleStrings());
        values.add(null);
        values.add(null);
        Set<String> expectation = new HashSet<>(values);

        Aggregator<Entry<String, String>, Set<String>> aggregation = Aggregators.distinct();
        for (String value : values) {
            aggregation.accumulate(createEntryWithValue(value));
        }

        Aggregator<Entry<String, String>, Set<String>> resultAggregation = Aggregators.distinct();
        resultAggregation.combine(aggregation);
        Set<String> result = resultAggregation.aggregate();

        assertThat(result).isEqualTo(expectation);
    }

    @Test(timeout = TimeoutInMillis.MINUTE)
    public void testCountAggregator_withAttributePath() {
        Person[] people = {new Person(5.1), new Person(3.3)};
        Double[] ages = {5.1, 3.3};
        List<Person> values = repeatTimes(3, Arrays.asList(people));
        Set<Double> expectation = Set.of(ages);

        Aggregator<Entry<Person, Person>, Set<Double>> aggregation = Aggregators.distinct("age");
        for (Person value : values) {
            aggregation.accumulate(createExtractableEntryWithValue(value, ss));
        }

        Aggregator<Entry<Person, Person>, Set<Double>> resultAggregation = Aggregators.distinct("age");
        resultAggregation.combine(aggregation);
        Set<Double> result = resultAggregation.aggregate();

        assertThat(result).isEqualTo(expectation);
    }

    @Test(timeout = TimeoutInMillis.MINUTE)
    public void testCountAggregator_withAttributePath_withNull() {
        Person[] people = {new Person(5.1), new Person(null)};
        Double[] ages = {5.1, null};
        List<Person> values = repeatTimes(3, Arrays.asList(people));
        Set<Double> expectation = new HashSet<>(Arrays.asList(ages));

        Aggregator<Entry<Person, Person>, Set<Double>> aggregation = Aggregators.distinct("age");
        for (Person value : values) {
            aggregation.accumulate(createExtractableEntryWithValue(value, ss));
        }

        Aggregator<Entry<Person, Person>, Set<Double>> resultAggregation = Aggregators.distinct("age");
        resultAggregation.combine(aggregation);
        Set<Double> result = resultAggregation.aggregate();

        assertThat(result).isEqualTo(expectation);
    }

    private <T> List<T> repeatTimes(int times, List<T> values) {
        List<T> repeatedValues = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            repeatedValues.addAll(values);
        }
        return repeatedValues;
    }
}
