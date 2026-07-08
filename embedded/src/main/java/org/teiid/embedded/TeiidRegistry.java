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

package org.teiid.embedded;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.ExecutionFactory;

/**
 * A self-contained registry for the artifacts that make up an embedded Teiid instance:
 * translators, data sources (connection factories) and deployed VDBs.
 * <p>
 * This intentionally replaces the WildFly / JNDI based lookup model. Nothing here relies on
 * a JNDI naming context or a WildFly subsystem: translators and data sources are plain objects
 * registered by name and handed to the {@link EmbeddedServer}. The registry keeps track of what
 * has been registered so callers can introspect the running instance without going through an
 * application server management API.
 */
public class TeiidRegistry {

    private final EmbeddedServer server;

    // Track registered names so the instance can be introspected without an app-server admin API.
    private final Map<String, ExecutionFactory<?, ?>> translators = new LinkedHashMap<>();
    private final Map<String, Object> dataSources = new LinkedHashMap<>();
    private final Map<String, String> vdbs = new LinkedHashMap<>();

    public TeiidRegistry(EmbeddedServer server) {
        this.server = server;
    }

    /** The underlying embedded server, for advanced use (e.g. obtaining the JDBC driver). */
    public EmbeddedServer getServer() {
        return server;
    }

    // ---- Translators ---------------------------------------------------------------------

    /** Register an already-configured {@link ExecutionFactory} instance under the given name. */
    public void registerTranslator(String name, ExecutionFactory<?, ?> translator) {
        server.addTranslator(name, translator);
        translators.put(name, translator);
    }

    /**
     * Register a translator by its {@link ExecutionFactory} class. The name is taken from the
     * {@code @Translator} annotation on the class.
     */
    public void registerTranslatorClass(Class<? extends ExecutionFactory> clazz) throws Exception {
        server.addTranslator(clazz);
        // Name is derived from the @Translator annotation; record the class name for introspection.
        translators.put(clazz.getSimpleName(), null);
    }

    public Set<String> listTranslators() {
        return Collections.unmodifiableSet(translators.keySet());
    }

    // ---- Data sources (connection factories) ---------------------------------------------

    /**
     * Register a data source (connection factory) under the given name. This is the embedded
     * replacement for a JNDI-bound {@code javax.sql.DataSource} / JCA connection factory: the
     * object is looked up by this name when a VDB source model references the connection.
     */
    public void registerDataSource(String name, Object connectionFactory) {
        server.addConnectionFactory(name, connectionFactory);
        dataSources.put(name, connectionFactory);
    }

    public Set<String> listDataSources() {
        return Collections.unmodifiableSet(dataSources.keySet());
    }

    // ---- VDBs ----------------------------------------------------------------------------

    /** Deploy a VDB from an inline DDL string ({@code CREATE DATABASE ...}). */
    public void deployVdbFromDdl(String name, String ddl) throws Exception {
        InputStream is = new ByteArrayInputStream(ddl.getBytes(StandardCharsets.UTF_8));
        server.deployVDB(is, true);
        vdbs.put(name, "ddl");
    }

    /** Deploy a VDB from a {@code -vdb.ddl} file on disk. */
    public void deployVdbFromDdlFile(String name, Path ddlFile) throws Exception {
        deployVdbFromDdl(name, new String(Files.readAllBytes(ddlFile), StandardCharsets.UTF_8));
    }

    /** Deploy a VDB from a {@code -vdb.xml} file on disk. */
    public void deployVdbFromXmlFile(String name, Path xmlFile) throws Exception {
        try (InputStream is = Files.newInputStream(xmlFile)) {
            server.deployVDB(is);
        }
        vdbs.put(name, "xml");
    }

    public void undeployVdb(String name) {
        server.undeployVDB(name);
        vdbs.remove(name);
    }

    public Set<String> listVdbs() {
        return Collections.unmodifiableSet(vdbs.keySet());
    }
}
