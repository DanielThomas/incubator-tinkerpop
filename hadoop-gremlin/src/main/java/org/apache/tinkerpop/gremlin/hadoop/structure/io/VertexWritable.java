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
package org.apache.tinkerpop.gremlin.hadoop.structure.io;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.giraph.GiraphWorkerContext;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class VertexWritable implements Writable {

    private Vertex vertex;

    public VertexWritable() {

    }

    // TODO: REMOVE!!!
    public VertexWritable(final Vertex vertex) {
        this.vertex = vertex;
    }

    public void set(final Vertex vertex) {
        this.vertex = vertex;
    }

    public Vertex get() {
        return this.vertex;
    }

    @Override
    public void readFields(final DataInput input) throws IOException {
        try {
            this.vertex = null;
            this.vertex = GiraphWorkerContext.GRYO_POOL.doWithReader(gryoReader -> {
                try {
                    final ByteArrayInputStream inputStream = new ByteArrayInputStream(WritableUtils.readCompressedByteArray(input));
                    final Graph gLocal = TinkerGraph.open();
                    return gryoReader.readVertex(inputStream, Direction.BOTH,
                            detachedVertex -> DetachedVertex.addTo(gLocal, detachedVertex),
                            detachedEdge -> DetachedEdge.addTo(gLocal, detachedEdge));
                } catch (final IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            });
        } catch (final IllegalStateException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            else
                throw e;
        }
    }

    @Override
    public void write(final DataOutput output) throws IOException {
        try {
            GiraphWorkerContext.GRYO_POOL.doWithWriter(gryoWriter -> {
                try {
                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    gryoWriter.writeVertex(outputStream, this.vertex, Direction.BOTH);
                    WritableUtils.writeCompressedByteArray(output, outputStream.toByteArray());
                } catch (final IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            });
        } catch (final IllegalStateException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            else
                throw e;
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof VertexWritable && ElementHelper.areEqual(this.vertex, ((VertexWritable) other).get());
    }

    @Override
    public int hashCode() {
        return this.vertex.hashCode();
    }

    @Override
    public String toString() {
        return this.vertex.toString();
    }
}
