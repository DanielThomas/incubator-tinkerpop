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
package org.apache.tinkerpop.gremlin.hadoop.structure.util;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.InputOutputHelper;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class HadoopHelper {

    private HadoopHelper() {
    }

    public static HadoopGraph getOutputGraph(final HadoopGraph hadoopGraph, boolean hasVertexProgram) {
        final BaseConfiguration newConfiguration = new BaseConfiguration();
        newConfiguration.copy(hadoopGraph.configuration());
        if (hasVertexProgram) {
            newConfiguration.setProperty(Constants.GREMLIN_HADOOP_INPUT_LOCATION, hadoopGraph.configuration().getOutputLocation() + "/" + Constants.HIDDEN_G);
            newConfiguration.setProperty(Constants.GREMLIN_HADOOP_GRAPH_INPUT_FORMAT, InputOutputHelper.getInputFormat(hadoopGraph.configuration().getGraphOutputFormat()).getCanonicalName());
            newConfiguration.setProperty(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, hadoopGraph.configuration().getOutputLocation() + "_");
        } else {
            newConfiguration.setProperty(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, hadoopGraph.configuration().getOutputLocation() + "_");
        }
        return HadoopGraph.open(newConfiguration);
    }

}
