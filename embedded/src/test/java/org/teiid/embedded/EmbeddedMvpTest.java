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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;
import org.teiid.embedded.config.ConfigLoader;
import org.teiid.embedded.config.EmbeddedYamlConfig;

public class EmbeddedMvpTest {

    /** Boot from an inline DDL VDB via the registry and query it over in-VM JDBC. */
    @Test public void testDeployDdlAndQuery() throws Exception {
        String ddl = "CREATE DATABASE demo VERSION '1';"
                + "USE DATABASE demo VERSION '1';"
                + "CREATE VIRTUAL SCHEMA app;"
                + "SET SCHEMA app;"
                + "CREATE VIEW helloworld AS SELECT 'HELLO WORLD' AS greeting;";

        try (EmbeddedBootstrap boot = EmbeddedBootstrap.start()) {
            boot.getRegistry().deployVdbFromDdl("demo", ddl);
            assertTrue(boot.getRegistry().listVdbs().contains("demo"));

            try (Connection c = boot.connect("demo");
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("select * from helloworld")) {
                assertTrue(rs.next());
                assertEquals("HELLO WORLD", rs.getString(1));
            }
        }
    }

    /** Boot from the bundled YAML descriptor and query it end-to-end (the CLI's code path). */
    @Test public void testYamlConfigLoadAndQuery() throws Exception {
        Path yaml = extractResource("/examples/hello.yaml");
        EmbeddedYamlConfig config = ConfigLoader.parse(yaml);
        assertEquals("demo", config.vdbs.get(0).name);

        try (EmbeddedBootstrap boot = ConfigLoader.load(yaml)) {
            try (Connection c = boot.connect("demo");
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("select greeting from helloworld")) {
                assertTrue(rs.next());
                assertEquals("HELLO WORLD", rs.getString("greeting"));
            }
        }
    }

    private static Path extractResource(String classpathResource) throws Exception {
        Path tmp = java.nio.file.Files.createTempFile("hello", ".yaml");
        try (InputStream is = EmbeddedMvpTest.class.getResourceAsStream(classpathResource)) {
            java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        tmp.toFile().deleteOnExit();
        return tmp;
    }
}
