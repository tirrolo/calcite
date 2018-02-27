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
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.test.CalciteAssert;
import org.apache.calcite.tools.*;
import org.apache.calcite.util.Util;

import java.util.List;

/**
// * Example that uses {@link RelBuilder}
 * to create various relational expressions.
 */
public class RelBuilderExampleDavide {
  private final boolean verbose;

  public RelBuilderExampleDavide(boolean verbose) {
    this.verbose = verbose;
  }

  /* The class "Frameworks" contains tools for invoking Calcite functionality
     without initializing a container server first
  */
  public static Frameworks.ConfigBuilder config() {
    // Davide> Create a root schema. Parameter true: Add "metadata" schema
    // containing definitions of tables, columns, etc.
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);

    return Frameworks.newConfigBuilder()
        .parserConfig(SqlParser.Config.DEFAULT)
        .defaultSchema(
            CalciteAssert.addSchema(rootSchema, CalciteAssert.SchemaSpec.SCOTT))
        .traitDefs((List<RelTraitDef>) null)
        .programs(Programs.heuristicJoinOrder(Programs.RULE_SET, false, 2));
  }

  public static void main(String[] args) throws ValidationException, RelConversionException, SqlParseException {
    new RelBuilderExampleDavide(true).runAllExamples();
  }

  public void runAllExamples() throws SqlParseException, ValidationException, RelConversionException {
    // Create a builder. The config contains a schema mapped
    // to the SCOTT database, with tables EMP and DEPT.
    final FrameworkConfig config = config().build();
    final RelBuilder builder = RelBuilder.create(config);
    for (int i = 1; i < 2; i++) {
      doExample(builder, i);
      final RelNode node = builder.build();
      if (verbose) {
        System.out.println("PLAN START");
        System.out.println(RelOptUtil.toString(node));
        System.out.println("PLAN END");
      }
      // Davide>
      System.out.println("TRANS START");
      String mysql = convert(node);
      System.out.println(mysql);
      mysql = mysql.replaceAll("\\[","\"");
      mysql = mysql.replaceAll("\\]","\"");

      System.out.println("TRANS END");
      Planner planner = Frameworks.getPlanner(config);
      SqlNode sqlNode = planner.parse(mysql);

      SqlNode validatedSqlNode = planner.validate(sqlNode);
      RelNode logicalPlan = planner.rel(validatedSqlNode).project();
      RelTraitSet traits = planner.getEmptyTraitSet().replace(EnumerableConvention.INSTANCE);
      RelNode transformedPlan = planner.transform(0, traits, logicalPlan);
      System.out.println("Logical Plan: ");
      System.out.println(RelOptUtil.toString(logicalPlan));
      System.out.println("End Planner");
    }
  }

  private RelBuilder doExample(RelBuilder builder, int i) {
    switch (i) {
    case 0:
      return example0(builder);
    case 1:
      return example1(builder);
    case 2:
      return example2(builder);
    case 3:
      return example3(builder);
    case 4:
      return example4(builder);
    default:
      throw new AssertionError("unknown example " + i);
    }
  }

  // Davide>
  private String convert(RelNode node){
    SqlDialect dialect = SqlDialect.DatabaseProduct.MSSQL.getDialect();
    RelToSqlConverter converter = new RelToSqlConverter(SqlDialect.DatabaseProduct.MSSQL.getDialect());
    SqlNode sqlNode = converter.visitChild(0, node).asStatement();
    return Util.toLinux(sqlNode.toSqlString(dialect).getSql());
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
        .values(new String[] {"a", "b"}, 1, true, null, false);
  }

  /**
   * Creates a relational expression for a table scan.
   * It is equivalent to
   *
   * <blockquote><pre>SELECT *
   * FROM emp</pre></blockquote>
   */
  private RelBuilder example1(RelBuilder builder) {
    return builder
        .scan("EMP");
  }

  /**
   * Creates a relational expression for a table scan and project.
   * It is equivalent to
   *
   * <blockquote><pre>SELECT deptno, ename
   * FROM emp</pre></blockquote>
   */
  private RelBuilder example2(RelBuilder builder) {
    return builder
        .scan("EMP")
        .project(builder.field("DEPTNO"), builder.field("ENAME"));
  }

  /**
   * Creates a relational expression for a table scan, aggregate, filter.
   * It is equivalent to
   *
   * <blockquote><pre>SELECT deptno, count(*) AS c, sum(sal) AS s
   * FROM emp
   * GROUP BY deptno
   * HAVING count(*) &gt; 10</pre></blockquote>
   */
  private RelBuilder example3(RelBuilder builder) {
    return builder
        .scan("EMP")
        .aggregate(builder.groupKey("DEPTNO"),
            builder.count(false, "C"),
            builder.sum(false, "S", builder.field("SAL")))
        .filter(
            builder.call(SqlStdOperatorTable.GREATER_THAN, builder.field("C"),
                builder.literal(10)));
  }

  /**
   * Sometimes the stack becomes so deeply nested it gets confusing. To keep
   * things straight, you can remove expressions from the stack. For example,
   * here we are building a bushy join:
   *
   * <blockquote><pre>
   *                join
   *              /      \
   *         join          join
   *       /      \      /      \
   * CUSTOMERS ORDERS LINE_ITEMS PRODUCTS
   * </pre></blockquote>
   *
   * <p>We build it in three stages. Store the intermediate results in variables
   * `left` and `right`, and use `push()` to put them back on the stack when it
   * is time to create the final `Join`.
   * Davide> BROKEN!
   */
  private RelBuilder example4(RelBuilder builder) {
    final RelNode left = builder
        .scan("CUSTOMERS")
        .scan("ORDERS")
        .join(JoinRelType.INNER, "ORDER_ID")
        .build();

    final RelNode right = builder
        .scan("LINE_ITEMS")
        .scan("PRODUCTS")
        .join(JoinRelType.INNER, "PRODUCT_ID")
        .build();

    return builder
        .push(left)
        .push(right)
        .join(JoinRelType.INNER, "ORDER_ID");
  }
}

// End RelBuilderExampleDavide.java
