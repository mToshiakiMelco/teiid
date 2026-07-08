/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teiid.embedded.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Root of the YAML descriptor used to configure an embedded Teiid instance. Example:
 *
 * <pre>
 * server:
 *   useDisk: false
 * translators:
 *   - name: file
 *     class: org.teiid.translator.file.FileExecutionFactory
 * vdbs:
 *   - name: demo
 *     ddl: |
 *       CREATE DATABASE demo VERSION '1';
 *       ...
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddedYamlConfig {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServerConfig {
        public boolean useDisk = false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TranslatorConfig {
        /** Logical name the translator is registered under (and referenced by source models). */
        public String name;
        /** Fully-qualified {@link org.teiid.translator.ExecutionFactory} class name. */
        public String clazz;

        // Support the friendlier YAML key "class" (a reserved word in Java) as well as "clazz".
        @com.fasterxml.jackson.annotation.JsonProperty("class")
        public void setClassName(String className) {
            this.clazz = className;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataSourceConfig {
        public String name;
        /** Fully-qualified class name of a connection factory to instantiate via its no-arg constructor. */
        public String clazz;

        @com.fasterxml.jackson.annotation.JsonProperty("class")
        public void setClassName(String className) {
            this.clazz = className;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VdbConfig {
        public String name;
        /** Inline DDL body ({@code CREATE DATABASE ...}). Mutually exclusive with ddlFile/xmlFile. */
        public String ddl;
        /** Path to a {@code -vdb.ddl} file. */
        public String ddlFile;
        /** Path to a {@code -vdb.xml} file. */
        public String xmlFile;
    }

    public ServerConfig server = new ServerConfig();
    public List<TranslatorConfig> translators = new ArrayList<>();
    public List<DataSourceConfig> dataSources = new ArrayList<>();
    public List<VdbConfig> vdbs = new ArrayList<>();
}
