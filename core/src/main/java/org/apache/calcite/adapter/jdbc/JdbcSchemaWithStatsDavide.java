package org.apache.calcite.adapter.jdbc;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.StatisticsDavide;
import org.apache.calcite.schema.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

/**
 * I duplicate here private fields from JdbcSchema that I need
 * to actually manipulate
 */
public class JdbcSchemaWithStatsDavide extends SchemaDecoratorDavide {

  protected SchemaPlus parentSchema;
  protected String name;
  protected DataSource dataSource;
  protected String catalog;
  protected String schema;

  // Internal (incremental) Map

  protected Map<String, JdbcTableWithStatsDavide> map;
  protected StatsGrepper grepper;

  public JdbcSchemaWithStatsDavide(Schema decorated, SchemaPlus parentSchema,
                                   String name,
                                   DataSource dataSource,
                                   String catalog,
                                   String schema) {
    super(decorated);

    grepper = new StatsGrepper(parentSchema, name, dataSource, catalog, schema);
    this.parentSchema = parentSchema;
    this.name = name;
    this.dataSource = dataSource;
    this.catalog = catalog;
    this.schema = schema;
  }

  @Override
  public Table getTable(String name) {
    if (!map.containsKey(name)) {
      computeTable(name);
    }
    return map.get(name);
  }

  /**
   * It populates the Map
   */
  protected void computeTable(String name) {
    Table table = super.getTable(name); // In practice, a JdbcTable
    StatisticsDavide stats = this.grepper.getStatisticsObject();



  }
}
