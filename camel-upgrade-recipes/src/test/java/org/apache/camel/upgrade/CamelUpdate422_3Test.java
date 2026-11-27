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
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for migrating from Camel 4.22.2 to 4.22.3: the remaining camel-openstack Swift header names (CAMEL-25895) and
 * the pubnub override channel header (CAMEL-25888), which also ship in 4.24.0.
 */
public class CamelUpdate422_3Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_22_3, true)
                .parser(CamelTestUtil.parserFromClasspath(CamelTestUtil.CamelVersion.v4_22, "camel-api", "camel-core-model", "camel-support", "camel-endpointdsl", "camel-pubnub"))
                .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void openstackSwiftHeadersJava() {
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
                                        from("direct:container")
                                            .setHeader("X-Container-Read", constant(".r:*"))
                                            .setHeader("X-Container-Write", constant("admin"))
                                            .setHeader("X-Versions-Location", constant("versions"))
                                            .setHeader("X-Container-Meta-", constant(java.util.Map.of("k", "v")))
                                            .setHeader("X-Container-Meta-Color", constant("blue"))
                                            .to("openstack-swift:localhost");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container")
                                            .setHeader("CamelOpenstackSwiftContainerRead", constant(".r:*"))
                                            .setHeader("CamelOpenstackSwiftContainerWrite", constant("admin"))
                                            .setHeader("CamelOpenstackSwiftVersionsLocation", constant("versions"))
                                            .setHeader("CamelOpenstackSwiftContainerMetadata", constant(java.util.Map.of("k", "v")))
                                            .setHeader("X-Container-Meta-Color", constant("blue"))
                                            .to("openstack-swift:localhost");
                                    }
                                }
                                """
                        ),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class SwiftEndpointDslRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container")
                                            .setHeader(openstackSwift().xContainerRead(), constant(".r:*"))
                                            .setHeader(openstackSwift().xContainerWrite(), constant("admin"))
                                            .setHeader(openstackSwift().xVersionsLocation(), constant("versions"))
                                            .setHeader(openstackSwift().xContainerMeta(), constant("m"))
                                            .to(openstackSwift("localhost"));
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class SwiftEndpointDslRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container")
                                            .setHeader(openstackSwift().openstackSwiftContainerRead(), constant(".r:*"))
                                            .setHeader(openstackSwift().openstackSwiftContainerWrite(), constant("admin"))
                                            .setHeader(openstackSwift().openstackSwiftVersionsLocation(), constant("versions"))
                                            .setHeader(openstackSwift().openstackSwiftContainerMetadata(), constant("m"))
                                            .to(openstackSwift("localhost"));
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void openstackSwiftHeadersXml() {
        //language=xml
        rewriteRun(
                mavenProject("test-openstack-xml",
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_22_2),
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:container"/>
                                    <setHeader name="X-Container-Read">
                                        <constant>.r:*</constant>
                                    </setHeader>
                                    <setHeader name="X-Versions-Location">
                                        <constant>versions</constant>
                                    </setHeader>
                                    <to uri="openstack-swift:localhost"/>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:container"/>
                                    <setHeader name="CamelOpenstackSwiftContainerRead">
                                        <constant>.r:*</constant>
                                    </setHeader>
                                    <setHeader name="CamelOpenstackSwiftVersionsLocation">
                                        <constant>versions</constant>
                                    </setHeader>
                                    <to uri="openstack-swift:localhost"/>
                                </route>
                                """
                        )
                )
        );
    }

    @Test
    void openstackSwiftHeadersYaml() {
        //language=yaml
        rewriteRun(
                mavenProject("test-openstack-yaml",
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_22_2),
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "direct:container"
                                      steps:
                                        - setHeader:
                                            name: X-Container-Write
                                            constant: admin
                                        - to: "openstack-swift:localhost"
                                """,
                                """
                                - route:
                                    from:
                                      uri: "direct:container"
                                      steps:
                                        - setHeader:
                                            name: CamelOpenstackSwiftContainerWrite
                                            constant: admin
                                        - to: "openstack-swift:localhost"
                                """
                        )
                )
        );
    }

    @Test
    void preconditionBlocksOpenstackSwiftWithoutDependency() {
        //language=java
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-openstack",
                        CamelTestUtil.pomXmlSpec("camel-core", CamelTestUtil.CamelVersion.v4_22_2),
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class HttpRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container")
                                            .setHeader("X-Container-Read", constant(".r:*"))
                                            .to("http://swift-proxy/v1/container");
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
