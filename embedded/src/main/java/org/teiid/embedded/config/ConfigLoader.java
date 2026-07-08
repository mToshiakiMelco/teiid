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

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.teiid.embedded.EmbeddedBootstrap;
import org.teiid.embedded.TeiidRegistry;
import org.teiid.embedded.config.EmbeddedYamlConfig.DataSourceConfig;
import org.teiid.embedded.config.EmbeddedYamlConfig.TranslatorConfig;
import org.teiid.embedded.config.EmbeddedYamlConfig.VdbConfig;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.translator.ExecutionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Parses a YAML descriptor ({@link EmbeddedYamlConfig}) and applies it to an embedded server:
 * registers translators and data sources, then deploys the described VDBs. Paths in the descriptor
 * (ddlFile / xmlFile) are resolved relative to the descriptor's own location.
 */
public class ConfigLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    /** Parse a YAML descriptor from a file. */
    public static EmbeddedYamlConfig parse(Path yamlFile) throws Exception {
        try (InputStream is = java.nio.file.Files.newInputStream(yamlFile)) {
            return YAML.readValue(is, EmbeddedYamlConfig.class);
        }
    }

    /** Parse a YAML descriptor from a stream. */
    public static EmbeddedYamlConfig parse(InputStream is) throws Exception {
        return YAML.readValue(is, EmbeddedYamlConfig.class);
    }

    /**
     * Start an embedded server from a YAML descriptor file and return the running bootstrap.
     * Relative ddlFile/xmlFile references are resolved against the descriptor's directory.
     */
    public static EmbeddedBootstrap load(Path yamlFile) throws Exception {
        EmbeddedYamlConfig config = parse(yamlFile);
        Path baseDir = yamlFile.toAbsolutePath().getParent();
        return load(config, baseDir);
    }

    public static EmbeddedBootstrap load(EmbeddedYamlConfig config, Path baseDir) throws Exception {
        EmbeddedConfiguration ec = EmbeddedBootstrap.defaultConfiguration();
        if (config.server != null) {
            ec.setUseDisk(config.server.useDisk);
        }
        EmbeddedBootstrap boot = EmbeddedBootstrap.start(ec);
        apply(config, boot.getRegistry(), baseDir);
        return boot;
    }

    /** Apply translators, data sources and VDBs from the descriptor onto an already-started registry. */
    public static void apply(EmbeddedYamlConfig config, TeiidRegistry registry, Path baseDir) throws Exception {
        for (TranslatorConfig t : config.translators) {
            Class<?> clazz = Class.forName(t.clazz);
            ExecutionFactory<?, ?> ef = (ExecutionFactory<?, ?>) clazz.getDeclaredConstructor().newInstance();
            ef.start();
            registry.registerTranslator(t.name, ef);
        }
        for (DataSourceConfig d : config.dataSources) {
            Class<?> clazz = Class.forName(d.clazz);
            Object cf = clazz.getDeclaredConstructor().newInstance();
            registry.registerDataSource(d.name, cf);
        }
        for (VdbConfig v : config.vdbs) {
            if (v.ddl != null && !v.ddl.isEmpty()) {
                registry.deployVdbFromDdl(v.name, v.ddl);
            } else if (v.ddlFile != null) {
                registry.deployVdbFromDdlFile(v.name, resolve(baseDir, v.ddlFile));
            } else if (v.xmlFile != null) {
                registry.deployVdbFromXmlFile(v.name, resolve(baseDir, v.xmlFile));
            } else {
                throw new IllegalArgumentException("VDB '" + v.name + "' has no ddl, ddlFile or xmlFile");
            }
        }
    }

    private static Path resolve(Path baseDir, String maybeRelative) {
        Path p = Paths.get(maybeRelative);
        if (p.isAbsolute() || baseDir == null) {
            return p;
        }
        return baseDir.resolve(p);
    }
}
