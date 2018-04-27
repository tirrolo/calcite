package org.apache.calcite.adapter.jdbc;

import org.apache.calcite.sql.SqlDialect;

import javax.sql.DataSource;

public class JdbcSchemaDavide extends JdbcSchema {
  /**
   * Creates a JDBC schema.
   *
   * @param dataSource Data source
   * @param dialect    SQL dialect
   * @param convention Calling convention
   * @param catalog    Catalog name, or null
   * @param schema     Schema name pattern
   */
  public JdbcSchemaDavide(DataSource dataSource, SqlDialect dialect, JdbcConvention convention, String catalog, String schema) {
    super(dataSource, dialect, convention, catalog, schema);
  }
}
