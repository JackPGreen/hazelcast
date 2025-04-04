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

package com.hazelcast.query.impl.extractor.predicates;

import java.io.Serializable;
import java.util.Objects;

/**
 * Data structure used in the tests of extraction in single-value attributes (not in collections).
 */
public final class SingleValueDataStructure {

    private SingleValueDataStructure() {
    }

    public static class Person implements Serializable {

        Brain brain;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person other)) {
                return false;
            }
            return Objects.equals(this.brain, other.brain);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(brain);
        }
    }

    public static class Brain implements Serializable {

        Integer iq;
        String name;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Brain other)) {
                return false;
            }
            return Objects.equals(this.iq, other.iq);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(iq);
        }
    }

    public static Person person(Integer iq) {
        Brain brain = new Brain();
        brain.iq = iq;
        brain.name = "brain" + iq;
        Person person = new Person();
        person.brain = brain;
        return person;
    }
}
