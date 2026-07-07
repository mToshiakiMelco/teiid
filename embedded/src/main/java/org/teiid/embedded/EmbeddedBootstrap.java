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

import java.sql.Connection;
import java.util.Properties;

import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;

/**
 * Explicit bootstrap for an embedded Teiid instance.
 * <p>
 * This is the replacement for both the WildFly subsystem startup and the Spring Boot starter:
 * the {@link EmbeddedServer} is created and {@link EmbeddedServer#start(EmbeddedConfiguration) started}
 * explicitly, with no application server, no JNDI and no auto-configuration. The returned
 * {@link TeiidRegistry} is the single handle through which translators, data sources and VDBs are
 * managed, and {@link #connect(String)} yields an in-VM JDBC connection for querying.
 */
public class EmbeddedBootstrap implements AutoCloseable {

    private final EmbeddedServer server;
    private final TeiidRegistry registry;

    private EmbeddedBootstrap(EmbeddedServer server, TeiidRegistry registry) {
        this.server = server;
        this.registry = registry;
    }

    /** Start an embedded server with a sensible in-memory default configuration. */
    public static EmbeddedBootstrap start() {
        return start(defaultConfiguration());
    }

    /** Start an embedded server with the supplied configuration. */
    public static EmbeddedBootstrap start(EmbeddedConfiguration config) {
        EmbeddedServer server = new EmbeddedServer();
        server.start(config);
        return new EmbeddedBootstrap(server, new TeiidRegistry(server));
    }

    /**
     * The default MVP configuration: no disk buffering, no transaction manager (non-transactional)
     * and no socket transports, i.e. purely in-VM.
     */
    public static EmbeddedConfiguration defaultConfiguration() {
        EmbeddedConfiguration config = new EmbeddedConfiguration();
        config.setUseDisk(false);
        return config;
    }

    public TeiidRegistry getRegistry() {
        return registry;
    }

    public EmbeddedServer getServer() {
        return server;
    }

    /** Open an in-VM JDBC connection to the named VDB ({@code jdbc:teiid:<vdb>}). */
    public Connection connect(String vdbName) throws Exception {
        return connect(vdbName, null);
    }

    public Connection connect(String vdbName, Properties info) throws Exception {
        return server.getDriver().connect("jdbc:teiid:" + vdbName, info);
    }

    @Override
    public void close() {
        server.stop();
    }
}
