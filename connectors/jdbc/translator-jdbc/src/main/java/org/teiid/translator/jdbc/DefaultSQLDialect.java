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

package org.teiid.translator.jdbc;

import java.sql.Types;

/**
 * The default, ANSI-flavored {@link SQLDialect}. Sources with different
 * temporary-table syntax (for example Oracle) subclass this and override the
 * relevant methods.
 */
public class DefaultSQLDialect implements SQLDialect {

    private final String createTemporaryTableString;
    private final String createTemporaryTablePostfix;

    public DefaultSQLDialect() {
        this("create local temporary table", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public DefaultSQLDialect(String createTemporaryTableString, String createTemporaryTablePostfix) {
        this.createTemporaryTableString = createTemporaryTableString;
        this.createTemporaryTablePostfix = createTemporaryTablePostfix;
    }

    @Override
    public String getTypeName(int code, long length, int precision, int scale) {
        switch (code) {
        case Types.CHAR:
            return "char(" + clamp(length) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
            return "varchar(" + clamp(length) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        case Types.BOOLEAN:
        case Types.BIT:
            return "boolean"; //$NON-NLS-1$
        case Types.TINYINT:
            return "tinyint"; //$NON-NLS-1$
        case Types.SMALLINT:
            return "smallint"; //$NON-NLS-1$
        case Types.INTEGER:
            return "integer"; //$NON-NLS-1$
        case Types.BIGINT:
            return "bigint"; //$NON-NLS-1$
        case Types.REAL:
        case Types.FLOAT:
            return "float"; //$NON-NLS-1$
        case Types.DOUBLE:
            return "double precision"; //$NON-NLS-1$
        case Types.NUMERIC:
        case Types.DECIMAL:
            return "decimal(" + precision + "," + scale + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        case Types.DATE:
            return "date"; //$NON-NLS-1$
        case Types.TIME:
            return "time"; //$NON-NLS-1$
        case Types.TIMESTAMP:
            return "timestamp"; //$NON-NLS-1$
        case Types.BLOB:
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            return "blob"; //$NON-NLS-1$
        case Types.CLOB:
        case Types.NCLOB:
            return "clob"; //$NON-NLS-1$
        default:
            return "varchar(" + clamp(length) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static long clamp(long length) {
        return length <= 0 ? 255 : length;
    }

    @Override
    public boolean supportsTemporaryTables() {
        return true;
    }

    @Override
    public String getCreateTemporaryTableString() {
        return createTemporaryTableString;
    }

    @Override
    public String getCreateTemporaryTablePostfix() {
        return createTemporaryTablePostfix;
    }

    @Override
    public String getDropTemporaryTableString() {
        return "drop table"; //$NON-NLS-1$
    }
}
