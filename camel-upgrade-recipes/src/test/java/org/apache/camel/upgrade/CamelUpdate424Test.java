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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for migrating from Camel 4.23 to 4.24, developed against the 4.23.0 jars on the parser classpath.
 */
public class CamelUpdate424Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_24, true)
                .parser(CamelTestUtil.parserFromClasspath(CamelTestUtil.CamelVersion.v4_23,
                        "camel-core-model", "camel-api", "camel-support", "camel-endpointdsl", "camel-toon", "camel-diagram",
                        "camel-huaweicloud-smn", "camel-minio", "camel-typesafe-ai", "camel-aws-bedrock", "camel-semantic",
                        "camel-jacksonxml", "camel-jackson3xml", "bcprov-jdk18on"))
                .typeValidationOptions(TypeValidation.none())
                .expectedCyclesThatMakeChanges(1);
    }

    /** A pom whose org.apache.camel 4.23.0 versions are bumped when the test runs under the latest aggregate. */
    private static SourceSpecs pom(String before, String after) {
        if (CamelTestUtil.isRecipeOverridden()) {
            after = after.replace("<version>4.23.0</version>", "<version>" + CamelTestUtil.getCamelLatestVersion() + "</version>");
        }
        return before.equals(after) ? pomXml(before) : pomXml(before, after);
    }

    private static SourceSpecs pom(String unchanged) {
        return pom(unchanged, unchanged);
    }

    private static SourceSpecs pomWith(String artifactId) {
        return CamelTestUtil.pomXmlSpec(artifactId, CamelTestUtil.CamelVersion.v4_23);
    }

    @Test
    void javaVersion21() {
        //language=xml
        rewriteRun(
                spec -> spec.recipeFromResources("org.apache.camel.upgrade.JavaVersion21"),
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <maven.compiler.release>17</maven.compiler.release>
                            </properties>
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

    @Test
    void javaVersion21LeavesNewerJavaAlone() {
        //language=xml
        rewriteRun(
                spec -> spec.recipeFromResources("org.apache.camel.upgrade.JavaVersion21").expectedCyclesThatMakeChanges(0),
                pomXml(
                        """
                        <project>
                            <groupId>com.example</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                            <properties>
                                <maven.compiler.release>25</maven.compiler.release>
                            </properties>
                        </project>
                        """
                )
        );
    }

    @Test
    void migrateUndertowServlet() {
        //language=xml
        rewriteRun(
                mavenProject("test-undertow",
                        pom(
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
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-undertow</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.undertow</groupId>
                                            <artifactId>undertow-servlet</artifactId>
                                            <version>2.3.26.Final</version>
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
                                    <dependencies>
                                        <dependency>
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-undertow</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.undertow.ee</groupId>
                                            <artifactId>undertow-servlet</artifactId>
                                            <version>2.0.3.Final</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void undertowServletWithoutCamelUndertowIsUntouched() {
        //language=xml
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-undertow",
                        pom(
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
                                            <groupId>io.undertow</groupId>
                                            <artifactId>undertow-servlet</artifactId>
                                            <version>2.3.26.Final</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void migrateBouncyCastleLms() {
        rewriteRun(
                mavenProject("test-pqc",
                        pomWith("camel-pqc"),
                        //language=java
                        java(
                                """
                                import org.bouncycastle.pqc.crypto.lms.LMOtsParameters;
                                import org.bouncycastle.pqc.crypto.lms.LMSigParameters;
                                import org.bouncycastle.pqc.jcajce.spec.LMSKeyGenParameterSpec;

                                public class LmsKeys {
                                    LMSKeyGenParameterSpec spec = new LMSKeyGenParameterSpec(LMSigParameters.lms_sha256_n32_h5, LMOtsParameters.sha256_n32_w4);
                                }
                                """,
                                """
                                import org.bouncycastle.crypto.params.LMOtsParameters;
                                import org.bouncycastle.crypto.params.LMSigParameters;
                                import org.bouncycastle.jcajce.spec.LMSKeyGenParameterSpec;

                                public class LmsKeys {
                                    LMSKeyGenParameterSpec spec = new LMSKeyGenParameterSpec(LMSigParameters.lms_sha256_n32_h5, LMOtsParameters.sha256_n32_w4);
                                }
                                """
                        ),
                        //language=java
                        java(
                                """
                                import org.bouncycastle.pqc.jcajce.interfaces.XMSSPrivateKey;
                                import org.bouncycastle.pqc.jcajce.spec.XMSSParameterSpec;

                                public class XmssKeys {
                                    XMSSParameterSpec spec = XMSSParameterSpec.SHA2_10_256;
                                    XMSSPrivateKey key;
                                }
                                """,
                                """
                                import org.bouncycastle.jcajce.interfaces.XMSSPrivateKey;
                                import org.bouncycastle.jcajce.spec.XMSSParameterSpec;

                                public class XmssKeys {
                                    XMSSParameterSpec spec = XMSSParameterSpec.SHA2_10_256;
                                    XMSSPrivateKey key;
                                }
                                """
                        )
                )
        );
    }

    @Test
    void bouncyCastleLmsWithoutCamelPqcIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-pqc",
                        pomWith("camel-crypto"),
                        //language=java
                        java(
                                """
                                import org.bouncycastle.pqc.crypto.lms.LMSigParameters;

                                public class LmsKeys {
                                    LMSigParameters sig = LMSigParameters.lms_sha256_n32_h5;
                                }
                                """
                        )
                )
        );
    }

    @DocumentExample
    @Test
    void removedComponents() {
        //language=xml
        rewriteRun(
                pom(
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
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-core</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                                <dependency>
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-ognl</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                                <dependency>
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-jbang-plugin-route-parser</artifactId>
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
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-core</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """
                )
        );
    }

    @Test
    void migrateRocketMqHeadersJava() {
        rewriteRun(
                mavenProject("test-rocketmq",
                        pomWith("camel-rocketmq"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class RocketRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("rocketmq:in")
                                            .setHeader("CamelRockerMQOverrideTopicName", constant("out"))
                                            .log("${header.CamelRockerMQMsgId}")
                                            .to("rocketmq:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class RocketRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("rocketmq:in")
                                            .setHeader("CamelRocketMQOverrideTopicName", constant("out"))
                                            .log("${header.CamelRocketMQMsgId}")
                                            .to("rocketmq:out");
                                    }
                                }
                                """
                        ),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class RocketDslRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(rocketmq("in"))
                                            .setHeader(rocketmq().rockerMQOverrideTopicName(), constant("out"))
                                            .setHeader(rocketmq().rockerMQTag(), constant("t"))
                                            .to(rocketmq("out"));
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class RocketDslRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(rocketmq("in"))
                                            .setHeader(rocketmq().rocketMQOverrideTopicName(), constant("out"))
                                            .setHeader(rocketmq().rocketMQTag(), constant("t"))
                                            .to(rocketmq("out"));
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateRocketMqHeadersXmlAndYaml() {
        rewriteRun(
                mavenProject("test-rocketmq-dsl",
                        pomWith("camel-rocketmq"),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <setHeader name="CamelRockerMQTag">
                                        <constant>t</constant>
                                    </setHeader>
                                    <to uri="rocketmq:out"/>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <setHeader name="CamelRocketMQTag">
                                        <constant>t</constant>
                                    </setHeader>
                                    <to uri="rocketmq:out"/>
                                </route>
                                """
                        ),
                        //language=yaml
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - setHeader:
                                            name: CamelRockerMQKey
                                            constant: k
                                        - to: "rocketmq:out"
                                """,
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - setHeader:
                                            name: CamelRocketMQKey
                                            constant: k
                                        - to: "rocketmq:out"
                                """
                        )
                )
        );
    }

    @Test
    void rocketMqHeadersWithoutCamelRocketMqAreUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-rocketmq",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class OtherRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in").setHeader("CamelRockerMQTag", constant("t")).to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Disabled("needs the Camel 4.24.0 artifacts on Maven Central or the 4.24.0 staging repository")
    @Test
    void migrateWolfDefenderArtifact() {
        //language=xml
        rewriteRun(
                pom(
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
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-wolf-defender</artifactId>
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
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.camel</groupId>
                                    <artifactId>camel-semantic-wolf-defender</artifactId>
                                    <version>4.23.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                        """
                )
        );
    }

    @Disabled("needs the Camel 4.24.0 artifacts on Maven Central or the 4.24.0 staging repository")
    @Test
    void addSemanticTypesafeAiWhenAdapterIsUsed() {
        rewriteRun(
                mavenProject("test-typesafe-ai",
                        //language=xml
                        pom(
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
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-typesafe-ai</artifactId>
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
                                    <dependencies>
                                        <dependency>
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-typesafe-ai</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-semantic-typesafe-ai</artifactId>
                                            <version>4.24.0</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        ),
                        //language=java
                        java(
                                """
                                import org.apache.camel.component.typesafeai.TypeSafeAiSemanticAdapter;

                                public class Experts {
                                    TypeSafeAiSemanticAdapter adapter;
                                }
                                """
                        )
                )
        );
    }

    @Test
    void semanticTypesafeAiNotAddedWithoutAdapter() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-typesafe-ai-only",
                        //language=xml
                        pom(
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
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-typesafe-ai</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        ),
                        //language=java
                        java(
                                """
                                public class NoExperts {
                                }
                                """
                        )
                )
        );
    }

    private static final String BACKLOG_TRACER_4_23 = """
            package org.apache.camel.spi;

            public interface BacklogTracer {
                void setEnabled(boolean enabled);
                void setActivityEnabled(boolean activityEnabled);
                void setActivitySize(int activitySize);
            }
            """;

    @Test
    void removeBacklogTracerActivity() {
        rewriteRun(
                spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(BACKLOG_TRACER_4_23)),
                //language=properties
                properties(
                        """
                        camel.trace.enabled=true
                        camel.trace.activityEnabled=true
                        camel.trace.activity-size=50
                        """,
                        """
                        camel.trace.enabled=true
                        """
                ),
                //language=java
                java(
                        """
                        import org.apache.camel.spi.BacklogTracer;

                        public class TracerSetup {
                            void setup(BacklogTracer tracer) {
                                tracer.setEnabled(true);
                                tracer.setActivityEnabled(true);
                                tracer.setActivitySize(50);
                            }
                        }
                        """,
                        """
                        import org.apache.camel.spi.BacklogTracer;

                        public class TracerSetup {
                            void setup(BacklogTracer tracer) {
                                tracer.setEnabled(true);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void backlogTracerKeepsOtherOptions() {
        //language=properties
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(0),
                properties(
                        """
                        camel.trace.enabled=true
                        camel.trace.backlogSize=100
                        """
                )
        );
    }

    @Test
    void migrateMinioGetObjectRange() {
        rewriteRun(
                mavenProject("test-minio",
                        pomWith("camel-minio"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;
                                import org.apache.camel.component.minio.MinioOperations;

                                public class MinioRoute extends RouteBuilder {
                                    MinioOperations op = MinioOperations.getPartialObject;

                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("minio:bucket?operation=getPartialObject&offset=0&length=10")
                                            .to("minio:bucket?region=eu&operation=getPartialObject");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;
                                import org.apache.camel.component.minio.MinioOperations;

                                public class MinioRoute extends RouteBuilder {
                                    MinioOperations op = MinioOperations.getObjectRange;

                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("minio:bucket?operation=getObjectRange&offset=0&length=10")
                                            .to("minio:bucket?region=eu&operation=getObjectRange");
                                    }
                                }
                                """
                        ),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <to uri="minio:bucket?operation=getPartialObject"/>
                                    <to uri="minio:bucket?region=eu&amp;operation=getPartialObject"/>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <to uri="minio:bucket?operation=getObjectRange"/>
                                    <to uri="minio:bucket?region=eu&amp;operation=getObjectRange"/>
                                </route>
                                """
                        ),
                        //language=yaml
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - to:
                                            uri: "minio:bucket?operation=getPartialObject"
                                """,
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - to:
                                            uri: "minio:bucket?operation=getObjectRange"
                                """
                        ),
                        //language=properties
                        properties(
                                """
                                camel.component.minio.operation=getPartialObject
                                """,
                                """
                                camel.component.minio.operation=getObjectRange
                                """
                        )
                )
        );
    }

    @Test
    void minioOtherOperationsAreUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-minio-other",
                        pomWith("camel-minio"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class MinioRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("minio:bucket?operation=getObject")
                                            .to("minio:getPartialObject?operation=listObjects");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateHuaweiCloudSmnMessageId() {
        rewriteRun(
                mavenProject("test-smn",
                        pomWith("camel-huaweicloud-smn"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.Exchange;
                                import org.apache.camel.component.huaweicloud.smn.constants.SmnProperties;

                                public class SmnReader {
                                    Object id(Exchange exchange) {
                                        return exchange.getProperty(SmnProperties.SERVICE_MESSAGE_ID);
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.Exchange;
                                import org.apache.camel.component.huaweicloud.smn.constants.SmnProperties;

                                public class SmnReader {
                                    Object id(Exchange exchange) {
                                        return exchange.getProperty(SmnProperties.MESSAGE_ID);
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void huaweiCloudSmnWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-smn",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.component.huaweicloud.smn.constants.SmnProperties;

                                public class SmnReader {
                                    String id = SmnProperties.SERVICE_MESSAGE_ID;
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateMcpJsonJackson3() {
        //language=xml
        rewriteRun(
                mavenProject("test-mcp",
                        pom(
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
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-mcp-server</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.modelcontextprotocol.sdk</groupId>
                                            <artifactId>mcp-json-jackson2</artifactId>
                                            <version>2.0.1</version>
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
                                    <dependencies>
                                        <dependency>
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-mcp-server</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.modelcontextprotocol.sdk</groupId>
                                            <artifactId>mcp-json-jackson3</artifactId>
                                            <version>2.2.0</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void migrateRouteDiagramRenderer() {
        rewriteRun(
                mavenProject("test-diagram",
                        pomWith("camel-diagram"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.diagram.RouteDiagramRenderer;

                                public class Diagrams {
                                    RouteDiagramRenderer old = new RouteDiagramRenderer(180, 12, 8, false);
                                    RouteDiagramRenderer current = new RouteDiagramRenderer(180, 12, true);
                                }
                                """,
                                """
                                import org.apache.camel.diagram.RouteDiagramRenderer;

                                public class Diagrams {
                                    RouteDiagramRenderer old = new RouteDiagramRenderer(180, 12, false);
                                    RouteDiagramRenderer current = new RouteDiagramRenderer(180, 12, true);
                                }
                                """
                        )
                )
        );
    }

    @Test
    void routeDiagramRendererWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-diagram",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.diagram.RouteDiagramRenderer;

                                public class Diagrams {
                                    RouteDiagramRenderer old = new RouteDiagramRenderer(180, 12, 8, false);
                                }
                                """
                        )
                )
        );
    }

    @Test
    void removeToonLengthMarker() {
        rewriteRun(
                mavenProject("test-toon",
                        pomWith("camel-toon"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class ToonRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .marshal(dataFormat().toon().lengthMarker(true).end())
                                            .to("mock:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class ToonRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .marshal(dataFormat().toon().end())
                                            .to("mock:out");
                                    }
                                }
                                """
                        ),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <marshal>
                                        <toon lengthMarker="true" strict="false"/>
                                    </marshal>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <marshal>
                                        <toon strict="false"/>
                                    </marshal>
                                </route>
                                """
                        ),
                        //language=yaml
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - marshal:
                                            toon:
                                              lengthMarker: true
                                              strict: false
                                """,
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - marshal:
                                            toon:
                                              strict: false
                                """
                        ),
                        //language=properties
                        properties(
                                """
                                camel.dataformat.toon.length-marker=true
                                camel.dataformat.toon.strict=false
                                """,
                                """
                                camel.dataformat.toon.strict=false
                                """
                        )
                )
        );
    }

    @Test
    void toonLengthMarkerWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-toon",
                        pomWith("camel-core"),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <marshal>
                                        <toon lengthMarker="true"/>
                                    </marshal>
                                </route>
                                """
                        )
                )
        );
    }

    @Test
    void removeRemoteFileReadLockOptions() {
        rewriteRun(
                mavenProject("test-sftp",
                        pomWith("camel-ftp"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class SftpRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(sftp("host/in").readLock("changed").readLockRemoveOnCommit(true).readLockIdempotentReleaseDelay(1000).delay(5000))
                                            .to(ftp("host/out"));
                                        from(ftp("host/in2").readLockDeleteOrphanLockFiles(false))
                                            .to("mock:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class SftpRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(sftp("host/in").readLock("changed").delay(5000))
                                            .to(ftp("host/out"));
                                        from(ftp("host/in2"))
                                            .to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void fileReadLockOptionsAreUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-file",
                        pomWith("camel-ftp"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class FileRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(file("in").readLock("idempotent").readLockRemoveOnCommit(true).readLockIdempotentReleaseDelay(1000))
                                            .to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateKubernetesHttpClientExclusion() {
        //language=xml
        rewriteRun(
                mavenProject("test-kubernetes",
                        pom(
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
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-kubernetes</artifactId>
                                            <version>4.23.0</version>
                                            <exclusions>
                                                <exclusion>
                                                    <groupId>io.fabric8</groupId>
                                                    <artifactId>kubernetes-httpclient-vertx</artifactId>
                                                </exclusion>
                                            </exclusions>
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
                                    <dependencies>
                                        <dependency>
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-kubernetes</artifactId>
                                            <version>4.23.0</version>
                                            <exclusions>
                                                <exclusion>
                                                    <groupId>io.fabric8</groupId>
                                                    <artifactId>kubernetes-httpclient-vertx-5</artifactId>
                                                </exclusion>
                                            </exclusions>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void kubernetesHttpClientExclusionWithoutCamelKubernetesIsUntouched() {
        //language=xml
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(0),
                mavenProject("test-no-kubernetes",
                        pom(
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
                                            <groupId>io.fabric8</groupId>
                                            <artifactId>kubernetes-client</artifactId>
                                            <version>7.4.0</version>
                                            <exclusions>
                                                <exclusion>
                                                    <groupId>io.fabric8</groupId>
                                                    <artifactId>kubernetes-httpclient-vertx</artifactId>
                                                </exclusion>
                                            </exclusions>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void migratePlc4xS7Light() {
        rewriteRun(
                mavenProject("test-plc4x",
                        pomWith("camel-plc4x"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class PlcRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("timer:t").to("plc4x:s7-light://10.0.0.1?remote-rack=0").to("plc4x:modbus-tcp://10.0.0.2");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class PlcRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("timer:t").to("plc4x:s7://10.0.0.1?remote-rack=0").to("plc4x:modbus-tcp://10.0.0.2");
                                    }
                                }
                                """
                        ),
                        //language=yaml
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "timer:t"
                                      steps:
                                        - to:
                                            uri: "plc4x:s7-light://10.0.0.1"
                                """,
                                """
                                - route:
                                    from:
                                      uri: "timer:t"
                                      steps:
                                        - to:
                                            uri: "plc4x:s7://10.0.0.1"
                                """
                        )
                )
        );
    }

    @Test
    void plc4xWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-plc4x",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class PlcRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("timer:t").to("plc4x:s7-light://10.0.0.1");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateAlibabaSlsListLogStoreName() {
        rewriteRun(
                mavenProject("test-sls",
                        pomWith("camel-alibaba-sls"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class SlsRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .setHeader(alibabaSls().alibabaSlsLogstoreName(), constant("app"))
                                            .setHeader(alibabaSls().alibabaSlsLogStoreName(), constant("logs"))
                                            .to("alibaba-sls:project");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class SlsRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .setHeader(alibabaSls().alibabaSlsListLogStoreName(), constant("app"))
                                            .setHeader(alibabaSls().alibabaSlsLogStoreName(), constant("logs"))
                                            .to("alibaba-sls:project");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void camel424ChainIncludesCamel422_3PatchRecipe() {
        rewriteRun(
                mavenProject("test-openstack",
                        pomWith("camel-openstack"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in").setHeader("X-Container-Read", constant(".r:*")).to("openstack-swift:localhost");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class SwiftRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in").setHeader("CamelOpenstackSwiftContainerRead", constant(".r:*")).to("openstack-swift:localhost");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateBedrockNonStreamingOperations() {
        rewriteRun(
                mavenProject("test-bedrock",
                        pomWith("camel-aws-bedrock"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;
                                import org.apache.camel.component.aws2.bedrock.runtime.BedrockOperations;

                                public class BedrockRoute extends RouteBuilder {
                                    BedrockOperations image = BedrockOperations.invokeImageModelStreaming;
                                    BedrockOperations embeddings = BedrockOperations.invokeEmbeddingsModelStreaming;
                                    BedrockOperations text = BedrockOperations.invokeTextModelStreaming;

                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("aws-bedrock:label?operation=invokeImageModelStreaming&modelId=m")
                                            .to("aws-bedrock:label?modelId=m&operation=invokeEmbeddingsModelStreaming")
                                            .to("aws-bedrock:label?operation=invokeTextModelStreaming");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;
                                import org.apache.camel.component.aws2.bedrock.runtime.BedrockOperations;

                                public class BedrockRoute extends RouteBuilder {
                                    BedrockOperations image = BedrockOperations.invokeImageModel;
                                    BedrockOperations embeddings = BedrockOperations.invokeEmbeddingsModel;
                                    BedrockOperations text = BedrockOperations.invokeTextModelStreaming;

                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("aws-bedrock:label?operation=invokeImageModel&modelId=m")
                                            .to("aws-bedrock:label?modelId=m&operation=invokeEmbeddingsModel")
                                            .to("aws-bedrock:label?operation=invokeTextModelStreaming");
                                    }
                                }
                                """
                        ),
                        //language=yaml
                        yaml(
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - to:
                                            uri: "aws-bedrock:label?operation=invokeImageModelStreaming"
                                """,
                                """
                                - route:
                                    from:
                                      uri: "direct:in"
                                      steps:
                                        - to:
                                            uri: "aws-bedrock:label?operation=invokeImageModel"
                                """
                        ),
                        //language=properties
                        properties(
                                """
                                camel.component.aws-bedrock.operation=invokeEmbeddingsModelStreaming
                                """,
                                """
                                camel.component.aws-bedrock.operation=invokeEmbeddingsModel
                                """
                        )
                )
        );
    }

    @Test
    void bedrockWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-bedrock",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class BedrockRoute extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in").to("aws-bedrock:label?operation=invokeImageModelStreaming");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void removeSemanticAuditDecisionCalls() {
        rewriteRun(
                mavenProject("test-semantic",
                        pomWith("camel-semantic"),
                        //language=java
                        java(
                                """
                                import java.util.List;
                                import java.util.Map;

                                import org.apache.camel.Exchange;
                                import org.apache.camel.semantic.SemanticAudit;

                                public class AuditStep {
                                    void process(Exchange exchange) {
                                        exchange.getIn().setHeader("approved", true);
                                        SemanticAudit.get(exchange.getContext()).decision(exchange, Map.of("action", "approve"), List.of());
                                    }
                                }
                                """,
                                // the argument imports are left unused, which still compiles
                                """
                                import java.util.List;
                                import java.util.Map;

                                import org.apache.camel.Exchange;

                                public class AuditStep {
                                    void process(Exchange exchange) {
                                        exchange.getIn().setHeader("approved", true);
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void semanticAuditWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-semantic",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import java.util.List;
                                import java.util.Map;

                                import org.apache.camel.Exchange;
                                import org.apache.camel.semantic.SemanticAudit;

                                public class AuditStep {
                                    void process(Exchange exchange) {
                                        SemanticAudit.get(exchange.getContext()).decision(exchange, Map.of("action", "approve"), List.of());
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateJacksonXmlTypeConverterConstant() {
        rewriteRun(
                mavenProject("test-jacksonxml",
                        pomWith("camel-jacksonxml"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.component.jacksonxml.JacksonXMLConstants;

                                public class XmlSettings {
                                    String key = JacksonXMLConstants.ENABLE_XML_TYPE_CONVERTER;
                                }
                                """,
                                """
                                import org.apache.camel.component.jacksonxml.JacksonXMLConstants;

                                public class XmlSettings {
                                    String key = JacksonXMLConstants.ENABLE_TYPE_CONVERTER;
                                }
                                """
                        )
                ),
                mavenProject("test-jackson3xml",
                        pomWith("camel-jackson3xml"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.component.jackson3xml.JacksonXMLConstants;

                                public class Xml3Settings {
                                    String key = JacksonXMLConstants.ENABLE_XML_TYPE_CONVERTER;
                                }
                                """,
                                """
                                import org.apache.camel.component.jackson3xml.JacksonXMLConstants;

                                public class Xml3Settings {
                                    String key = JacksonXMLConstants.ENABLE_TYPE_CONVERTER;
                                }
                                """
                        )
                )
        );
    }

    @Test
    void jacksonXmlConstantWithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-jacksonxml",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.component.jacksonxml.JacksonXMLConstants;

                                public class XmlSettings {
                                    String key = JacksonXMLConstants.ENABLE_XML_TYPE_CONVERTER;
                                }
                                """
                        )
                )
        );
    }

    @Test
    void mcpJsonJackson2WithoutCamelMcpIsUntouched() {
        //language=xml
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(0),
                mavenProject("test-no-mcp",
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
                                            <groupId>io.modelcontextprotocol.sdk</groupId>
                                            <artifactId>mcp-json-jackson2</artifactId>
                                            <version>2.0.1</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void migrateAs2EdiMessageContentType() {
        rewriteRun(
                mavenProject("test-as2",
                        pomWith("camel-as2"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class As2Route extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("as2:client/send?ediMessageType=application/edifact&as2To=partner")
                                            .to("as2:client/send?as2To=partner&ediMessageType=application/xml")
                                            .to("as2://client/send?ediMessageType=application/edi-consent");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class As2Route extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in")
                                            .to("as2:client/send?ediMessageContentType=application/edifact&as2To=partner")
                                            .to("as2:client/send?as2To=partner&ediMessageContentType=application/xml")
                                            .to("as2://client/send?ediMessageContentType=application/edi-consent");
                                    }
                                }
                                """
                        ),
                        //language=xml
                        xml(
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <to uri="as2:client/send?ediMessageType=application/edi-x12"/>
                                    <to uri="as2://client/send?as2To=partner&amp;ediMessageType=application/xml"/>
                                </route>
                                """,
                                """
                                <route xmlns="http://camel.apache.org/schema/spring">
                                    <from uri="direct:in"/>
                                    <to uri="as2:client/send?ediMessageContentType=application/edi-x12"/>
                                    <to uri="as2://client/send?as2To=partner&amp;ediMessageContentType=application/xml"/>
                                </route>
                                """
                        )
                )
        );
    }

    @Test
    void as2OtherApisAndExplicitContentTypeAreUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-as2-other",
                        pomWith("camel-as2"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class As2Route extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("as2:server/listen?ediMessageType=application/edifact")
                                            .to("as2:client/send?ediMessageContentType=application/xml&ediMessageType=application/edifact")
                                            .to("as2://client/send?ediMessageType=application/edifact&ediMessageContentType=application/xml");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void as2WithoutDependencyIsUntouched() {
        rewriteRun(
                spec -> spec.expectedCyclesThatMakeChanges(CamelTestUtil.isRecipeOverridden() ? 1 : 0),
                mavenProject("test-no-as2",
                        pomWith("camel-core"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.RouteBuilder;

                                public class As2Route extends RouteBuilder {
                                    @Override
                                    public void configure() {
                                        from("direct:in").to("as2:client/send?ediMessageType=application/edifact");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void migrateMcpJsonJackson3WithCamelOpenAI() {
        //language=xml
        rewriteRun(
                mavenProject("test-openai-mcp",
                        pom(
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
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-openai</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.modelcontextprotocol.sdk</groupId>
                                            <artifactId>mcp-json-jackson2</artifactId>
                                            <version>2.0.1</version>
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
                                    <dependencies>
                                        <dependency>
                                            <groupId>org.apache.camel</groupId>
                                            <artifactId>camel-openai</artifactId>
                                            <version>4.23.0</version>
                                        </dependency>
                                        <dependency>
                                            <groupId>io.modelcontextprotocol.sdk</groupId>
                                            <artifactId>mcp-json-jackson3</artifactId>
                                            <version>2.2.0</version>
                                        </dependency>
                                    </dependencies>
                                </project>
                                """
                        )
                )
        );
    }

    @Test
    void removeMinaSftpReadLockOptions() {
        rewriteRun(
                mavenProject("test-camel-mina-sftp",
                        pomWith("camel-mina-sftp"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class ReadLockRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(minaSftp("in").readLockRemoveOnRollback(true).readLockIdempotentReleaseAsync(true).delay(5000))
                                            .to("mock:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class ReadLockRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(minaSftp("in").delay(5000))
                                            .to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void removeAzureFilesReadLockOptions() {
        rewriteRun(
                mavenProject("test-camel-azure-files",
                        pomWith("camel-azure-files"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class ReadLockRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(azureFiles("in").readLockRemoveOnRollback(true).readLockDeleteOrphanLockFiles(false).delay(5000))
                                            .to("mock:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class ReadLockRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(azureFiles("in").delay(5000))
                                            .to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }

    @Test
    void removeSmbReadLockOptions() {
        rewriteRun(
                mavenProject("test-camel-smb",
                        pomWith("camel-smb"),
                        //language=java
                        java(
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class ReadLockRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(smb("in").readLockRemoveOnRollback(true).readLockRemoveOnCommit(true).delay(5000))
                                            .to("mock:out");
                                    }
                                }
                                """,
                                """
                                import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

                                public class ReadLockRoute extends EndpointRouteBuilder {
                                    @Override
                                    public void configure() {
                                        from(smb("in").delay(5000))
                                            .to("mock:out");
                                    }
                                }
                                """
                        )
                )
        );
    }
}
