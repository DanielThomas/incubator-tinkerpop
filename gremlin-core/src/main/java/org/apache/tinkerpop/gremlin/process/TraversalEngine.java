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
package org.apache.tinkerpop.gremlin.process;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.traversal.engine.ComputerTraversalEngine;
import org.apache.tinkerpop.gremlin.process.traversal.engine.StandardTraversalEngine;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.io.Serializable;
import java.util.Optional;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface TraversalEngine extends Serializable {

    public enum Type {STANDARD, COMPUTER}

    public void processTraversal(final Traversal.Admin<?, ?> traversal);

    public Type getType();

    public Optional<GraphComputer> getGraphComputer();

    public default boolean isStandard() {
        return this.getType().equals(Type.STANDARD);
    }

    public default boolean isComputer() {
        return this.getType().equals(Type.COMPUTER);
    }

    ///////////

    public interface Builder extends Serializable {
        public TraversalEngine create(final Graph graph);
    }
}
