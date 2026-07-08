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

package org.teiid.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A Hibernate {@link Dialect} for the Teiid server, reimplemented against the
 * Hibernate 6 dialect SPI.
 */
public class TeiidDialect extends Dialect {

    public TeiidDialect() {
        super(DatabaseVersion.make(1));
    }

    public TeiidDialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);
        DdlTypeRegistry ddl = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

        ddl.addDescriptor(new DdlTypeImpl(Types.CHAR, "char", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.VARCHAR, "string", this)); //$NON-NLS-1$

        ddl.addDescriptor(new DdlTypeImpl(Types.BIT, "boolean", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.TINYINT, "byte", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.SMALLINT, "short", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.INTEGER, "integer", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.BIGINT, "long", this)); //$NON-NLS-1$

        ddl.addDescriptor(new DdlTypeImpl(Types.REAL, "float", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.FLOAT, "float", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.DOUBLE, "double", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.NUMERIC, "bigdecimal", this)); //$NON-NLS-1$

        ddl.addDescriptor(new DdlTypeImpl(Types.DATE, "date", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.TIME, "time", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.TIMESTAMP, "timestamp", this)); //$NON-NLS-1$

        ddl.addDescriptor(new DdlTypeImpl(Types.BLOB, "blob", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.VARBINARY, "blob", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.CLOB, "clob", this)); //$NON-NLS-1$
        ddl.addDescriptor(new DdlTypeImpl(Types.JAVA_OBJECT, "object", this)); //$NON-NLS-1$
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
        TypeConfiguration types = functionContributions.getTypeConfiguration();

        final BasicType<?> doubleType = resolve(types, StandardBasicTypes.DOUBLE);
        final BasicType<?> stringType = resolve(types, StandardBasicTypes.STRING);
        final BasicType<?> bigDecimalType = resolve(types, StandardBasicTypes.BIG_DECIMAL);
        final BasicType<?> floatType = resolve(types, StandardBasicTypes.FLOAT);
        final BasicType<?> integerType = resolve(types, StandardBasicTypes.INTEGER);
        final BasicType<?> longType = resolve(types, StandardBasicTypes.LONG);
        final BasicType<?> characterType = resolve(types, StandardBasicTypes.CHARACTER);
        final BasicType<?> bigIntegerType = resolve(types, StandardBasicTypes.BIG_INTEGER);
        final BasicType<?> dateType = resolve(types, StandardBasicTypes.DATE);
        final BasicType<?> timeType = resolve(types, StandardBasicTypes.TIME);
        final BasicType<?> timestampType = resolve(types, StandardBasicTypes.TIMESTAMP);
        final BasicType<?> blobType = resolve(types, StandardBasicTypes.BLOB);
        final BasicType<?> clobType = resolve(types, StandardBasicTypes.CLOB);

        named(registry, "acos", doubleType); //$NON-NLS-1$
        named(registry, "asin", doubleType); //$NON-NLS-1$
        named(registry, "atan", doubleType); //$NON-NLS-1$
        named(registry, "atan2", doubleType); //$NON-NLS-1$
        registry.namedDescriptorBuilder("ceil", "ceiling").register(); //$NON-NLS-1$ //$NON-NLS-2$
        named(registry, "cos", doubleType); //$NON-NLS-1$
        named(registry, "cot", doubleType); //$NON-NLS-1$
        named(registry, "degrees", doubleType); //$NON-NLS-1$
        named(registry, "exp", doubleType); //$NON-NLS-1$
        registry.namedDescriptorBuilder("floor").register(); //$NON-NLS-1$
        named(registry, "formatbigdecimal", stringType); //$NON-NLS-1$
        named(registry, "formatbiginteger", stringType); //$NON-NLS-1$
        named(registry, "formatdouble", stringType); //$NON-NLS-1$
        named(registry, "formatfloat", stringType); //$NON-NLS-1$
        named(registry, "formatinteger", stringType); //$NON-NLS-1$
        named(registry, "formatlong", stringType); //$NON-NLS-1$
        named(registry, "log", doubleType); //$NON-NLS-1$
        registry.namedDescriptorBuilder("mod").register(); //$NON-NLS-1$
        named(registry, "parsebigdecimal", bigDecimalType); //$NON-NLS-1$
        named(registry, "parsebiginteger", bigIntegerType); //$NON-NLS-1$
        named(registry, "parsedouble", doubleType); //$NON-NLS-1$
        named(registry, "parsefloat", floatType); //$NON-NLS-1$
        named(registry, "parseinteger", integerType); //$NON-NLS-1$
        named(registry, "parselong", longType); //$NON-NLS-1$
        named(registry, "pi", doubleType); //$NON-NLS-1$
        named(registry, "power", doubleType); //$NON-NLS-1$
        named(registry, "radians", doubleType); //$NON-NLS-1$
        registry.namedDescriptorBuilder("round").register(); //$NON-NLS-1$
        named(registry, "sign", integerType); //$NON-NLS-1$
        named(registry, "sin", doubleType); //$NON-NLS-1$
        named(registry, "tan", doubleType); //$NON-NLS-1$

        named(registry, "ascii", integerType); //$NON-NLS-1$
        named(registry, "chr", characterType); //$NON-NLS-1$
        named(registry, "char", characterType); //$NON-NLS-1$
        registry.patternDescriptorBuilder("concat", "(?1||?2)").setInvariantType(stringType).register(); //$NON-NLS-1$ //$NON-NLS-2$
        named(registry, "initcap", stringType); //$NON-NLS-1$
        named(registry, "insert", stringType); //$NON-NLS-1$
        named(registry, "lcase", stringType); //$NON-NLS-1$
        named(registry, "left", stringType); //$NON-NLS-1$
        named(registry, "locate", integerType); //$NON-NLS-1$
        named(registry, "lpad", stringType); //$NON-NLS-1$
        named(registry, "ltrim", stringType); //$NON-NLS-1$
        named(registry, "repeat", stringType); //$NON-NLS-1$
        named(registry, "replace", stringType); //$NON-NLS-1$
        named(registry, "right", stringType); //$NON-NLS-1$
        named(registry, "rpad", stringType); //$NON-NLS-1$
        named(registry, "rtrim", stringType); //$NON-NLS-1$
        named(registry, "substring", stringType); //$NON-NLS-1$
        named(registry, "translate", stringType); //$NON-NLS-1$
        named(registry, "ucase", stringType); //$NON-NLS-1$

        noArgs(registry, "curdate", dateType); //$NON-NLS-1$
        noArgs(registry, "curtime", timeType); //$NON-NLS-1$
        noArgs(registry, "now", timestampType); //$NON-NLS-1$
        named(registry, "dayname", stringType); //$NON-NLS-1$
        named(registry, "dayofmonth", integerType); //$NON-NLS-1$
        named(registry, "dayofweek", integerType); //$NON-NLS-1$
        named(registry, "dayofyear", integerType); //$NON-NLS-1$
        named(registry, "formatdate", stringType); //$NON-NLS-1$
        named(registry, "formattime", stringType); //$NON-NLS-1$
        named(registry, "formattimestamp", stringType); //$NON-NLS-1$
        named(registry, "hour", integerType); //$NON-NLS-1$
        named(registry, "minute", integerType); //$NON-NLS-1$
        named(registry, "monthname", stringType); //$NON-NLS-1$
        named(registry, "parsedate", dateType); //$NON-NLS-1$
        named(registry, "parsetime", timeType); //$NON-NLS-1$
        named(registry, "parsetimestamp", timestampType); //$NON-NLS-1$
        named(registry, "second", integerType); //$NON-NLS-1$
        named(registry, "timestampcreate", timestampType); //$NON-NLS-1$
        registry.namedDescriptorBuilder("timestampAdd").register(); //$NON-NLS-1$
        named(registry, "timestampDiff", longType); //$NON-NLS-1$
        named(registry, "week", integerType); //$NON-NLS-1$
        named(registry, "year", integerType); //$NON-NLS-1$
        named(registry, "modifytimezone", timestampType); //$NON-NLS-1$

        registry.namedDescriptorBuilder("convert").register(); //$NON-NLS-1$

        named(registry, "to_bytes", blobType); //$NON-NLS-1$
        named(registry, "to_chars", clobType); //$NON-NLS-1$
        named(registry, "from_unittime", timestampType); //$NON-NLS-1$
        named(registry, "session_id", stringType); //$NON-NLS-1$

        named(registry, "uuid", stringType); //$NON-NLS-1$
        named(registry, "unescape", stringType); //$NON-NLS-1$

        registry.namedDescriptorBuilder("array_get").register(); //$NON-NLS-1$
        named(registry, "array_length", integerType); //$NON-NLS-1$
    }

    private static BasicType<?> resolve(TypeConfiguration types, BasicTypeReference<?> ref) {
        return types.getBasicTypeRegistry().resolve(ref);
    }

    private static void named(SqmFunctionRegistry registry, String name, BasicType<?> type) {
        registry.namedDescriptorBuilder(name).setInvariantType(type).register();
    }

    private static void noArgs(SqmFunctionRegistry registry, String name, BasicType<?> type) {
        registry.noArgsBuilder(name).setInvariantType(type).register();
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }

    @Override
    public boolean hasAlterTable() {
        return false;
    }

    @Override
    public boolean supportsColumnCheck() {
        return false;
    }

    @Override
    public boolean supportsCascadeDelete() {
        return false;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public boolean supportsTableCheck() {
        return false;
    }

    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    @Override
    public void appendBooleanValueString(SqlAppender appender, boolean bool) {
        appender.appendSql(bool ? "{b'true'}" : "{b'false'}"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public LimitHandler getLimitHandler() {
        return LIMIT_HANDLER;
    }

    private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
        @Override
        public boolean supportsLimit() {
            return true;
        }

        @Override
        public boolean supportsLimitOffset() {
            return true;
        }

        @Override
        public String processSql(String sql, Limit limit) {
            boolean hasOffset = limit != null && limit.getFirstRow() != null && limit.getFirstRow() > 0;
            return sql + (hasOffset ? " limit ?, ?" : " limit ?"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    };

    @Override
    public ResultSet getResultSet(CallableStatement ps) throws SQLException {
        boolean isResultSet = ps.execute();
        while (!isResultSet && ps.getUpdateCount() != -1) {
            isResultSet = ps.getMoreResults();
        }
        return ps.getResultSet();
    }

    @Override
    public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
        return col;
    }

    @Override
    public String getForUpdateNowaitString() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getForUpdateNowaitString(String aliases) {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getForUpdateString() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getForUpdateString(LockMode lockMode) {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getForUpdateString(String aliases) {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getSelectGUIDString() {
        return "select uuid()"; //$NON-NLS-1$
    }

    @Override
    public SequenceSupport getSequenceSupport() {
        return SEQUENCE_SUPPORT;
    }

    private static final SequenceSupport SEQUENCE_SUPPORT = new SequenceSupport() {
        @Override
        public String getSelectSequenceNextValString(String sequenceName) {
            return sequenceName + "_nextval()"; //$NON-NLS-1$
        }

        @Override
        public String getSequenceNextValString(String sequenceName) {
            return "select " + getSelectSequenceNextValString(sequenceName); //$NON-NLS-1$
        }
    };

    @Override
    public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(EntityMappingType rootEntityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new LocalTemporaryTableMutationStrategy(
                TemporaryTable.createIdTable(rootEntityDescriptor,
                        basename -> TemporaryTable.ID_TABLE_PREFIX + basename, this, runtimeModelCreationContext),
                runtimeModelCreationContext.getSessionFactory());
    }

    @Override
    public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(EntityMappingType rootEntityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new LocalTemporaryTableInsertStrategy(
                TemporaryTable.createEntityTable(rootEntityDescriptor,
                        basename -> TemporaryTable.ENTITY_TABLE_PREFIX + basename, this, runtimeModelCreationContext),
                runtimeModelCreationContext.getSessionFactory());
    }

    @Override
    public String getTemporaryTableCreateCommand() {
        return "create local temporary table"; //$NON-NLS-1$
    }

    @Override
    public String getTemporaryTableDropCommand() {
        return "drop table"; //$NON-NLS-1$
    }

    @Override
    public AfterUseAction getTemporaryTableAfterUseAction() {
        return AfterUseAction.DROP;
    }

    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return NameQualifierSupport.SCHEMA;
    }
}
