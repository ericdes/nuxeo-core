/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DialectFactory;
import org.hibernate.exception.SQLExceptionConverter;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 *
 * @author Florent Guillaume
 */
public class Dialect {

    private final org.hibernate.dialect.Dialect dialect;

    private final String name;

    private final char openQuote;

    private final char closeQuote;

    /**
     * Creats a {@code Dialect} by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    public Dialect(Connection connection) throws StorageException {
        String dbname;
        int dbmajor;
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            dbname = metadata.getDatabaseProductName();
            dbmajor = metadata.getDatabaseMajorVersion();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        try {
            dialect = DialectFactory.determineDialect(dbname, dbmajor);
        } catch (HibernateException e) {
            throw new StorageException("Cannot determine dialect for: " +
                    connection, e);
        }
        name = dialect.getClass().getSimpleName();
        openQuote = dialect.openQuote();
        closeQuote = dialect.closeQuote();
    }

    @Override
    public String toString() {
        return name;
    }

    public char openQuote() {
        return openQuote;
    }

    public char closeQuote() {
        return closeQuote;
    }

    public SQLExceptionConverter buildSQLExceptionConverter() {
        return dialect.buildSQLExceptionConverter();
    }

    public String toBooleanValueString(boolean bool) {
        return dialect.toBooleanValueString(bool);
    }

    public String getIdentitySelectString(String table, String column,
            int sqlType) {
        return dialect.getIdentitySelectString(table, column, sqlType);
    }

    public boolean hasDataTypeInIdentityColumn() {
        return dialect.hasDataTypeInIdentityColumn();
    }

    public String getIdentityColumnString(int sqlType) {
        return dialect.getIdentityColumnString(sqlType);
    }

    public String getTypeName(int sqlType, int length, int precision, int scale) {
        String typeName;
        if (dialect instanceof DerbyDialect && sqlType == Types.CLOB) {
            typeName = "clob"; // skip size
        } else {
            typeName = dialect.getTypeName(sqlType, length, precision, scale);
        }
        return typeName;
    }

    public String getNoColumnsInsertString() {
        return dialect.getNoColumnsInsertString();
    }

    public String getNullColumnString() {
        return dialect.getNullColumnString();
    }

    // this is just for MySQL to add its ENGINE=InnoDB
    public String getTableTypeString() {
        return dialect.getTableTypeString();
    }

    public String getAddPrimaryKeyConstraintString(String constraintName) {
        return dialect.getAddPrimaryKeyConstraintString(constraintName);
    }

    public String getAddForeignKeyConstraintString(String constraintName,
            String[] foreignKeys, String referencedTable, String[] primaryKeys,
            boolean referencesPrimaryKey) {
        return dialect.getAddForeignKeyConstraintString(constraintName,
                foreignKeys, referencedTable, primaryKeys, referencesPrimaryKey);
    }

    public boolean qualifyIndexName() {
        return dialect.qualifyIndexName();
    }

    public boolean supportsIfExistsBeforeTableName() {
        return dialect.supportsIfExistsBeforeTableName();
    }

    public boolean supportsIfExistsAfterTableName() {
        return dialect.supportsIfExistsAfterTableName();
    }

    public String getCascadeConstraintsString() {
        return dialect.getCascadeConstraintsString();
    }

}