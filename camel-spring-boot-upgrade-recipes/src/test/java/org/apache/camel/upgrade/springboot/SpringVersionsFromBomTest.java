/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.upgrade.springboot;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The Spring versions are read from {@code spring-boot-dependencies} at build time (see the pom).
 * Ensures they were set before the recipes got filtered.
 */
class SpringVersionsFromBomTest {

    @ParameterizedTest
    @ValueSource(strings = { "spring-framework-6-to-7.yaml", "spring-security-6-to-7.yaml", "spring-batch-5-to-6.yaml",
            "latest.yaml", "4.18LTS.yaml" })
    void springVersionsAreFiltered(String recipe) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/rewrite/" + recipe)) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(content.matches("(?s).*(\\$\\{|@)spring[a-z-]*(4\\.18-lts-)?version[}@].*"),
                    recipe + " contains an unresolved Spring version placeholder");
        }
    }
}
