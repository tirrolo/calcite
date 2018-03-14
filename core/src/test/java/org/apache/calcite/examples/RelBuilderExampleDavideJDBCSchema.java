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

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.*;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.*;
import org.apache.calcite.util.Util;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.SQLException;
import java.util.List;

/**
// * Example that uses {@link RelBuilder}
 * to create various relational expressions.
 */
public class RelBuilderExampleDavideJDBCSchema {
  private void fromSQLExample() throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    final FrameworkConfig config = config(Programs.joinToMultiJoinDavide()).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql = "SELECT `col1` from `startups`.`contactInfo`";

    System.out.println(mysql);
    SqlNode sqlNode = this.planner.parse(mysql);
    SqlNode validatedSqlNode = planner.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    RelNode transformedPlan = planner.transform(0, planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE), logicalPlan);
    System.out.println("PARSED Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("END PARSED Plan");

    String mysql1 = convert(logicalPlan);
    System.out.println(mysql1);
  }

  /**
   * Davide>
   *
   * (A Join B) FILTER -> (FILTER A) JOIN B
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   * @throws SQLException
   */
  private void filterIntoJoinExample() throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    final FrameworkConfig config = config(Programs.filterJoinRuleProgramDavide()).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql = "SELECT * from `startups`.`contactInfo` " +
            "AS A INNER JOIN `startups`.`contactInfo` AS B " +
            "ON A.col1 = B.col1 WHERE A.col18 = 20";

    System.out.println(mysql);
    SqlNode sqlNode = this.planner.parse(mysql);
    SqlNode validatedSqlNode = planner.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    RelNode transformedPlan = planner.transform(0, planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE), logicalPlan);
    System.out.println("Logical Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("End Planner");
    System.out.println("Transformed Plan");
    System.out.println(RelOptUtil.toString(transformedPlan));
    System.out.println("End Transformed Plan");

    System.out.println("Transformed SQL");
    String mysql1 = convert(transformedPlan);
    System.out.println(mysql1);
    System.out.println("End Transformed SQL");
  }

  /**
   * Davide>
   *
   * (A Join B) FILTER -> (FILTER A) JOIN B
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   * @throws SQLException
   */
  private void joinUnionTransposeExample() throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    final FrameworkConfig config = config(Programs.filterJoinRuleProgramDavide()).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql =
            "SELECT * FROM " +
                    "((SELECT col1A FROM A) AS col1 " +
                    "UNION ALL (SELECT col1B FROM B)) QVIEW " +
                    "INNER JOIN C ON QVIEW.col1=C.col1C;";

    System.out.println(mysql);
    SqlNode sqlNode = this.planner.parse(mysql);
    SqlNode validatedSqlNode = planner.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    RelNode transformedPlan = planner.transform(0, planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE), logicalPlan);
    System.out.println("Logical Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("End Planner");
    System.out.println("Transformed Plan");
    System.out.println(RelOptUtil.toString(transformedPlan));
    System.out.println("End Transformed Plan");

    System.out.println("Transformed SQL");
    String mysql1 = convert(transformedPlan);
    System.out.println(mysql1);
    System.out.println("End Transformed SQL");
  }


  /**
   * Davide> Convert the root RelNode to (My)SQL
   * @param node
   */
  private String convert(RelNode node){
    SqlDialect dialect = SqlDialect.DatabaseProduct.MYSQL.getDialect();
    RelToSqlConverter converter = new RelToSqlConverter(dialect);
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
  private RelBuilder buildThreeSelfJoinExpression(RelBuilder builder){

    RelNode left = builder.scan("contactInfo")
            .scan("contactInfo")
            .join(JoinRelType.INNER, "col1").build();

    RelNode right = builder.scan("contactInfo").build();


    return builder.push(left).push(right).join(JoinRelType.INNER, "col1");

  }

  private Planner planner;

  public static SchemaPlus createSchema (SchemaPlus rootSchema) throws ClassNotFoundException, SQLException {
    Class.forName("com.mysql.jdbc.Driver");
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:mysql://localhost/startups");
    dataSource.setUsername("fish");
    dataSource.setPassword("fish");


//  Elem> SqlDialect dialect = new SqlDialectFactoryImpl().create(dataSource.getConnection().getMetaData());

    Schema schema = JdbcSchema.create(rootSchema, "startups", dataSource,
            null, "startups");
    SchemaPlus result = rootSchema.add("startups", schema);
    return result;
  }

  public static Frameworks.ConfigBuilder config(Program ...programs) throws ClassNotFoundException, SQLException {
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    SchemaPlus schema = createSchema(rootSchema);

    return Frameworks.newConfigBuilder()
            .parserConfig(SqlParser.configBuilder().setLex(Lex.MYSQL).build())
//           .parserConfig(SqlParser.Config.DEFAULT)

            .defaultSchema(schema)
            .traitDefs((List<RelTraitDef>) null)
//            .programs(Programs.heuristicJoinOrder(Programs.RULE_SET, false, 2));
            .programs(programs)
            ;
  }

  public static void main(String[] args) throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    RelBuilderExampleDavideJDBCSchema exampler = new RelBuilderExampleDavideJDBCSchema();
//    exampler.runJoinToMultiJoinPlan();
//    exampler.fromSQLExample();
    exampler.filterIntoJoinExample();
  }


  /**
   * Davide> Very nice transformation:
   *
   * (A JOIN B) JOIN C => MULTI-JOIN(A,B,C)
   *
   * LogicalProject(col1=[$0], col8=[$1], col9=[$2], col10=[$3], col14=[$4], col15=[$5], col16=[$6], col17=[$7], col18=[$8], col19=[$9], col20=[$10], col21=[$11], col11=[$12], col80=[$13], col90=[$14], col100=[$15], col140=[$16], col150=[$17], col160=[$18], col170=[$19], col180=[$20], col190=[$21], col200=[$22], col210=[$23], col12=[$24], col81=[$25], col91=[$26], col101=[$27], col141=[$28], col151=[$29], col161=[$30], col171=[$31], col181=[$32], col191=[$33], col201=[$34], col211=[$35])
   *   MultiJoin(joinFilter=[AND(=($0, $24), =($0, $12))], isFullOuterJoin=[false], joinTypes=[[INNER, INNER, INNER]], outerJoinConditions=[[NULL, NULL, NULL]], projFields=[[ALL, ALL, ALL]])
   *     JdbcTableScan(table=[[startups, contactInfo]])
   *     JdbcTableScan(table=[[startups, contactInfo]])
   *     JdbcTableScan(table=[[startups, contactInfo]])
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   */
  public void runJoinToMultiJoinPlan() throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    final FrameworkConfig config = config(Programs.joinToMultiJoinDavide()).build();
    this.planner = Frameworks.getPlanner(config);
    final RelBuilder builder = RelBuilder.create(config);
    buildThreeSelfJoinExpression(builder);
    final RelNode node = builder.build(); // node contains a logical plan (no traits)

    System.out.println("Logical-PLAN START");
    System.out.println(RelOptUtil.toString(node));
    System.out.println("Logical-PLAN END");

    // Davide>
    System.out.println("MYSQL-TRANSLATION START");
    String mysql = convert(node);
    System.out.println(mysql);
    System.out.println("MYSQL-TRANSLATION END");

    Planner planner = Frameworks.getPlanner(config);
    SqlNode sqlNode = planner.parse(mysql);

    SqlNode validatedSqlNode = planner.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    RelTraitSet traits = planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE); // We need to give the node an IMPLEMENTABLE trait
    // Relational operators with the Enumberable(calling)Convention simply operate over tuples via an iterator
    // interface. This convention allows calcite to implement operators which may be not available
    // in each adapter's backend.
    RelNode transformedPlan = planner.transform(0, traits, logicalPlan);
    System.out.println("TRANSFORMED Plan START");
    System.out.println(RelOptUtil.toString(transformedPlan));
    System.out.println("TRANSFORMED Plan END");

    // Davide>
    /*
    MySQL does not support the MultiJoin (In fact, the MultiJoin node in the plan does
    not have the JDBC trait). Hence, the following three commented out lines
    will give us the exception:
    Caused by: java.lang.AssertionError: Need to implement org.apache.calcite.rel.rules.MultiJoin
    at org.apache.calcite.rel.rel2sql.RelToSqlConverter.visit(RelToSqlConverter.java:118)
     */
//    System.out.println("TRANSFORMED-TRANSLATION START");
//    System.out.println(convert(transformedPlan));
//    System.out.println("TRANSFORMED-TRANSLATION END");

  }
}

// End RelBuilderExampleDavideJDBCSchema.java
