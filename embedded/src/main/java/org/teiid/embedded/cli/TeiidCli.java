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

package org.teiid.embedded.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.teiid.embedded.EmbeddedBootstrap;
import org.teiid.embedded.config.ConfigLoader;
import org.teiid.embedded.config.EmbeddedYamlConfig;

/**
 * Command line entry point for the embedded MVP.
 * <p>
 * Boots an {@link EmbeddedBootstrap} from a YAML descriptor, deploys the described VDBs and runs a
 * SQL query against one of them over an in-VM JDBC connection, printing the result set as a table.
 *
 * <pre>
 *   java -jar teiid-embedded.jar --config app.yaml --vdb demo --sql "select * from helloworld"
 *   java -jar teiid-embedded.jar --config app.yaml --interactive
 * </pre>
 */
public class TeiidCli {

    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);
        if (parsed.config == null) {
            usage(System.err);
            System.exit(2);
            return;
        }

        EmbeddedYamlConfig config = ConfigLoader.parse(Paths.get(parsed.config));
        String vdb = parsed.vdb != null ? parsed.vdb : firstVdbName(config);
        if (vdb == null) {
            System.err.println("No VDB found in " + parsed.config + " and none supplied with --vdb");
            System.exit(2);
            return;
        }

        try (EmbeddedBootstrap boot = ConfigLoader.load(Paths.get(parsed.config))) {
            System.out.println("Started embedded Teiid. VDBs=" + boot.getRegistry().listVdbs()
                    + " translators=" + boot.getRegistry().listTranslators());
            try (Connection c = boot.connect(vdb)) {
                if (parsed.sql != null) {
                    runQuery(c, parsed.sql, System.out);
                } else if (parsed.interactive) {
                    repl(c, System.out);
                } else {
                    System.err.println("Nothing to do: pass --sql \"...\" or --interactive");
                    System.exit(2);
                }
            }
        }
    }

    private static String firstVdbName(EmbeddedYamlConfig config) {
        return config.vdbs.isEmpty() ? null : config.vdbs.get(0).name;
    }

    private static void repl(Connection c, PrintStream out) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        out.println("Enter SQL statements (one per line). Blank line or 'exit' to quit.");
        String line;
        out.print("teiid> ");
        out.flush();
        while ((line = in.readLine()) != null) {
            String sql = line.trim();
            if (sql.isEmpty() || sql.equalsIgnoreCase("exit") || sql.equalsIgnoreCase("quit")) {
                break;
            }
            try {
                runQuery(c, sql, out);
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
            out.print("teiid> ");
            out.flush();
        }
    }

    private static void runQuery(Connection c, String sql, PrintStream out) throws Exception {
        try (Statement s = c.createStatement()) {
            boolean isResultSet = s.execute(sql);
            if (!isResultSet) {
                out.println("Update count: " + s.getUpdateCount());
                return;
            }
            try (ResultSet rs = s.getResultSet()) {
                printResultSet(rs, out);
            }
        }
    }

    private static void printResultSet(ResultSet rs, PrintStream out) throws Exception {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        List<String> headers = new ArrayList<>();
        for (int i = 1; i <= cols; i++) {
            headers.add(md.getColumnLabel(i));
        }
        List<List<String>> rows = new ArrayList<>();
        int count = 0;
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= cols; i++) {
                Object v = rs.getObject(i);
                row.add(v == null ? "null" : v.toString());
            }
            rows.add(row);
            count++;
        }

        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < cols; i++) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }

        printRow(headers, widths, out);
        printSeparator(widths, out);
        for (List<String> row : rows) {
            printRow(row, widths, out);
        }
        out.println("(" + count + " row" + (count == 1 ? "" : "s") + ")");
    }

    private static void printRow(List<String> cells, int[] widths, PrintStream out) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(pad(cells.get(i), widths[i]));
        }
        out.println(sb.toString());
    }

    private static void printSeparator(int[] widths, PrintStream out) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) {
                sb.append("-+-");
            }
            for (int j = 0; j < widths[i]; j++) {
                sb.append('-');
            }
        }
        out.println(sb.toString());
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void usage(PrintStream out) {
        out.println("Usage: TeiidCli --config <yaml> [--vdb <name>] (--sql \"<query>\" | --interactive)");
    }

    /** Minimal hand-rolled argument holder to avoid an external CLI dependency. */
    static class Args {
        String config;
        String vdb;
        String sql;
        boolean interactive;

        static Args parse(String[] argv) {
            Args a = new Args();
            for (int i = 0; i < argv.length; i++) {
                switch (argv[i]) {
                    case "--config":
                        a.config = argv[++i];
                        break;
                    case "--vdb":
                        a.vdb = argv[++i];
                        break;
                    case "--sql":
                        a.sql = argv[++i];
                        break;
                    case "--interactive":
                        a.interactive = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown argument: " + argv[i]);
                }
            }
            return a;
        }
    }
}
