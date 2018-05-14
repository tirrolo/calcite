package org.apache.calcite.adapter.jdbc;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.StatisticsDavide;

public class JdbcTableWithStatsDavide extends JdbcTable {

  private StatisticsDavide stats;

  JdbcTableWithStatsDavide(JdbcSchemaWithStatsDavide jdbcSchema, JdbcTable table,
                           StatisticsDavide stats) {


    super(jdbcSchema.getDecoration(), jdbcSchema.getCatalog(), jdbcSchema.getSchema(), table.tableName().toString(), table.getJdbcTableType());
    this.stats = stats;
  }


  /**
   *
   * Davide> Overriders AbstractTable.getStatistics()
   * @return Return pre-computed statistics
   *
   *
   */
  @Override
  public Statistic getStatistic(){
    return this.stats;
  }
}
