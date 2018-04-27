package org.apache.calcite.adapter.jdbc;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Statistic;

public class JdbcTableDavide extends JdbcTable {
  JdbcTableDavide(JdbcSchema jdbcSchema, String jdbcCatalogName, String jdbcSchemaName, String tableName, Schema.TableType jdbcTableType) {
    super(jdbcSchema, jdbcCatalogName, jdbcSchemaName, tableName, jdbcTableType);
  }


  /**
   * TODO
   * Davide> Overriders AbstractTable.getStatistics()
   * @return Return pre-computed statistics
   *
   * Allora, quando carico questa JdbcWhatever,
   * la istruisco anche che deve eseguire certe queries, cosi' da
   * calcolare quello che mi serve
   */
  @Override
  public Statistic getStatistic(){
    return null;
  }
}
