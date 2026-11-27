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
package org.apache.camel.upgrade.camel422_3;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.upgrade.AbstractCamelJavaVisitor;
import org.apache.camel.upgrade.AbstractCamelXmlVisitor;
import org.apache.camel.upgrade.AbstractCamelYamlVisitor;
import org.apache.camel.upgrade.RecipesUtil;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.tree.Yaml;

/**
 * Renames a header only where a route sets it: {@code setHeader} in the Java DSL and on a {@code Message},
 * {@code <setHeader name="...">} in the XML DSL and {@code setHeader: name:} in the YAML DSL. Reads of the header
 * ({@code getHeader}, {@code ${header.x}}, {@code header("x")}) are left alone.
 * <p>
 * For a header that a consumer still sets under the old name, while a producer now reads a new one (for example
 * {@code CamelPubNubChannel} and {@code CamelPubNubOverrideChannel}). In the Java DSL the first argument of
 * {@code setHeader} may be the string literal, the qualified constant ({@code constantClass} with {@code oldConstantName})
 * or the Endpoint DSL header name builder method ({@code oldHeaderNameBuilderMethod}).
 */
public class RenameHeaderWhenSet extends Recipe {

    @Option(displayName = "Old header name", description = "The header name a route sets today.",
            example = "CamelPubNubChannel")
    String oldHeaderName;

    @Option(displayName = "New header name", description = "The header name to set instead.",
            example = "CamelPubNubOverrideChannel")
    String newHeaderName;

    @Option(displayName = "Constant class", description = "Fully qualified class of the header constants.",
            example = "org.apache.camel.component.pubnub.PubNubConstants", required = false)
    String constantClass;

    @Option(displayName = "Old constant name", description = "The constant holding the old header name.",
            example = "CHANNEL", required = false)
    String oldConstantName;

    @Option(displayName = "New constant name", description = "The constant holding the new header name.",
            example = "OVERRIDE_CHANNEL", required = false)
    String newConstantName;

    @Option(displayName = "Old header name builder method",
            description = "Method pattern of the Endpoint DSL header name builder method of the old header.",
            example = "org.apache.camel.builder.endpoint.dsl.PubNubEndpointBuilderFactory.PubNubHeaderNameBuilder pubNubChannel()",
            required = false)
    String oldHeaderNameBuilderMethod;

    @Option(displayName = "New header name builder method name",
            description = "Name of the Endpoint DSL header name builder method of the new header.",
            example = "pubNubOverrideChannel", required = false)
    String newHeaderNameBuilderMethod;

    public RenameHeaderWhenSet() {
    }

    public void setOldHeaderName(String oldHeaderName) {
        this.oldHeaderName = oldHeaderName;
    }

    public void setNewHeaderName(String newHeaderName) {
        this.newHeaderName = newHeaderName;
    }

    public void setConstantClass(String constantClass) {
        this.constantClass = constantClass;
    }

    public void setOldConstantName(String oldConstantName) {
        this.oldConstantName = oldConstantName;
    }

    public void setNewConstantName(String newConstantName) {
        this.newConstantName = newConstantName;
    }

    public void setOldHeaderNameBuilderMethod(String oldHeaderNameBuilderMethod) {
        this.oldHeaderNameBuilderMethod = oldHeaderNameBuilderMethod;
    }

    public void setNewHeaderNameBuilderMethod(String newHeaderNameBuilderMethod) {
        this.newHeaderNameBuilderMethod = newHeaderNameBuilderMethod;
    }

    @Override
    public String getDisplayName() {
        return "Rename a Camel header where it is set";
    }

    @Override
    public String getDescription() {
        return "Renames a header in setHeader of the Java, XML and YAML DSLs and in Message.setHeader, but not where "
               + "the header is read.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(new JavaRecipe(this), new XmlRecipe(this), new YamlRecipe(this));
    }

    private abstract static class Part extends Recipe {
        final RenameHeaderWhenSet options;

        Part(RenameHeaderWhenSet options) {
            this.options = options;
        }

        @Override
        public String getDescription() {
            return options.getDescription();
        }
    }

    private static final class JavaRecipe extends Part {
        JavaRecipe(RenameHeaderWhenSet options) {
            super(options);
        }

