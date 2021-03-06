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
package org.apache.tinkerpop.gremlin.process.graph.traversal.step.filter;

import org.apache.tinkerpop.gremlin.process.Traversal;
import org.apache.tinkerpop.gremlin.process.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.process.traverser.TraverserRequirement;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
public final class IsStep<S> extends FilterStep<S> {

    private final Object value;
    private final BiPredicate<S, Object> biPredicate;

    public IsStep(final Traversal.Admin traversal, final BiPredicate<S, Object> biPredicate, final Object value) {
        super(traversal);
        this.value = value;
        this.biPredicate = biPredicate;
    }

    @Override
    protected boolean filter(final Traverser.Admin<S> traverser) {
        return this.biPredicate.test(traverser.get(), this.value);
    }

    @Override
    public String toString() {
        return TraversalHelper.makeStepString(this, this.biPredicate, this.value);
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return Collections.singleton(TraverserRequirement.OBJECT);
    }

    public Object getValue() {
        return value;
    }

    public BiPredicate<S, Object> getPredicate() {
        return biPredicate;
    }
}
