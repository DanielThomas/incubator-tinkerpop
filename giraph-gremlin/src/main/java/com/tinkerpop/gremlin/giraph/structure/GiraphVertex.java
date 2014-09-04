package com.tinkerpop.gremlin.giraph.structure;

import com.tinkerpop.gremlin.giraph.process.computer.util.GiraphComputerHelper;
import com.tinkerpop.gremlin.giraph.process.graph.step.sideEffect.GiraphGraphStep;
import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.StartStep;
import com.tinkerpop.gremlin.process.graph.step.util.IdentityStep;
import com.tinkerpop.gremlin.process.graph.util.DefaultGraphTraversal;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Compare;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.MetaProperty;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.HasContainer;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;
import com.tinkerpop.gremlin.tinkergraph.structure.TinkerVertex;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GiraphVertex extends GiraphElement implements Vertex, Serializable, WrappedVertex<TinkerVertex> {

    protected GiraphVertex() {
    }

    @Override
    public <V> MetaProperty<V> property(final String key) {
        return ((Vertex) this.element).property(key);
    }

    @Override
    public <V> MetaProperty<V> property(final String key, final V value) {
        throw Element.Exceptions.propertyAdditionNotSupported();
    }

    public GiraphVertex(final TinkerVertex vertex, final GiraphGraph graph) {
        super(vertex, graph);
    }

    @Override
    public Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        throw Vertex.Exceptions.edgeAdditionsNotSupported();
    }

    @Override
    public GraphTraversal<Vertex, Vertex> start() {
        final GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<Vertex, Vertex>(this.graph) {
            @Override
            public GraphTraversal<Vertex, Vertex> submit(final GraphComputer computer) {

                GiraphComputerHelper.prepareTraversalForComputer(this);
                final String label = this.getSteps().get(0).getLabel();
                TraversalHelper.removeStep(0, this);
                final Step identityStep = new IdentityStep(this);
                if (TraversalHelper.isLabeled(label))
                    identityStep.setLabel(label);

                TraversalHelper.insertStep(identityStep, 0, this);
                TraversalHelper.insertStep(new HasStep(this, new HasContainer(Element.ID, Compare.EQUAL, element.id())), 0, this);
                TraversalHelper.insertStep(new GiraphGraphStep<>(this, Vertex.class, graph), 0, this);

                return super.submit(computer);
            }
        };
        return (GraphTraversal) traversal.addStep(new StartStep<>(traversal, this));
    }

    @Override
    public TinkerVertex getBaseVertex() {
        return (TinkerVertex) this.element;
    }

    @Override
    public Vertex.Iterators iterators() {
        return this.iterators;
    }

    private final Vertex.Iterators iterators = new Iterators(this);

    protected class Iterators implements Vertex.Iterators {

        private final GiraphVertex vertex;

        public Iterators(final GiraphVertex vertex) {
            this.vertex = vertex;
        }

        // TODO: if GiraphHelper is not needed, then just call direct TinkerVertex methods
        @Override
        public Iterator<Vertex> vertices(final Direction direction, final int branchFactor, final String... labels) {
            return GiraphHelper.getVertices(graph, this.vertex, direction, branchFactor, labels);
        }

        @Override
        public Iterator<Edge> edges(final Direction direction, final int branchFactor, final String... labels) {
            return GiraphHelper.getEdges(graph, this.vertex, direction, branchFactor, labels);
        }

        @Override
        public <V> Iterator<MetaProperty<V>> properties(final String... propertyKeys) {
            return this.vertex.getBaseVertex().iterators().properties(propertyKeys);
        }

        @Override
        public <V> Iterator<MetaProperty<V>> hiddens(final String... propertyKeys) {
            return this.vertex.getBaseVertex().iterators().hiddens(propertyKeys);
        }
    }
}