        @Override
        public String getDisplayName() {
            return "Rename a header where it is set in Java";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {
                private final String MESSAGE_SET_HEADER = "org.apache.camel.Message setHeader(String, Object)";
                // matchOverrides=true: the DSL method is invoked on ProcessorDefinition subtypes (RouteDefinition, ...)
                private final MethodMatcher dslSetHeader
                        = new MethodMatcher("org.apache.camel.model.ProcessorDefinition setHeader(String, ..)", true);
                private final MethodMatcher builderMethod = options.oldHeaderNameBuilderMethod == null
                        ? null : new MethodMatcher(options.oldHeaderNameBuilderMethod);

                @Override
                protected J.MethodInvocation doVisitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation mi = super.doVisitMethodInvocation(method, ctx);
                    if (mi.getArguments().isEmpty()
                        || !(getMethodMatcher(MESSAGE_SET_HEADER).matches(mi) || dslSetHeader.matches(mi))) {
                        return mi;
                    }
                    Expression renamed = rename(mi.getArguments().get(0));
                    return renamed == mi.getArguments().get(0) ? mi : mi.withArguments(ListUtils.mapFirst(mi.getArguments(), a -> renamed));
                }

                private Expression rename(Expression arg) {
                    if (arg instanceof J.Literal literal && options.oldHeaderName.equals(literal.getValue())) {
                        return literal.withValue(options.newHeaderName).withValueSource("\"" + options.newHeaderName + "\"");
                    }
                    if (options.constantClass != null && options.oldConstantName != null && options.newConstantName != null) {
                        if (arg instanceof J.FieldAccess fa && isOldConstant(fa.getName().getFieldType())) {
                            return fa.withName(fa.getName().withSimpleName(options.newConstantName)
                                    .withFieldType(withName(fa.getName().getFieldType())));
                        }
                        // a statically imported constant is left alone: its import would need the rename too
                    }
                    if (builderMethod != null && options.newHeaderNameBuilderMethod != null
                        && arg instanceof J.MethodInvocation call && builderMethod.matches(call)) {
                        return call.withName(call.getName().withSimpleName(options.newHeaderNameBuilderMethod));
                    }
                    return arg;
                }

                private boolean isOldConstant(JavaType.Variable field) {
                    return field != null && options.oldConstantName.equals(field.getName())
                           && field.getOwner() instanceof JavaType.FullyQualified owner
                           && options.constantClass.equals(owner.getFullyQualifiedName());
                }

                private JavaType.Variable withName(JavaType.Variable field) {
                    return field.withName(options.newConstantName);
                }
            });
        }
    }

    private static final class XmlRecipe extends Part {
        XmlRecipe(RenameHeaderWhenSet options) {
            super(options);
        }

        @Override
        public String getDisplayName() {
            return "Rename a header where it is set in the XML DSL";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(RecipesUtil.camelXmlDslPrecondition(), new AbstractCamelXmlVisitor() {
                @Override
                public Xml.Tag doVisitTag(Xml.Tag tag, ExecutionContext ctx) {
                    Xml.Tag t = super.doVisitTag(tag, ctx);
                    if (!"setHeader".equals(t.getName())) {
                        return t;
                    }
                    return t.withAttributes(ListUtils.map(t.getAttributes(), attr ->
                            "name".equals(attr.getKeyAsString()) && options.oldHeaderName.equals(attr.getValueAsString())
                                    ? attr.withValue(new Xml.Attribute.Value(attr.getValue().getId(), "", Markers.EMPTY,
                                            attr.getValue().getQuote(), options.newHeaderName))
                                    : attr));
                }
            });
        }
    }

    private static final class YamlRecipe extends Part {
        YamlRecipe(RenameHeaderWhenSet options) {
            super(options);
        }

        @Override
        public String getDisplayName() {
            return "Rename a header where it is set in the YAML DSL";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(RecipesUtil.camelYamlDslPrecondition(), new AbstractCamelYamlVisitor() {
                @Override
                protected void clearLocalCache() {
                    // nothing cached
                }

                @Override
                public Yaml.Mapping.Entry doVisitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                    Yaml.Mapping.Entry e = super.doVisitMappingEntry(entry, ctx);
                    if ("name".equals(e.getKey().getValue()) && e.getValue() instanceof Yaml.Scalar scalar
                        && options.oldHeaderName.equals(scalar.getValue()) && "setHeader".equals(enclosingKey())) {
                        return e.withValue(scalar.withValue(options.newHeaderName));
                    }
                    return e;
                }

                /** Key of the nearest mapping entry enclosing the current one ({@code setHeader} for its name). */
                private String enclosingKey() {
                    Cursor c = getCursor().getParent();
                    while (c != null) {
                        if (c.getValue() instanceof Yaml.Mapping.Entry parent) {
                            return parent.getKey().getValue();
                        }
                        c = c.getParent();
                    }
                    return null;
                }
            });
        }
    }
}
