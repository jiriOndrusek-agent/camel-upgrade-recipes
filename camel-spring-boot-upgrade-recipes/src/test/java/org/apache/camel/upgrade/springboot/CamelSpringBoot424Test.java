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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Each test activates its own narrow camel424 sub-recipe, as in CamelSpringBoot423Test.
 */
class CamelSpringBoot424Test implements RewriteTest {

    private static RecipeSpec withRecipe(RecipeSpec spec, String... recipeNames) {
        return spec.recipe(Environment.builder()
                .scanYamlResources()
                .build()
                .activateRecipes(recipeNames));
    }

    @DocumentExample
    @Test
    void removeOgnlStarter() {
        //language=xml
        rewriteRun(
                spec -> withRecipe(spec, "org.apache.camel.upgrade.camel424.removedComponentStarters"),
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.camel.springboot</groupId>
                                    <artifactId>camel-ognl-starter</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                                <dependency>
                                    <groupId>org.apache.camel.springboot</groupId>
                                    <artifactId>camel-spring-boot-starter</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """,
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.camel.springboot</groupId>
                                    <artifactId>camel-spring-boot-starter</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """
                )
        );
    }

    @Test
    void camel424ChainRemovesOgnlStarterAndDependency() {
        //language=xml
        rewriteRun(
                spec -> withRecipe(spec, "org.apache.camel.upgrade.camel424.CamelSpringBootMigrationRecipe"),
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <maven.compiler.release>21</maven.compiler.release>
                            </properties>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.camel.springboot</groupId>
                                    <artifactId>camel-ognl-starter</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                                <dependency>
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-ognl</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """,
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <maven.compiler.release>21</maven.compiler.release>
                            </properties>
                        </project>
                        """
                )
        );
    }
}
