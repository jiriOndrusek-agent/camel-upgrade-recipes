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
package org.apache.camel.upgrade;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;

/**
 * Tests for migrating from Camel 4.22.2 to 4.22.3. The openstack Swift header rename (CAMEL-25895) is the 4.18.6
 * recipe, covered by {@link CamelUpdate418_6Test}; this class guards that the 4.22.3 chain references it.
 */
public class CamelUpdate422_3Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_22_3, true)
                .parser(CamelTestUtil.parserFromClasspath(CamelTestUtil.CamelVersion.v4_22, "camel-api", "camel-core-model", "camel-support", "camel-endpointdsl"))
                .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void camel422_3ChainIncludesCamel418_6OpenstackRecipe() {
        //language=java
        rewriteRun(
                mavenProject("test-openstack",
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_22_2),
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container").setHeader("X-Container-Read", constant(".r:*")).to("openstack-swift:localhost");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container").setHeader("CamelOpenstackSwiftContainerRead", constant(".r:*")).to("openstack-swift:localhost");
                                    }
                                }
                                """
                        )
                )
        );
    }
}
