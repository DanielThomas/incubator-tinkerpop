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
package org.apache.tinkerpop.gremlin.neo4j.structure;

import org.apache.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;
import org.apache.tinkerpop.gremlin.neo4j.BaseNeo4jGraphTest;
import org.apache.tinkerpop.gremlin.process.T;
import org.apache.tinkerpop.gremlin.process.graph.traversal.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.graph.traversal.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Contains;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.util.StreamFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.tooling.GlobalGraphOperations;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * These are tests specific to Neo4j.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Pieter Martin
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class Neo4jGraphTest extends BaseNeo4jGraphTest {

    @Test
    public void shouldOpenWithOverriddenConfig() throws Exception {
        assertNotNull(this.graph);
    }

    @Test
    public void shouldNotThrowConcurrentModificationException() {
        this.graph.addVertex("name", "a");
        this.graph.addVertex("name", "b");
        this.graph.addVertex("name", "c");
        this.graph.addVertex("name", "d");
        this.graph.vertices().forEachRemaining(Vertex::remove);
        this.graph.tx().commit();
        assertEquals(0, IteratorUtils.count(this.graph.vertices()), 0);
    }

    /**
     * Neo4j upgrades from 1.x don't come with labels.
     */
    @Test
    public void shouldTraverseWithoutLabels() {
        final GraphDatabaseService service = graph.getBaseGraph();

        final Transaction tx = service.beginTx();
        final Node n = service.createNode();
        tx.success();
        tx.close();

        final Transaction tx2 = service.beginTx();
        assertEquals(0, IteratorUtils.count(n.getLabels().iterator()));
        assertEquals(1, IteratorUtils.count(graph.vertices()));
        graph.tx().close();
        tx2.close();
    }

    @Test
    public void shouldReturnResultsLabeledIndexOnVertexWithHasHas() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertEquals(2, this.g.V().has(T.label, "Person").has("name", "marko").count().next(), 0);
        assertEquals(2, this.g.V().has("name", "marko").count().next(), 0);
    }

    @Test
    public void shouldEnsureColonedKeyIsTreatedAsNormalKey() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertEquals(2, this.g.V().has(T.label, "Person").has("name", "marko").count().next(), 0);
        assertEquals(0, this.g.V().has("Person:name", "marko").count().next(), 0);

    }

    @Test
    public void shouldReturnResultsUsingLabeledIndexOnVertexWithHasHasHas() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko", "color", "blue");
        this.graph.addVertex(T.label, "Person", "name", "marko", "color", "green");
        this.graph.tx().commit();
        assertEquals(1, this.g.V().has(T.label, "Person").has("name", "marko").has("color", "blue").count().next(), 0);
        assertEquals(2, this.g.V().has("name", "marko").count().next(), 0);
    }

    @Test
    public void shouldReturnResultsOnVertexWithHasHasHasNoIndex() {
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko", "color", "blue");
        this.graph.addVertex(T.label, "Person", "name", "marko", "color", "green");
        this.graph.tx().commit();
        assertEquals(1, this.g.V().has(T.label, "Person").has("name", "marko").has("color", "blue").count().next(), 0);
        assertEquals(2, this.g.V().has("name", "marko").count().next(), 0);
    }

    @Test
    public void shouldReturnResultsUsingLabeledIndexOnVertexWithColonFails() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertNotEquals(2l, this.g.V().has("Person:name", "marko").count().next().longValue());
        assertEquals(2, this.g.V().has("name", "marko").count().next(), 0);
    }

    @Test
    public void shouldReturnResultsUsingLegacyIndexOnVertex() {
        graph.tx().readWrite();
        final AutoIndexer<Node> nodeAutoIndexer = this.graph.getBaseGraph().index().getNodeAutoIndexer();
        nodeAutoIndexer.startAutoIndexingProperty("name");
        this.graph.tx().commit();

        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertEquals(2, this.g.V().has("Person", "name", "marko").count().next(), 0);
        assertEquals(2, this.g.V().has("name", "marko").count().next(), 0);
    }

    @Test
    public void shouldUseLegacyIndexOnEdge() {
        graph.tx().readWrite();
        final AutoIndexer<Relationship> relAutoIndexer = this.graph.getBaseGraph().index().getRelationshipAutoIndexer();
        relAutoIndexer.startAutoIndexingProperty("weight");
        this.graph.tx().commit();

        Vertex marko = this.graph.addVertex(T.label, "Person", "name", "marko");
        Vertex john = this.graph.addVertex(T.label, "Person", "name", "john");
        Vertex pete = this.graph.addVertex(T.label, "Person", "name", "pete");
        marko.addEdge("friend", john, "weight", "a");
        marko.addEdge("friend", pete, "weight", "a");
        this.graph.tx().commit();
        assertEquals(2, this.g.E().has("weight", "a").count().next(), 0);
    }

    @Test
    public void shouldEnforceUniqueConstraint() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.constraintFor(DynamicLabel.label("Person")).assertPropertyIsUnique("name").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertEquals("marko", g.V().has(T.label, "Person").has("name", "marko").next().value("name"));
    }

    @Test
    public void shouldEnforceMultipleUniqueConstraint() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.constraintFor(DynamicLabel.label("Person")).assertPropertyIsUnique("name").create();
        schema.constraintFor(DynamicLabel.label("Person")).assertPropertyIsUnique("surname").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "surname", "aaaa");
        this.graph.tx().commit();
        boolean failSurname = false;
        try {
            this.graph.addVertex(T.label, "Person", "surname", "aaaa");
        } catch (ConstraintViolationException e) {
            failSurname = true;
        }
        assertTrue(failSurname);
        boolean failName = false;
        try {
            this.graph.addVertex(T.label, "Person", "name", "marko");
        } catch (ConstraintViolationException e) {
            failName = true;
        }
        assertTrue(failName);
        this.graph.tx().commit();
    }

    @Test
    public void shouldDropMultipleUniqueConstraint() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.constraintFor(DynamicLabel.label("Person")).assertPropertyIsUnique("name").create();
        schema.constraintFor(DynamicLabel.label("Person")).assertPropertyIsUnique("surname").create();
        this.graph.tx().commit();

        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "surname", "aaaa");
        this.graph.tx().commit();
        boolean failSurname = false;
        try {
            this.graph.addVertex(T.label, "Person", "surname", "aaaa");
        } catch (ConstraintViolationException e) {
            failSurname = true;
        }
        assertTrue(failSurname);
        boolean failName = false;
        try {
            this.graph.addVertex(T.label, "Person", "name", "marko");
        } catch (ConstraintViolationException e) {
            failName = true;
        }
        assertTrue(failName);
        this.graph.tx().commit();

        this.graph.tx().readWrite();
        for (ConstraintDefinition cd : schema.getConstraints(DynamicLabel.label("Person"))) {
            cd.drop();
        }

        this.graph.tx().commit();
        assertEquals(1, this.g.V().has(T.label, "Person").has("name", "marko").count().next(), 0);
        assertEquals(1, this.g.V().has(T.label, "Person").has("surname", "aaaa").count().next(), 0);
        this.graph.addVertex(T.label, "Person", "surname", "aaaa");
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertEquals(2, this.g.V().has(T.label, "Person").has("name", "marko").count().next(), 0);
        assertEquals(2, this.g.V().has(T.label, "Person").has("surname", "aaaa").count().next(), 0);
    }

    @Test(expected = ConstraintViolationException.class)
    public void shouldFailUniqueConstraint() {
        this.graph.tx().readWrite();
        final Schema schema = this.graph.getBaseGraph().schema();
        schema.constraintFor(DynamicLabel.label("Person")).assertPropertyIsUnique("name").create();
        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.tx().commit();
        assertEquals("marko", g.V().has(T.label, "Person").has("name", "marko").next().value("name"));
        this.graph.addVertex(T.label, "Person", "name", "marko");
    }

    @Test
    public void shouldEnsureTraverseRelationshipNeedsTx() throws ScriptException {
        final GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
        final Bindings bindings = engine.createBindings();
        bindings.put("g", graph.traversal(GraphTraversalSource.standard));
        bindings.put("#jsr223.groovy.engine.keep.globals", "phantom");

        Vertex marko = this.graph.addVertex(T.label, "Person", "name", "marko");
        Vertex john = this.graph.addVertex(T.label, "Person", "name", "john");
        Vertex pete = this.graph.addVertex(T.label, "Person", "name", "pete");
        marko.addEdge("friend", john);
        marko.addEdge("friend", pete);
        this.graph.tx().commit();

        Object result = engine.eval("g.V(" + marko.id().toString() + ").outE('friend')", bindings);
        assertTrue(result instanceof GraphTraversal);

        this.graph.tx().commit();
        assertEquals(2L, ((GraphTraversal) result).count().next());
    }

    @Test
    public void shouldEnsureTraversalOfVerticesNeedsTx() throws ScriptException {
        final GremlinGroovyScriptEngine engine = new GremlinGroovyScriptEngine();
        final Bindings bindings = engine.createBindings();
        bindings.put("g", graph.traversal(GraphTraversalSource.standard));
        bindings.put("#jsr223.groovy.engine.keep.globals", "phantom");

        Vertex marko = this.graph.addVertex(T.label, "Person", "name", "marko");
        Vertex john = this.graph.addVertex(T.label, "Person", "name", "john");
        Vertex pete = this.graph.addVertex(T.label, "Person", "name", "pete");
        marko.addEdge("friend", john);
        marko.addEdge("friend", pete);
        this.graph.tx().commit();

        Object result = engine.eval("g.V(" + marko.id().toString() + ").out('friend')", bindings);
        assertTrue(result instanceof GraphTraversal);

        this.graph.tx().commit();
        assertEquals(2L, ((GraphTraversal) result).count().next());
    }

    @Test
    public void shouldDoLabelSearch() {
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "john");
        Vertex pete = this.graph.addVertex(T.label, "Person", "name", "pete");
        this.graph.addVertex(T.label, "Monkey", "name", "pete");
        this.graph.tx().commit();
        assertEquals(3, this.g.V().has(T.label, "Person").count().next(), 0);
        pete.remove();
        this.graph.tx().commit();
        assertEquals(2, this.g.V().has(T.label, "Person").count().next(), 0);
    }

    @Test
    public void shouldDoLabelAndIndexSearch() {
        graph.tx().readWrite();

        final Schema schema = graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();
        this.graph.tx().commit();

        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "john");
        this.graph.addVertex(T.label, "Person", "name", "pete");
        this.graph.tx().commit();
        assertEquals(1, this.g.V().has("Person", "name", "marko").count().next(), 0);
        assertEquals(3, this.g.V().has(T.label, "Person").count().next(), 0);
        assertEquals(1, this.g.V().has(T.label, "Person").has("name", "marko").count().next(), 0);
    }

    @Test
    public void shouldDoLabelAndLegacyIndexSearch() {
        graph.tx().readWrite();

        final Schema schema = graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();

        final AutoIndexer<Node> nodeAutoIndexer = this.graph.getBaseGraph().index().getNodeAutoIndexer();
        nodeAutoIndexer.startAutoIndexingProperty("name");

        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "john");
        this.graph.addVertex(T.label, "Person", "name", "pete");
        this.graph.tx().commit();
        assertEquals(1, this.g.V().has(T.label, "Person").has("name", "marko").count().next(), 0);
        assertEquals(3, this.g.V().has(T.label, "Person").count().next(), 0);
        assertEquals(1, this.g.V().has("name", "john").count().next(), 0);

    }

    @Test
    public void shouldSupportVertexPropertyToVertexMappingOnIndexCalls() {
        // todo: review this feature check - this test does a lot of stuff - maybe losing some important assertions this way
        if (graph.features().vertex().supportsMultiProperties()) {
            graph.tx().readWrite();
            final Schema schema = graph.getBaseGraph().schema();
            schema.indexFor(DynamicLabel.label("person")).on("name").create();
            schema.indexFor(DynamicLabel.label("name")).on(T.value.getAccessor()).create();
            this.graph.tx().commit();

            final Vertex a = graph.addVertex(T.label, "person", "name", "marko", "age", 34);
            a.property("name", "okram");
            a.property("name", "marko a. rodriguez");
            final Vertex b = graph.addVertex(T.label, "person", "name", "stephen");
            final Vertex c = graph.addVertex("name", "matthias", "name", "mbroecheler");

            tryCommit(graph, graph -> {
                assertEquals(a.id(), graph.traversal().V().has("person", "name", "okram").id().next());
                assertEquals(1, graph.traversal().V().has("person", "name", "okram").count().next().intValue());
                assertEquals(34, ((Neo4jVertex) graph.traversal().V().has("person", "name", "okram").next()).getBaseVertex().getProperty("age"));
                assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN, ((Neo4jVertex) graph.traversal().V().has("person", "name", "okram").next()).getBaseVertex().getProperty("name"));
                ///
                assertEquals(b.id(), graph.traversal().V().has("person", "name", "stephen").id().next());
                assertEquals(1, graph.traversal().V().has("person", "name", "stephen").count().next().intValue());
                assertEquals("stephen", ((Neo4jVertex) graph.traversal().V().has("person", "name", "stephen").next()).getBaseVertex().getProperty("name"));
                ///
                assertEquals(c.id(), graph.traversal().V().has("name", "matthias").id().next());
                assertEquals(c.id(), graph.traversal().V().has("name", "mbroecheler").id().next());
                assertEquals(1, graph.traversal().V().has("name", "matthias").count().next().intValue());
                assertEquals(1, graph.traversal().V().has("name", "mbroecheler").count().next().intValue());
                assertEquals(0, graph.traversal().V().has("person", "name", "matthias").count().next().intValue());
                assertEquals(0, graph.traversal().V().has("person", "name", "mbroecheler").count().next().intValue());
            });

            final Vertex d = graph.addVertex(T.label, "person", "name", "kuppitz");
            tryCommit(graph, graph -> {
                assertEquals(d.id(), graph.traversal().V().has("person", "name", "kuppitz").id().next());
                assertEquals("kuppitz", ((Neo4jVertex) graph.traversal().V().has("person", "name", "kuppitz").next()).getBaseVertex().getProperty("name"));
            });
            d.property("name", "daniel", "acl", "private");
            tryCommit(graph, graph -> {
                assertEquals(d.id(), graph.traversal().V().has("person", "name", Contains.within, Arrays.asList("daniel", "kuppitz")).id().next());
                assertEquals(d.id(), graph.traversal().V().has("person", "name", "kuppitz").id().next());
                assertEquals(d.id(), graph.traversal().V().has("person", "name", "daniel").id().next());
                assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN, ((Neo4jVertex) graph.traversal().V().has("person", "name", "kuppitz").next()).getBaseVertex().getProperty("name"));
            });
            d.property("name", "marko", "acl", "private");
            tryCommit(graph, g -> {
                assertEquals(2, g.traversal().V().has("person", "name", "marko").count().next().intValue());
                assertEquals(1, g.traversal().V().has("person", "name", "marko").properties("name").has(T.value, "marko").has("acl", "private").count().next().intValue());
                g.traversal().V().has("person", "name", "marko").forEachRemaining(v -> {
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN, ((Neo4jVertex) v).getBaseVertex().getProperty("name"));
                });

            });
        }
    }

    @Test
    public void shouldDoLabelsNamespaceBehavior() {
        graph.tx().readWrite();

        final Schema schema = graph.getBaseGraph().schema();
        schema.indexFor(DynamicLabel.label("Person")).on("name").create();
        schema.indexFor(DynamicLabel.label("Product")).on("name").create();
        schema.indexFor(DynamicLabel.label("Corporate")).on("name").create();

        this.graph.tx().commit();
        this.graph.addVertex(T.label, "Person", "name", "marko");
        this.graph.addVertex(T.label, "Person", "name", "john");
        this.graph.addVertex(T.label, "Person", "name", "pete");
        this.graph.addVertex(T.label, "Product", "name", "marko");
        this.graph.addVertex(T.label, "Product", "name", "john");
        this.graph.addVertex(T.label, "Product", "name", "pete");
        this.graph.addVertex(T.label, "Corporate", "name", "marko");
        this.graph.addVertex(T.label, "Corporate", "name", "john");
        this.graph.addVertex(T.label, "Corporate", "name", "pete");
        this.graph.tx().commit();
        assertEquals(1, this.g.V().has(T.label, "Person").has("name", "marko").has(T.label, "Person").count().next(), 0);
        assertEquals(1, this.g.V().has(T.label, "Product").has("name", "marko").has(T.label, "Product").count().next(), 0);
        assertEquals(1, this.g.V().has(T.label, "Corporate").has("name", "marko").has(T.label, "Corporate").count().next(), 0);
        assertEquals(0, this.g.V().has(T.label, "Person").has("name", "marko").has(T.label, "Product").count().next(), 0);
        assertEquals(0, this.g.V().has(T.label, "Product").has("name", "marko").has(T.label, "Person").count().next(), 0);
        assertEquals(0, this.g.V().has(T.label, "Corporate").has("name", "marko").has(T.label, "Person").count().next(), 0);
    }

    @Test
    public void shouldNotGenerateVerticesOrEdgesForGraphVariables() {
        graph.tx().readWrite();
        graph.variables().set("namespace", "rdf-xml");
        tryCommit(graph, graph -> {
            assertEquals("rdf-xml", graph.variables().get("namespace").get());
            assertEquals(0, graph.traversal().V().count().next().intValue());
            assertEquals(0, graph.traversal().E().count().next().intValue());
            assertEquals(0, StreamFactory.stream(GlobalGraphOperations.at(graph.getBaseGraph()).getAllNodes()).count());
            assertEquals(0, StreamFactory.stream(GlobalGraphOperations.at(graph.getBaseGraph()).getAllRelationships()).count());
        });
    }

    @Test
    public void shouldNotGenerateNodesAndRelationshipsForNoMultiPropertiesNoMetaProperties() {
        graph.tx().readWrite();
        tryCommit(graph, g -> validateCounts(g, 0, 0, 0, 0));
        Vertex vertex = graph.addVertex(T.label, "person");
        tryCommit(graph, g -> validateCounts(g, 1, 0, 1, 0));
        vertex.property("name", "marko");
        assertEquals("marko", vertex.value("name"));
        tryCommit(graph, g -> validateCounts(g, 1, 0, 1, 0));
        vertex.property("name", "okram");
        tryCommit(graph, g -> {
            validateCounts(g, 1, 0, 1, 0);
            assertEquals("okram", vertex.value("name"));
        });
        VertexProperty vertexProperty = vertex.property("name");
        tryCommit(graph, g -> {
            assertTrue(vertexProperty.isPresent());
            assertEquals("name", vertexProperty.key());
            assertEquals("okram", vertexProperty.value());
            validateCounts(g, 1, 0, 1, 0);
        });
        try {
            vertexProperty.property("acl", "private");
        } catch (UnsupportedOperationException e) {
            assertEquals(VertexProperty.Exceptions.metaPropertiesNotSupported().getMessage(), e.getMessage());
        }
    }

    @Test
    public void shouldNotGenerateNodesAndRelationshipsForMultiPropertiesWithSingle() {
        graph.tx().readWrite();
        tryCommit(graph, g -> validateCounts(g, 0, 0, 0, 0));
        Vertex vertex = graph.addVertex(T.label, "person");
        tryCommit(graph, g -> validateCounts(g, 1, 0, 1, 0));
        vertex.property("name", "marko");
        assertEquals("marko", vertex.value("name"));
        tryCommit(graph, g -> validateCounts(g, 1, 0, 1, 0));
        vertex.property(VertexProperty.Cardinality.single, "name", "okram");
        tryCommit(graph, g -> {
            validateCounts(g, 1, 0, 1, 0);
            assertEquals("okram", vertex.value("name"));
        });
        VertexProperty vertexProperty = vertex.property("name");
        tryCommit(graph, g -> {
            assertTrue(vertexProperty.isPresent());
            assertEquals("name", vertexProperty.key());
            assertEquals("okram", vertexProperty.value());
            validateCounts(g, 1, 0, 1, 0);
        });

        // now make it a meta property (and thus, force node/relationship creation)
        vertexProperty.property("acl", "private");
        tryCommit(graph, g -> {
            assertEquals("private", vertexProperty.value("acl"));
            validateCounts(g, 1, 0, 2, 1);
        });

    }


    @Test
    public void shouldGenerateNodesAndRelationshipsCorrectlyForVertexProperties() {
        // todo: review this feature check - this test does a lot of stuff - maybe losing some important assertions this way
        if (graph.features().vertex().supportsMultiProperties()) {
            graph.tx().readWrite();
            ExecutionEngine cypher = Neo4jHelper.getCypher(graph);
            Neo4jVertex a = (Neo4jVertex) graph.addVertex("name", "marko", "name", "okram");
            Neo4jVertex b = (Neo4jVertex) graph.addVertex("name", "stephen", "location", "virginia");

            tryCommit(graph, graph -> {
                assertEquals(2, graph.traversal().V().count().next().intValue());
                // assertEquals(2, a.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("location").count().next().intValue());
                // assertEquals(0, g.E().count().next().intValue());

                assertEquals(4l, cypher.execute("MATCH n RETURN COUNT(n)").iterator().next().get("COUNT(n)"));
                assertEquals(2l, cypher.execute("MATCH (n)-[r]->(m) RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                assertEquals(2l, cypher.execute("MATCH (a)-[r]->() WHERE id(a) = " + a.id() + " RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                final AtomicInteger counter = new AtomicInteger(0);
                a.getBaseVertex().getRelationships(Direction.OUTGOING).forEach(relationship -> {
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat("name"), relationship.getType().name());
                    counter.incrementAndGet();
                });
                assertEquals(2, counter.getAndSet(0));
                cypher.execute("MATCH (a)-[]->(m) WHERE id(a) = " + a.id() + " RETURN labels(m)").forEach(results -> {
                    assertEquals(VertexProperty.DEFAULT_LABEL, ((List<String>) results.get("labels(m)")).get(0));
                    counter.incrementAndGet();
                });
                assertEquals(2, counter.getAndSet(0));
                StreamFactory.stream(a.getBaseVertex().getRelationships(Direction.OUTGOING)).map(Relationship::getEndNode).forEach(node -> {
                    assertEquals(2, StreamFactory.stream(node.getPropertyKeys()).count());
                    assertEquals("name", node.getProperty(T.key.getAccessor()));
                    assertTrue("marko".equals(node.getProperty(T.value.getAccessor())) || "okram".equals(node.getProperty(T.value.getAccessor())));
                    assertEquals(0, node.getDegree(Direction.OUTGOING));
                    assertEquals(1, node.getDegree(Direction.INCOMING));
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat("name"), node.getRelationships(Direction.INCOMING).iterator().next().getType().name());
                    counter.incrementAndGet();
                });
                assertEquals(2, counter.getAndSet(0));

                assertEquals(2, StreamFactory.stream(b.getBaseVertex().getPropertyKeys()).count());
                assertEquals("stephen", b.getBaseVertex().getProperty("name"));
                assertEquals("virginia", b.getBaseVertex().getProperty("location"));
            });

            a.property(VertexProperty.Cardinality.single, "name", "the marko");
            tryCommit(graph, g -> {
                assertEquals(2, g.traversal().V().count().next().intValue());
                //assertEquals(1, a.properties().count().next().intValue());
                //  assertEquals(1, b.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("location").count().next().intValue());
                //  assertEquals(0, g.E().count().next().intValue());

                assertEquals(2l, cypher.execute("MATCH n RETURN COUNT(n)").iterator().next().get("COUNT(n)"));
                assertEquals(0l, cypher.execute("MATCH (n)-[r]->(m) RETURN COUNT(r)").iterator().next().get("COUNT(r)"));

                assertEquals(1, StreamFactory.stream(a.getBaseVertex().getPropertyKeys()).count());
                assertEquals("the marko", a.getBaseVertex().getProperty("name"));
                assertEquals(2, StreamFactory.stream(b.getBaseVertex().getPropertyKeys()).count());
                assertEquals("stephen", b.getBaseVertex().getProperty("name"));
                assertEquals("virginia", b.getBaseVertex().getProperty("location"));
            });

            a.property("name").remove();
            tryCommit(graph, g -> {
                assertEquals(2, g.traversal().V().count().next().intValue());
                //    assertEquals(0, a.properties().count().next().intValue());
                //   assertEquals(2, b.properties().count().next().intValue());
                //     assertEquals(0, g.E().count().next().intValue());
                assertEquals(2l, cypher.execute("MATCH n RETURN COUNT(n)").iterator().next().get("COUNT(n)"));
                assertEquals(0l, cypher.execute("MATCH (n)-[r]->(m) RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                assertEquals(0, StreamFactory.stream(a.getBaseVertex().getPropertyKeys()).count());
                assertEquals(2, StreamFactory.stream(b.getBaseVertex().getPropertyKeys()).count());
            });

            a.property(VertexProperty.Cardinality.single, "name", "the marko", "acl", "private");
            tryCommit(graph, g -> {
                assertEquals(2, g.traversal().V().count().next().intValue());
                // assertEquals(1, a.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("location").count().next().intValue());
                //  assertEquals(0, g.E().count().next().intValue());

                assertEquals(3l, cypher.execute("MATCH n RETURN COUNT(n)").iterator().next().get("COUNT(n)"));
                assertEquals(1l, cypher.execute("MATCH (n)-[r]->(m) RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                assertEquals(1l, cypher.execute("MATCH (a)-[r]->() WHERE id(a) = " + a.id() + " RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                final AtomicInteger counter = new AtomicInteger(0);
                a.getBaseVertex().getRelationships(Direction.OUTGOING).forEach(relationship -> {
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat("name"), relationship.getType().name());
                    counter.incrementAndGet();
                });
                assertEquals(1, counter.getAndSet(0));
                cypher.execute("MATCH (a)-[]->(m) WHERE id(a) = " + a.id() + " RETURN labels(m)").forEach(results -> {
                    assertEquals(VertexProperty.DEFAULT_LABEL, ((List<String>) results.get("labels(m)")).get(0));
                    counter.incrementAndGet();
                });
                assertEquals(1, counter.getAndSet(0));
                StreamFactory.stream(a.getBaseVertex().getRelationships(Direction.OUTGOING)).map(Relationship::getEndNode).forEach(node -> {
                    assertEquals(3, StreamFactory.stream(node.getPropertyKeys()).count());
                    assertEquals("name", node.getProperty(T.key.getAccessor()));
                    assertEquals("the marko", node.getProperty(T.value.getAccessor()));
                    assertEquals("private", node.getProperty("acl"));
                    assertEquals(0, node.getDegree(Direction.OUTGOING));
                    assertEquals(1, node.getDegree(Direction.INCOMING));
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat("name"), node.getRelationships(Direction.INCOMING).iterator().next().getType().name());
                    counter.incrementAndGet();
                });
                assertEquals(1, counter.getAndSet(0));

                assertEquals(1, StreamFactory.stream(a.getBaseVertex().getPropertyKeys()).count());
                assertTrue(a.getBaseVertex().hasProperty("name"));
                assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN, a.getBaseVertex().getProperty("name"));
                assertEquals(2, StreamFactory.stream(b.getBaseVertex().getPropertyKeys()).count());
                assertEquals("stephen", b.getBaseVertex().getProperty("name"));
                assertEquals("virginia", b.getBaseVertex().getProperty("location"));
            });

            a.property("name", "marko", "acl", "private");
            a.property("name", "okram", "acl", "public");
            // TODO tx.commit() THIS IS REQUIRED: ?! Why does Neo4j not delete vertices correctly?
            graph.tx().commit();
            a.property(VertexProperty.Cardinality.single, "name", "the marko", "acl", "private");
            tryCommit(graph, g -> {
                assertEquals(2, g.traversal().V().count().next().intValue());
                // assertEquals(1, a.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("name").count().next().intValue());
                // assertEquals(1, b.properties("location").count().next().intValue());
                // assertEquals(0, g.E().count().next().intValue());

                assertEquals(3l, cypher.execute("MATCH n RETURN COUNT(n)").iterator().next().get("COUNT(n)"));
                assertEquals(1l, cypher.execute("MATCH (n)-[r]->(m) RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                assertEquals(1l, cypher.execute("MATCH (a)-[r]->() WHERE id(a) = " + a.id() + " RETURN COUNT(r)").iterator().next().get("COUNT(r)"));
                final AtomicInteger counter = new AtomicInteger(0);
                a.getBaseVertex().getRelationships(Direction.OUTGOING).forEach(relationship -> {
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat("name"), relationship.getType().name());
                    counter.incrementAndGet();
                });
                assertEquals(1, counter.getAndSet(0));
                cypher.execute("MATCH (a)-[]->(m) WHERE id(a) = " + a.id() + " RETURN labels(m)").forEach(results -> {
                    assertEquals(VertexProperty.DEFAULT_LABEL, ((List<String>) results.get("labels(m)")).get(0));
                    counter.incrementAndGet();
                });
                assertEquals(1, counter.getAndSet(0));
                StreamFactory.stream(a.getBaseVertex().getRelationships(Direction.OUTGOING)).map(Relationship::getEndNode).forEach(node -> {
                    assertEquals(3, StreamFactory.stream(node.getPropertyKeys()).count());
                    assertEquals("name", node.getProperty(T.key.getAccessor()));
                    assertEquals("the marko", node.getProperty(T.value.getAccessor()));
                    assertEquals("private", node.getProperty("acl"));
                    assertEquals(0, node.getDegree(Direction.OUTGOING));
                    assertEquals(1, node.getDegree(Direction.INCOMING));
                    assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_PREFIX.concat("name"), node.getRelationships(Direction.INCOMING).iterator().next().getType().name());
                    counter.incrementAndGet();
                });
                assertEquals(1, counter.getAndSet(0));

                assertEquals(1, StreamFactory.stream(a.getBaseVertex().getPropertyKeys()).count());
                assertTrue(a.getBaseVertex().hasProperty("name"));
                assertEquals(Neo4jVertexProperty.VERTEX_PROPERTY_TOKEN, a.getBaseVertex().getProperty("name"));
                assertEquals(2, StreamFactory.stream(b.getBaseVertex().getPropertyKeys()).count());
                assertEquals("stephen", b.getBaseVertex().getProperty("name"));
                assertEquals("virginia", b.getBaseVertex().getProperty("location"));
            });
        }
    }

    @Test
    public void shouldSupportNeo4jMultiLabels() {
        final Neo4jVertex vertex = (Neo4jVertex) graph.addVertex(T.label, "animal::person", "name", "marko");
        tryCommit(graph, g -> {
            assertTrue(vertex.label().equals("animal::person"));
            assertEquals(2, vertex.labels().size());
            assertTrue(vertex.labels().contains("person"));
            assertTrue(vertex.labels().contains("animal"));
            assertEquals(2, IteratorUtils.count(vertex.getBaseVertex().getLabels().iterator()));
        });

        vertex.addLabel("organism");
        tryCommit(graph, g -> {
            assertTrue(vertex.label().equals("animal::organism::person"));
            assertEquals(3, vertex.labels().size());
            assertTrue(vertex.labels().contains("person"));
            assertTrue(vertex.labels().contains("animal"));
            assertTrue(vertex.labels().contains("organism"));
            assertEquals(3, IteratorUtils.count(vertex.getBaseVertex().getLabels().iterator()));
        });

        vertex.removeLabel("person");
        tryCommit(graph, g -> {
            assertTrue(vertex.label().equals("animal::organism"));
            assertEquals(2, vertex.labels().size());
            assertTrue(vertex.labels().contains("animal"));
            assertTrue(vertex.labels().contains("organism"));
        });

        vertex.addLabel("organism"); // repeat add
        vertex.removeLabel("person"); // repeat remove
        tryCommit(graph, g -> {
            assertTrue(vertex.label().equals("animal::organism"));
            assertEquals(2, vertex.labels().size());
            assertTrue(vertex.labels().contains("animal"));
            assertTrue(vertex.labels().contains("organism"));
            assertEquals(2, IteratorUtils.count(vertex.getBaseVertex().getLabels().iterator()));
        });

    }
}
