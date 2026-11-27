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
 * Tests for migrating from Camel 4.18.5 to 4.18.6: the remaining camel-openstack Swift header names (CAMEL-25895),
 * which also ship in 4.22.3 and 4.24.0. The Swift Endpoint DSL header builders are the same in 4.18.5 and 4.22, so
 * the 4.22 jars are on the parser classpath.
 */
public class CamelUpdate418_6Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_18_6)
                .parser(CamelTestUtil.parserFromClasspath(CamelTestUtil.CamelVersion.v4_22, "camel-api", "camel-core-model", "camel-support", "camel-endpointdsl"))
                .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample
    @Test
    void openstackSwiftHeadersJava() {
        //language=java
        rewriteRun(
                mavenProject("test-openstack",
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_18_5),
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
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_18_5),
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:container"/>
                                    <setHeader name="X-Container-Meta-">
                                        <simple>${body}</simple>
                                    </setHeader>
                                    <setHeader name="X-Versions-Location">
                                        <constant>versions</constant>
                                    </setHeader>
                                    <setHeader name="X-Container-Read">
                                        <constant>.r:*</constant>
                                    </setHeader>
                                    <setHeader name="X-Container-Write">
                                        <constant>admin</constant>
                                    </setHeader>
                                    <log message="read ACL ${header.X-Container-Read}"/>
                                    <to uri="openstack-swift:localhost"/>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:container"/>
                                    <setHeader name="CamelOpenstackSwiftContainerMetadata">
                                        <simple>${body}</simple>
                                    </setHeader>
                                    <setHeader name="CamelOpenstackSwiftVersionsLocation">
                                        <constant>versions</constant>
                                    </setHeader>
                                    <setHeader name="CamelOpenstackSwiftContainerRead">
                                        <constant>.r:*</constant>
                                    </setHeader>
                                    <setHeader name="CamelOpenstackSwiftContainerWrite">
                                        <constant>admin</constant>
                                    </setHeader>
                                    <log message="read ACL ${header.CamelOpenstackSwiftContainerRead}"/>
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
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_18_5),
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "direct:container"
                                      steps:
                                        - setHeader:
                                            name: X-Container-Write
                                            constant: admin
                                        - setHeader:
                                            name: X-Versions-Location
                                            constant: versions
                                        - removeHeader:
                                            name: X-Container-Read
                                        - log: "write ACL ${header.X-Container-Write}"
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
                                        - setHeader:
                                            name: CamelOpenstackSwiftVersionsLocation
                                            constant: versions
                                        - removeHeader:
                                            name: CamelOpenstackSwiftContainerRead
                                        - log: "write ACL ${header.CamelOpenstackSwiftContainerWrite}"
                                        - to: "openstack-swift:localhost"
                                """
                        )
                )
        );
    }

    @Test
    void openstackSwiftHeadersSimpleInJava() {
        //language=java
        rewriteRun(
                mavenProject("test-openstack-simple",
                        CamelTestUtil.pomXmlSpec("camel-openstack", CamelTestUtil.CamelVersion.v4_18_5),
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftLogRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container")
                                            .log("versions in ${header.X-Versions-Location}")
                                            .to("openstack-swift:localhost");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftLogRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:container")
                                            .log("versions in ${header.CamelOpenstackSwiftVersionsLocation}")
                                            .to("openstack-swift:localhost");
                                    }
                                }
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
                        CamelTestUtil.pomXmlSpec("camel-core", CamelTestUtil.CamelVersion.v4_18_5),
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
    void preconditionBlocksOpenstackSwiftXmlWithoutDependency() {
        //language=xml
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-openstack-xml",
                        CamelTestUtil.pomXmlSpec("camel-http", CamelTestUtil.CamelVersion.v4_18_5),
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:container"/>
                                    <setHeader name="X-Container-Read">
                                        <constant>.r:*</constant>
                                    </setHeader>
                                    <to uri="http://swift-proxy/v1/container"/>
                                </route>
                                """
                        )
                )
        );
    }
}
