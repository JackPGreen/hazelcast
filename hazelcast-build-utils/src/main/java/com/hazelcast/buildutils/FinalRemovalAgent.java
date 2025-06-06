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

package com.hazelcast.buildutils;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.ModifierAdjustment;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.jar.asm.Opcodes;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

public final class FinalRemovalAgent {

    // Class name -> List<final method names> to be processed for
    // removing final modifier
    private static final Map<String, Set<String>> FINAL_METHODS;

    private FinalRemovalAgent() {
    }

    static {
        Map<String, Set<String>> finalMethods = new HashMap<>();
        finalMethods.put("com.hazelcast.cp.internal.session.AbstractProxySessionManager",
                Set.of("getSession", "getSessionAcquireCount"));
        finalMethods.put("com.hazelcast.spi.impl.AbstractInvocationFuture",
                Set.of("get", "join"));
        finalMethods.put("com.hazelcast.cp.internal.datastructures.spi.blocking.AbstractBlockingService",
                Set.of("getRegistryOrNull"));
        finalMethods.put("com.hazelcast.cp.internal.datastructures.spi.blocking.ResourceRegistry",
                Set.of("getWaitTimeouts"));
        FINAL_METHODS = finalMethods;
    }

    /**
     * When running a compatibility test, all com.hazelcast.* classes are transformed so that none are
     * loaded with final modifier to allow subclass proxying.
     * We configure the agent with REDEFINE type strategy and NoOp initialization strategy.
     * This allows for the redefinition of the type (instead of default REBASE strategy) without
     * adding any methods (which result in modifying Serializable classes' serialVersionUid). For
     * more details see {@link net.bytebuddy.ByteBuddy#rebase(Class)} vs
     * {@link net.bytebuddy.ByteBuddy#redefine(Class)}.
     */
    public static void premain(String argument, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(nameStartsWith("com.hazelcast"))
                .transform((builder, typeDescription, classLoader, module, domain) -> {
                    builder = manifestMethodAsPlain(builder, typeDescription);
                    int actualModifiers = typeDescription.getActualModifiers(false);
                    // unset final modifier
                    int nonFinalModifiers = actualModifiers & ~Opcodes.ACC_FINAL;
                    if (actualModifiers != nonFinalModifiers) {
                        return builder.modifiers(nonFinalModifiers);
                    } else {
                        return builder;
                    }
                }).installOn(instrumentation);
    }

    /**
     * This method assigns the {@link MethodManifestation.PLAIN} to each method of the
     * given {@link TypeDescription} that is listed in {@link #FINAL_METHODS}.
     */
    private static DynamicType.Builder manifestMethodAsPlain(DynamicType.Builder builder,
                                                             TypeDescription typeDescription) {
        final String typeName = typeDescription.getName();
        if (FINAL_METHODS.containsKey(typeName)) {
            for (String methodName : FINAL_METHODS.get(typeName)) {
                builder = builder.visit(new ModifierAdjustment()
                        .withMethodModifiers(named(methodName), MethodManifestation.PLAIN)
                );
            }
        }
        return builder;
    }
}
