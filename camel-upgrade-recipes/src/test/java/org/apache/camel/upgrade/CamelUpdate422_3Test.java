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
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for migrating from Camel 4.22.2 to 4.22.3: the pubnub override channel header (CAMEL-25888), and the chain
 * reference to the 4.18.6 openstack Swift header rename (CAMEL-25895), which {@link CamelUpdate418_6Test} covers.
 * <p>
 * Skipped under the latest aggregate until the 4.24 recipe chains {@code camel422_3}.
 */
@DisabledIfSystemProperty(named = CamelTestUtil.PROPERTY_USE_RECIPE, matches = "org\\.apache\\.camel\\.upgrade\\.CamelMigrationRecipe")
public class CamelUpdate422_3Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_22_3, true)
                .parser(CamelTestUtil.parserFromClasspath(CamelTestUtil.CamelVersion.v4_22, "camel-api", "camel-core-model", "camel-support", "camel-endpointdsl", "camel-pubnub"))
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

    @Test
    void pubNubOverrideChannelJava() {
        rewriteRun(
                mavenProject("test-pubnub",
                        CamelTestUtil.pomXmlSpec("camel-pubnub", CamelTestUtil.CamelVersion.v4_22_2),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;
                                import org.apache.camel.component.pubnub.PubNubConstants;

                                public class PubNubRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("pubnub:in")
                                            .log("received on ${header.CamelPubNubChannel}")
                                            .filter(header("CamelPubNubChannel").isEqualTo("in"))
                                            .setHeader("CamelPubNubChannel", constant("out"))
                                            .setHeader(PubNubConstants.CHANNEL, constant("out"))
                                            .process(exchange -> {
                                                String from = exchange.getIn().getHeader("CamelPubNubChannel", String.class);
                                                exchange.getIn().setHeader("CamelPubNubChannel", from + "-copy");
                                            })
                                            .to("pubnub:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;
                                import org.apache.camel.component.pubnub.PubNubConstants;

                                public class PubNubRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("pubnub:in")
                                            .log("received on ${header.CamelPubNubChannel}")
                                            .filter(header("CamelPubNubChannel").isEqualTo("in"))
                                            .setHeader("CamelPubNubOverrideChannel", constant("out"))
                                            .setHeader(PubNubConstants.OVERRIDE_CHANNEL, constant("out"))
                                            .process(exchange -> {
                                                String from = exchange.getIn().getHeader("CamelPubNubChannel", String.class);
                                                exchange.getIn().setHeader("CamelPubNubOverrideChannel", from + "-copy");
                                            })
                                            .to("pubnub:out");
                                    }
                                }
                                """
                        ),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class PubNubDslRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(pubnub("in"))
                                            .setHeader(pubnub().pubNubChannel(), constant("out"))
                                            .to(pubnub("out"));
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class PubNubDslRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(pubnub("in"))
                                            .setHeader(pubnub().pubNubOverrideChannel(), constant("out"))
                                            .to(pubnub("out"));
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void pubNubOverrideChannelXmlAndYaml() {
        rewriteRun(
                mavenProject("test-pubnub-dsl",
                        CamelTestUtil.pomXmlSpec("camel-pubnub", CamelTestUtil.CamelVersion.v4_22_2),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="pubnub:in"/>
                                    <log message="received on ${header.CamelPubNubChannel}"/>
                                    <setHeader name="CamelPubNubChannel">
                                        <constant>out</constant>
                                    </setHeader>
                                    <to uri="pubnub:out"/>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="pubnub:in"/>
                                    <log message="received on ${header.CamelPubNubChannel}"/>
                                    <setHeader name="CamelPubNubOverrideChannel">
                                        <constant>out</constant>
                                    </setHeader>
                                    <to uri="pubnub:out"/>
                                </route>
                                """
                        ),
                        //language=yaml
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "pubnub:in"
                                      steps:
                                        - removeHeader:
                                            name: CamelPubNubChannel
                                        - setHeader:
                                            name: CamelPubNubChannel
                                            simple: "${header.CamelPubNubChannel}-copy"
                                        - to:
                                            uri: "pubnub:out"
                                """,
                                """
                                - route:
                                    from:
                                      uri: "pubnub:in"
                                      steps:
                                        - removeHeader:
                                            name: CamelPubNubChannel
                                        - setHeader:
                                            name: CamelPubNubOverrideChannel
                                            simple: "${header.CamelPubNubChannel}-copy"
                                        - to:
                                            uri: "pubnub:out"
                                """
                        )
                )
        );
    }

    @Test
    void pubNubOverrideChannelIsIdempotent() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-pubnub-idempotent",
                        CamelTestUtil.pomXmlSpec("camel-pubnub", CamelTestUtil.CamelVersion.v4_22_2),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <setHeader name="CamelPubNubOverrideChannel">
                                        <constant>out</constant>
                                    </setHeader>
                                    <to uri="pubnub:out"/>
                                </route>
                                """
                        )
                )
        );
    }

    @Test
    void pubNubWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-pubnub",
                        CamelTestUtil.pomXmlSpec("camel-core", CamelTestUtil.CamelVersion.v4_22_2),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class OtherRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in").setHeader("CamelPubNubChannel", constant("out")).to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }
}
