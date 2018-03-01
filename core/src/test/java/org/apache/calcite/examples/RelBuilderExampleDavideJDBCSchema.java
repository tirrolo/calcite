/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.examples;

import com.google.common.collect.ImmutableSet;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.interpreter.InterpretableConvention;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rules.*;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.test.CalciteAssert;
import org.apache.calcite.tools.*;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.Util;
import org.apache.commons.dbcp.BasicDataSource;

import java.util.ArrayList;
import java.util.List;

/**
// * Example that uses {@link RelBuilder}
 * to create various relational expressions.
 */
public class RelBuilderExampleDavideJDBCSchema {
  private final boolean verbose;
  private Planner planner;

  public RelBuilderExampleDavideJDBCSchema(boolean verbose) {
    this.verbose = verbose;
  }

  public static SchemaPlus createSchema (SchemaPlus rootSchema) throws ClassNotFoundException {
    Class.forName("com.mysql.jdbc.Driver");
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:mysql://localhost/startups");
    dataSource.setUsername("fish");
    dataSource.setPassword("fish");
    Schema schema = JdbcSchema.create(rootSchema, "startups", dataSource,
            null, "startups");
    SchemaPlus result = rootSchema.add("startups", schema);
    return result;
  }

  public static Frameworks.ConfigBuilder config() throws ClassNotFoundException {
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    SchemaPlus schema = createSchema(rootSchema);
    return Frameworks.newConfigBuilder()
            .parserConfig(SqlParser.Config.DEFAULT)
            .defaultSchema(schema)
            .traitDefs((List<RelTraitDef>) null)
//            .programs(Programs.heuristicJoinOrder(Programs.RULE_SET, false, 2));
            .programs(Programs.joinToMultiJoinDavide());
  }

  public static void main(String[] args) throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException {
    RelBuilderExampleDavideJDBCSchema exampler = new RelBuilderExampleDavideJDBCSchema(true);
    exampler.runDavideExamples(); // TODO : Testa questo appena hai tempo
//    exampler.runDavideExamplesFromSQL();
  }

  private void runDavideExamplesFromSQL() throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException {
    final FrameworkConfig config = config().build();
    this.planner = Frameworks.getPlanner(config);
    final RelBuilder builder = RelBuilder.create(config);

      String mysql = "SELECT \"col1\" from \"startups\".\"contactInfo\"";

    System.out.println(mysql);
    SqlNode sqlNode = this.planner.parse(mysql);
    SqlNode validatedSqlNode = planner.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    RelNode transformedPlan = planner.transform(0, planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE), logicalPlan);
    System.out.println("Logical Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("End Planner");
  }

  /**
   * Davide> Very nice transformation:
   *
   * (A JOIN B) JOIN C => MULTI-JOIN(A,B,C)
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   */
  public void runDavideExamples () throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException {
    final FrameworkConfig config = config().build();
    this.planner = Frameworks.getPlanner(config);
    final RelBuilder builder = RelBuilder.create(config);
    exampleSelfJoinElimination(builder);
    final RelNode node = builder.build();

    if (verbose) {

      System.out.println("PLAN START");
      System.out.println(RelOptUtil.toString(node));
      System.out.println("PLAN END");
    }
    // Davide>
    System.out.println("SQL-TRANSLATION START");
    String mysql = convert(node);
    System.out.println(convert(node));
    System.out.println("SQL-TRANSLATION END");

    mysql = mysql.replaceAll("\\[","\"");
    mysql = mysql.replaceAll("\\]","\"");

    Planner planner = Frameworks.getPlanner(config);
    SqlNode sqlNode = planner.parse(mysql);

    SqlNode validatedSqlNode = planner.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    RelTraitSet traits = planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE); // We need to give the node an IMPLEMENTABLE trait
    RelNode transformedPlan = planner.transform(0, traits, logicalPlan);
    System.out.println("TRANSFORMED Plan START");
    System.out.println(RelOptUtil.toString(transformedPlan));
    System.out.println("TRANSFORMED Plan END");

  }

  /**
   * Davide> Convert the root RelNode to (My)SQL
   * @param node
   */
  private String convert(RelNode node){
    SqlDialect dialect = SqlDialect.DatabaseProduct.MSSQL.getDialect();
    RelToSqlConverter converter = new RelToSqlConverter(SqlDialect.DatabaseProduct.MSSQL.getDialect());
    SqlNode sqlNode = converter.visitChild(0, node).asStatement();
    String result = Util.toLinux(sqlNode.toSqlString(dialect).getSql());
    //SqlImplementor.Result res = converter.visit(node);
    return result;
  }

  /**
   *
   * @param builder
   * @return An expression containing a self-join that might be eliminated
   * Trying to get the optimization A JOIN B -> MJ(A, B) (MJ := Multi-join)
   *
   * (cI Join cI) Join cI
   *
   *
   * LogicalJoin(condition=[=($0, $24)], joinType=[inner])
   *   LogicalJoin(condition=[=($0, $12)], joinType=[inner])
   *     LogicalTableScan(table=[[startups, contactInfo]])
   *     LogicalTableScan(table=[[startups, contactInfo]])
   *   LogicalTableScan(table=[[startups, contactInfo]])
   *
   */
  private RelBuilder exampleSelfJoinElimination(RelBuilder builder){

    RelNode left = builder.scan("contactInfo")
            .scan("contactInfo")
            .join(JoinRelType.INNER, "col1").build();

    RelNode right = builder.scan("contactInfo").build();


    return builder.push(left).push(right).join(JoinRelType.INNER, "col1");

  }


  /**
   * Creates a relational expression for a table scan.
   * It is equivalent to
   *
   * <blockquote><pre>SELECT *
   * FROM emp</pre></blockquote>
   */
  private RelBuilder example0(RelBuilder builder) {
    return builder
        .scan("contactInfo");
  }

}

// End RelBuilderExampleDavideJDBCSchema.java
