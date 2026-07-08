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

/**
 * A minimal, Teiid-owned abstraction of the source-specific SQL needed to
 * create the temporary tables used for dependent-join pushdown. This used to
 * be a pruned view over a Hibernate {@code Dialect}; it is now fully
 * decoupled from Hibernate and implemented natively by the execution
 * factories.
 */
public interface SQLDialect {

    //TODO: there's a chance that the type is not supported by the source
    //which will throw an exception - this is likely a modeling error
    //rather than something we need to generally consider
    public String getTypeName(int code, long length, int precision, int scale);

    /**
     * @return true if this source supports the temporary tables used for
     *         dependent joins.
     */
    public boolean supportsTemporaryTables();

    /**
     * @return the command used to create a temporary table, e.g.
     *         {@code create local temporary table}.
     */
    public String getCreateTemporaryTableString();

    /**
     * @return the options appended after the temporary table definition, e.g.
     *         {@code on commit delete rows}. May be an empty string.
     */
    public String getCreateTemporaryTablePostfix();

    /**
     * @return the command used to drop a temporary table, e.g.
     *         {@code drop table}.
     */
    public String getDropTemporaryTableString();

}
