/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.structure.strategy;

import org.apache.tinkerpop.gremlin.AbstractGremlinTest;
import org.apache.tinkerpop.gremlin.FeatureRequirement;
import org.apache.tinkerpop.gremlin.FeatureRequirementSet;
import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.process.T;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.StreamFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.javatuples.Pair;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_PROPERTIES;
import static org.apache.tinkerpop.gremlin.structure.Graph.Features.VariableFeatures.FEATURE_VARIABLES;
import static org.junit.Assert.*;

/**
 * Tests that ensure proper wrapping of {@link org.apache.tinkerpop.gremlin.structure.Graph} classes.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@RunWith(Enclosed.class)
public class StrategyGraphTest {

    public static class CoreTest extends AbstractGremlinTest {
        @Test(expected = IllegalArgumentException.class)
        public void shouldNotAllowAStrategyWrappedGraphToBeReWrapped() {
            final StrategyGraph swg = new StrategyGraph(graph);
            new StrategyGraph(swg);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldNotAllowAStrategyWrappedGraphToBeReWrappedViaStrategy() {
            final StrategyGraph swg = new StrategyGraph(graph);
            swg.strategy(IdentityStrategy.instance());
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES)
        public void shouldHaveGraphWrappedFromVertex() {
            final StrategyGraph swg = new StrategyGraph(graph);
            assertTrue(swg.addVertex().graph() instanceof StrategyGraph);
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.EdgeFeatures.class, feature = Graph.Features.EdgeFeatures.FEATURE_ADD_EDGES)
        @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES)
        public void shouldHaveGraphWrappedFromEdge() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final Vertex v = swg.addVertex();
            assertTrue(v.addEdge("self", v).graph() instanceof StrategyGraph);
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_ADD_VERTICES)
        @FeatureRequirement(featureClass = Graph.Features.VertexPropertyFeatures.class, feature = Graph.Features.VertexPropertyFeatures.FEATURE_ADD_PROPERTY)
        @FeatureRequirement(featureClass = Graph.Features.VertexPropertyFeatures.class, feature = FEATURE_PROPERTIES)
        public void shouldHaveGraphWrappedFromVertexProperty() {
            final StrategyGraph swg = new StrategyGraph(graph);
            assertTrue(swg.addVertex().property("name", "stephen").graph() instanceof StrategyGraph);
        }
    }

    @RunWith(Parameterized.class)
    public static class ToStringConsistencyTest extends AbstractGremlinTest {

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            return new ArrayList<Object[]>() {{
                add(new Object[]{IdentityStrategy.instance()});
                add(new Object[]{IdStrategy.build("key").create()});
                add(new Object[]{PartitionStrategy.<PartitionStrategy.Builder>build().partitionKey("partition").startPartition("A").create()});
                add(new Object[]{ReadOnlyStrategy.instance()});
                add(new Object[]{SequenceStrategy.build().sequence(ReadOnlyStrategy.instance(), PartitionStrategy.build().partitionKey("partition").startPartition("A").create()).create()});
                add(new Object[]{SubgraphStrategy.build().create()});
            }};
        }

        @Parameterized.Parameter(value = 0)
        public GraphStrategy strategy;

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
        public void shouldReturnWrappedVertexToString() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final StrategyVertex v1 = (StrategyVertex) swg.addVertex(T.label, "Person");
            assertEquals(StringFactory.graphStrategyElementString(v1), v1.toString());
        }

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldReturnWrappedEdgeToString() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final Vertex v1 = swg.addVertex(T.label, "Person");
            final Vertex v2 = swg.addVertex(T.label, "Person");
            final StrategyEdge e1 = (StrategyEdge) v1.addEdge("friend", v2);
            assertEquals(StringFactory.graphStrategyElementString(e1), e1.toString());
        }

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
        public void shouldReturnWrappedVertexPropertyToString() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final Vertex v1 = swg.addVertex(T.label, "Person", "age", "one");
            final StrategyVertexProperty age = (StrategyVertexProperty) v1.property("age");
            assertEquals(StringFactory.graphStrategyElementString(age), age.toString());
        }

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldReturnWrappedPropertyToString() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final Vertex v1 = swg.addVertex(T.label, "Person");
            final Vertex v2 = swg.addVertex(T.label, "Person");
            final Edge e1 = v1.addEdge("friend", v2, "weight", "fifty");
            final StrategyProperty weight = (StrategyProperty) e1.property("weight");
            assertEquals(StringFactory.graphStrategyPropertyString(weight), weight.toString());
        }

        @Test
        public void shouldReturnWrappedGraphToString() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final GraphStrategy strategy = swg.getStrategy();
            assertNotEquals(g.toString(), swg.toString());
            assertEquals(StringFactory.graphStrategyString(strategy, graph), swg.toString());
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = FEATURE_VARIABLES)
        public void shouldReturnWrappedVariablesToString() {
            final StrategyGraph swg = new StrategyGraph(graph);
            final StrategyVariables variables = (StrategyVariables) swg.variables();
            assertEquals(StringFactory.graphStrategyVariables(variables), variables.toString());
        }
    }

    public static class BlockBaseFunctionTest extends AbstractGremlinTest {
        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldNotCallBaseFunctionThusNotRemovingTheVertex() throws Exception {
            // create an ad-hoc strategy that only marks a vertex as "deleted" and removes all edges and properties
            // but doesn't actually blow it away
            final StrategyGraph swg = graph.strategy(new GraphStrategy() {
                @Override
                public UnaryOperator<Supplier<Void>> getRemoveVertexStrategy(final StrategyContext<StrategyVertex> ctx, final GraphStrategy composingStrategy) {
                    return (t) -> () -> {
                        final Vertex v = ctx.getCurrent().getBaseVertex();
                        v.edges(Direction.BOTH).forEachRemaining(Edge::remove);
                        v.properties().forEachRemaining(Property::remove);
                        v.property("deleted", true);
                        return null;
                    };
                }
            });

            final Vertex toRemove = graph.addVertex("name", "pieter");
            toRemove.addEdge("likes", graph.addVertex("feature", "Strategy"));

            assertEquals(1, IteratorUtils.count(toRemove.properties()));
            assertEquals(1, IteratorUtils.count(toRemove.edges(Direction.BOTH)));
            assertFalse(toRemove.property("deleted").isPresent());

            swg.vertices(toRemove.id()).forEachRemaining(Vertex::remove);

            final Vertex removed = g.V(toRemove.id()).next();
            assertNotNull(removed);
            assertEquals(1, IteratorUtils.count(removed.properties()));
            assertEquals(0, IteratorUtils.count(removed.edges(Direction.BOTH)));
            assertTrue(removed.property("deleted").isPresent());
        }

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldNotCallBaseFunctionThusNotRemovingTheEdge() throws Exception {
            // create an ad-hoc strategy that only marks a vertex as "deleted" and removes all edges and properties
            // but doesn't actually blow it away
            final StrategyGraph swg = graph.strategy(new GraphStrategy() {
                @Override
                public UnaryOperator<Supplier<Void>> getRemoveEdgeStrategy(final StrategyContext<StrategyEdge> ctx, final GraphStrategy composingStrategy) {
                    return (t) -> () -> {
                        final Edge e = ctx.getCurrent().getBaseEdge();
                        e.properties().forEachRemaining(Property::remove);
                        e.property("deleted", true);
                        return null;
                    };
                }
            });

            final Vertex v = graph.addVertex("name", "pieter");
            final Edge e = v.addEdge("likes", graph.addVertex("feature", "Strategy"), "this", "something");

            assertEquals(1, IteratorUtils.count(e.properties()));
            assertFalse(e.property("deleted").isPresent());

            swg.edges(e.id()).forEachRemaining(Edge::remove);

            final Edge removed = g.E(e.id()).next();
            assertNotNull(removed);
            assertEquals(1, IteratorUtils.count(removed.properties()));
            assertTrue(removed.property("deleted").isPresent());
        }

        @Test
        public void shouldAdHocTheCloseStrategy() throws Exception {
            final AtomicInteger counter = new AtomicInteger(0);
            final StrategyGraph swg = graph.strategy(new GraphStrategy() {
                @Override
                public UnaryOperator<Supplier<Void>> getGraphCloseStrategy(final StrategyContext<StrategyGraph> ctx, final GraphStrategy composingStrategy) {
                    return (t) -> () -> {
                        counter.incrementAndGet();
                        return null;
                    };
                }
            });

            // allows multiple calls to close() - the test will clean up with a call to the base graph.close()
            swg.close();
            swg.close();
            swg.close();

            assertEquals(3, counter.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class EdgePropertyShouldBeWrappedTests extends AbstractGremlinTest {

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, Edge, Stream<Property<Object>>>>> tests = new ArrayList<>();
            tests.add(Pair.with("e.property(all)", (Graph g, Edge e) -> Stream.of(e.property("all"))));
            tests.add(Pair.with("e.properties()", (Graph g, Edge e) -> StreamFactory.stream(e.properties())));
            tests.add(Pair.with("e.properties(any)", (Graph g, Edge e) -> StreamFactory.stream(e.properties("any"))));
            tests.add(Pair.with("e.properties()", (Graph g, Edge e) -> StreamFactory.stream(e.properties())));
            tests.add(Pair.with("e.property(extra,more)", (Graph g, Edge e) -> Stream.<Property<Object>>of(e.property("extra", "more"))));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, Edge, Stream<? extends Property<Object>>> streamGetter;

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());
            final Vertex v = swg.addVertex();
            final Edge e = v.addEdge("to", v, "all", "a", "any", "something", "hideme", "hidden");

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(swg, e).allMatch(p -> {
                atLeastOne.set(true);
                return p instanceof StrategyProperty;
            }));

            assertTrue(atLeastOne.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class VertexPropertyPropertyShouldBeWrappedTests extends AbstractGremlinTest {

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, VertexProperty, Stream<Property<Object>>>>> tests = new ArrayList<>();
            tests.add(Pair.with("vp.property(food)", (Graph g, VertexProperty vp) -> Stream.of(vp.property("food"))));
            tests.add(Pair.with("vp.property(moreFood,sandwhich)", (Graph g, VertexProperty vp) -> Stream.of(vp.property("moreFood", "sandwhich"))));
            tests.add(Pair.with("vp.properties()", (Graph g, VertexProperty vp) -> StreamFactory.stream(vp.properties())));
            tests.add(Pair.with("vp.properties(food)", (Graph g, VertexProperty vp) -> StreamFactory.stream(vp.properties("food"))));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, VertexProperty, Stream<? extends Property<Object>>> streamGetter;

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
        @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_META_PROPERTIES)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());
            final Vertex v = swg.addVertex();
            final VertexProperty vp = v.property("property", "on-property", "food", "taco", "more", "properties");

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(swg, vp).allMatch(p -> {
                atLeastOne.set(true);
                return p instanceof StrategyProperty;
            }));

            assertTrue(atLeastOne.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class VertexPropertyShouldBeWrappedTest extends AbstractGremlinTest {
        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, Vertex, Stream<VertexProperty<Object>>>>> tests = new ArrayList<>();
            tests.add(Pair.with("v.property(all)", (Graph g, Vertex v) -> Stream.of(v.property("all"))));
            tests.add(Pair.with("v.property(extra, data)", (Graph g, Vertex v) -> Stream.of(v.<Object>property("extra", "data"))));
            tests.add(Pair.with("v.properties()", (Graph g, Vertex v) -> StreamFactory.stream(v.properties())));
            tests.add(Pair.with("v.properties(any)", (Graph g, Vertex v) -> StreamFactory.stream(v.properties("any"))));
            tests.add(Pair.with("v.properties()", (Graph g, Vertex v) -> StreamFactory.stream(v.properties())));
            tests.add(Pair.with("v.property(extra,more)", (Graph g, Vertex v) -> Stream.<VertexProperty<Object>>of(v.property("extra", "more"))));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, Vertex, Stream<? extends Property<Object>>> streamGetter;

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());
            final Vertex v = swg.addVertex("all", "a", "any", "something", "hideme", "hidden");

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(graph, v).allMatch(p -> {
                atLeastOne.set(true);
                return p instanceof StrategyVertexProperty;
            }));

            assertTrue(atLeastOne.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class VertexPropertyWithMultiPropertyShouldBeWrappedTest extends AbstractGremlinTest {
        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, Vertex, Stream<VertexProperty<Object>>>>> tests = new ArrayList<>();
            tests.add(Pair.with("v.property(all)", (Graph g, Vertex v) -> Stream.of(v.property("all"))));
            tests.add(Pair.with("v.property(extra, data)", (Graph g, Vertex v) -> Stream.of(v.<Object>property("extra", "data"))));
            tests.add(Pair.with("v.properties()", (Graph g, Vertex v) -> StreamFactory.stream(v.properties())));
            tests.add(Pair.with("v.properties(any)", (Graph g, Vertex v) -> StreamFactory.stream(v.properties("any"))));
            tests.add(Pair.with("v.properties()", (Graph g, Vertex v) -> StreamFactory.stream(v.properties())));
            tests.add(Pair.with("v.property(extra,more)", (Graph g, Vertex v) -> Stream.<VertexProperty<Object>>of(v.property("extra", "more"))));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, Vertex, Stream<? extends Property<Object>>> streamGetter;

        @Test
        @FeatureRequirementSet(FeatureRequirementSet.Package.SIMPLE)
        @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_MULTI_PROPERTIES)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());
            final Vertex v = swg.addVertex("all", "a", "any", "something", "any", "something-else", "hideme", "hidden", "hideme", "hidden-too");

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(graph, v).allMatch(p -> {
                atLeastOne.set(true);
                return p instanceof StrategyVertexProperty;
            }));

            assertTrue(atLeastOne.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class EdgeShouldBeWrappedTest extends AbstractGremlinTest {
        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, AbstractGremlinTest, Stream<Edge>>>> tests = new ArrayList<>();
            tests.add(Pair.with("g.E()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().E())));
            tests.add(Pair.with("g.edges", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.edges())));
            tests.add(Pair.with("g.edges(11)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.edges(instance.convertToEdgeId("josh", "created", "lop")))));
            tests.add(Pair.with("g.E(11)", (Graph g, AbstractGremlinTest instance) -> Stream.of(g.traversal().E(instance.convertToEdgeId("josh", "created", "lop")).next())));
            tests.add(Pair.with("g.V(1).outE()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).outE())));
            tests.add(Pair.with("g.V(4).bothE()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).bothE())));
            tests.add(Pair.with("g.V(4).inE()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).inE())));
            tests.add(Pair.with("g.V(11).property(weight).element()", (Graph g, AbstractGremlinTest instance) -> Stream.of((Edge) g.traversal().E(instance.convertToEdgeId("josh", "created", "lop")).next().property("weight").element())));
            tests.add(Pair.with("g.V(4).next().edges(Direction.BOTH)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).next().edges(Direction.BOTH))));
            tests.add(Pair.with("g.V(1).next().edges(Direction.OUT)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).next().edges(Direction.OUT))));
            tests.add(Pair.with("g.V(4).next().edges(Direction.IN)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).next().edges(Direction.IN))));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, AbstractGremlinTest, Stream<Edge>> streamGetter;

        @Test
        @LoadGraphWith(LoadGraphWith.GraphData.MODERN)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(swg, this).allMatch(e -> {
                atLeastOne.set(true);
                return e instanceof StrategyEdge;
            }));

            assertTrue(atLeastOne.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class VertexShouldBeWrappedTest extends AbstractGremlinTest {
        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, AbstractGremlinTest, Stream<Vertex>>>> tests = new ArrayList<>();
            tests.add(Pair.with("g.V()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V())));
            tests.add(Pair.with("g.vertices()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.vertices())));
            tests.add(Pair.with("g.vertices(1)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.vertices(instance.convertToVertexId("marko")))));
            tests.add(Pair.with("g.V(1)", (Graph g, AbstractGremlinTest instance) -> Stream.of(g.traversal().V(instance.convertToVertexId("marko")).next())));
            tests.add(Pair.with("g.V(1).outE().inV()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).outE().inV())));
            tests.add(Pair.with("g.V(4).bothE().bothV()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).bothE().bothV())));
            tests.add(Pair.with("g.V(4).inE().outV()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).inE().outV())));
            tests.add(Pair.with("g.V(1).out()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).out())));
            tests.add(Pair.with("g.V(4).vertices(Direction.BOTH)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).next().vertices(Direction.BOTH))));
            tests.add(Pair.with("g.V(1).vertices(Direction.OUT)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).next().vertices(Direction.OUT))));
            tests.add(Pair.with("g.V(4).vertices(Direction.IN)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).next().vertices(Direction.IN))));
            tests.add(Pair.with("g.V(4).verticesr(Direction.BOTH, \"created\")", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).next().vertices(Direction.BOTH, "created"))));
            tests.add(Pair.with("g.E(11).vertices(Direction.IN)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().E(instance.convertToEdgeId("josh", "created", "lop")).next().vertices(Direction.IN))));
            tests.add(Pair.with("g.E(11).vertices(Direction.OUT)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().E(instance.convertToEdgeId("josh", "created", "lop")).next().vertices(Direction.OUT))));
            tests.add(Pair.with("g.E(11).vertices(Direction.BOTH)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().E(instance.convertToEdgeId("josh", "created", "lop")).next().vertices(Direction.BOTH))));
            tests.add(Pair.with("g.V(1).properties(name).next().element()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).next().properties("name").next().element())));
            tests.add(Pair.with("g.V(1).outE().otherV()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).outE().otherV())));
            tests.add(Pair.with("g.V(4).inE().otherV()", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("josh")).inE().otherV())));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, AbstractGremlinTest, Stream<Vertex>> streamGetter;

        @Test
        @LoadGraphWith(LoadGraphWith.GraphData.MODERN)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(swg, this).allMatch(v -> {
                atLeastOne.set(true);
                return v instanceof StrategyVertex;
            }));

            assertTrue(atLeastOne.get());
        }
    }

    @RunWith(Parameterized.class)
    public static class GraphShouldBeWrappedTest extends AbstractGremlinTest {
        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> data() {
            final List<Pair<String, BiFunction<Graph, AbstractGremlinTest, Stream<Graph>>>> tests = new ArrayList<>();
            tests.add(Pair.with("g.V(1).graph()", (Graph g, AbstractGremlinTest instance) -> Stream.of(g.traversal().V(instance.convertToVertexId("marko")).next().graph())));
            tests.add(Pair.with("g.V(1).edges(BOTH).map(Edge::graph)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).next().edges(Direction.BOTH)).map(Edge::graph)));
            tests.add(Pair.with("g.V(1).properties().map(Edge::graph)", (Graph g, AbstractGremlinTest instance) -> StreamFactory.stream(g.traversal().V(instance.convertToVertexId("marko")).next().properties()).map(VertexProperty::graph)));

            return tests.stream().map(d -> {
                final Object[] o = new Object[2];
                o[0] = d.getValue0();
                o[1] = d.getValue1();
                return o;
            }).collect(Collectors.toList());
        }

        @Parameterized.Parameter(value = 0)
        public String name;

        @Parameterized.Parameter(value = 1)
        public BiFunction<Graph, AbstractGremlinTest, Stream<Graph>> streamGetter;

        @Test
        @LoadGraphWith(LoadGraphWith.GraphData.CREW)
        public void shouldWrap() {
            final StrategyGraph swg = graph.strategy(IdentityStrategy.instance());

            final AtomicBoolean atLeastOne = new AtomicBoolean(false);
            assertTrue(streamGetter.apply(swg, this).allMatch(v -> {
                atLeastOne.set(true);
                return v instanceof StrategyGraph;
            }));

            assertTrue(atLeastOne.get());
        }
    }
}
