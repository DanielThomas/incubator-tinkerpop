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
package org.apache.tinkerpop.gremlin.neo4j;

import org.apache.tinkerpop.gremlin.AbstractGraphProvider;
import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jEdge;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jElement;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraphVariables;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jProperty;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jVertex;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jVertexProperty;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.commons.configuration.Configuration;
import org.neo4j.graphdb.DynamicLabel;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractNeo4jGraphProvider extends AbstractGraphProvider {
    private static final Set<Class> IMPLEMENTATIONS = new HashSet<Class>() {{
        add(Neo4jEdge.class);
        add(Neo4jElement.class);
        add(Neo4jGraph.class);
        add(Neo4jGraphVariables.class);
        add(Neo4jProperty.class);
        add(Neo4jVertex.class);
        add(Neo4jVertexProperty.class);
    }};

    @Override
    public void clear(final Graph graph, final Configuration configuration) throws Exception {
        if (null != graph) {
            if (graph.features().graph().supportsTransactions() && graph.tx().isOpen())
                graph.tx().rollback();
            graph.close();
        }

        if (configuration.containsKey("gremlin.neo4j.directory")) {
            // this is a non-in-sideEffects configuration so blow away the directory
            final File graphDirectory = new File(configuration.getString("gremlin.neo4j.directory"));
            deleteDirectory(graphDirectory);
        }
    }

    @Override
    public void loadGraphData(final Graph graph, final LoadGraphWith loadGraphWith, final Class testClass, final String testName) {
        if (loadGraphWith != null) this.createIndices((Neo4jGraph) graph, loadGraphWith.value());
        super.loadGraphData(graph, loadGraphWith, testClass, testName);
    }

    private void createIndices(final Neo4jGraph g, final LoadGraphWith.GraphData graphData) {
        final Random random = new Random();
        final int pick = random.nextInt(3);
        //final int pick = 2;
        if (graphData.equals(LoadGraphWith.GraphData.GRATEFUL)) {
            if (pick == 1) {
                g.tx().readWrite();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("artist")).on("name").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("song")).on("name").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("song")).on("songType").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("song")).on("performances").create();
                g.tx().commit();
            } else if (pick == 2) {
                g.tx().readWrite();
                g.getBaseGraph().index().getNodeAutoIndexer().setEnabled(true);
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("name");
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("songType");
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("performances");
                g.tx().commit();
            }
        } else if (graphData.equals(LoadGraphWith.GraphData.MODERN)) {
            if (pick == 1) {
                g.tx().readWrite();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("person")).on("name").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("person")).on("age").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("software")).on("name").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("software")).on("lang").create();
                g.tx().commit();
            } else if (pick == 2) {
                g.tx().readWrite();
                g.getBaseGraph().index().getNodeAutoIndexer().setEnabled(true);
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("name");
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("age");
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("lang");
                g.tx().commit();
            }
        } else if (graphData.equals(LoadGraphWith.GraphData.CLASSIC)) {
            if (pick == 1) {
                g.tx().readWrite();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("vertex")).on("name").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("vertex")).on("age").create();
                if (random.nextBoolean())
                    g.getBaseGraph().schema().indexFor(DynamicLabel.label("vertex")).on("lang").create();
                g.tx().commit();
            } else if (pick == 2) {
                g.tx().readWrite();
                g.getBaseGraph().index().getNodeAutoIndexer().setEnabled(true);
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("name");
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("age");
                if (random.nextBoolean())
                    g.getBaseGraph().index().getNodeAutoIndexer().startAutoIndexingProperty("lang");
                g.tx().commit();
            }
        } else {
            // TODO: add CREW work here.
            // TODO: add meta_property indices when meta_property graph is provided
            //throw new RuntimeException("Could not load graph with " + graphData);
        }
    }

    @Override
    public Set<Class> getImplementations() {
        return IMPLEMENTATIONS;
    }
}
