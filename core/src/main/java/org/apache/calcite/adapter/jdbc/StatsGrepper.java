package org.apache.calcite.adapter.jdbc;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.StatisticsDavide;
import org.apache.calcite.schema.StatisticsDavideImpl;

import javax.sql.DataSource;
import java.sql.*;

public class StatsGrepper {

  protected SchemaPlus parentSchema;
  protected String name;
  protected DataSource dataSource;
  protected String catalog;
  protected String schema;


  public StatsGrepper(SchemaPlus parentSchema, String name, DataSource dataSource, String catalog, String schema) {
    this.parentSchema = parentSchema;
    this.name = name;
    this.dataSource = dataSource;
    this.catalog = catalog;
    this.schema = schema;
  }

  public StatisticsDavide getStatisticsObjectForTable(String tableName) {
    Connection connection = null;
    ResultSet resultSet = null;
    int numRows = 0;
    try {
      connection = dataSource.getConnection();
      String query = Templates.replacePlaceholder(Templates.ROW_COUNT, "$1", tableName);
      query = "SELECT COUNT(*) AS ciao FROM A";
      PreparedStatement stmt = connection.prepareStatement(query);
      resultSet = stmt.executeQuery();
      if (resultSet.next()) {
        numRows = resultSet.getInt(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException(
              "Exception while reading tables", e);
    } finally {
      close(connection, null, resultSet);
    }
    StatisticsDavide result = new StatisticsDavideImpl(numRows);
    return result;
  }

  private static class Templates {

    final static String ROW_COUNT = "SELECT COUNT(*) FROM $1";

    static String replacePlaceholder(String template, String key, String replacement) {
      String result = template.replace(key, replacement);
      return result;
    }
  }

  private static void close(
          Connection connection, Statement statement, ResultSet resultSet) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
        // ignore
      }
    }
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
        // ignore
      }
    }
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        // ignore
      }
    }
  }
}


