package org.apache.calcite.examples;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.rel.RelNode;
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

public class RelBuilderInnerJoinUnionTransposeExampleDavide {

  private Planner planner;

  public static SchemaPlus createSchema (SchemaPlus rootSchema) throws ClassNotFoundException, SQLException {
    Class.forName("com.mysql.jdbc.Driver");
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:mysql://localhost/prova");
    dataSource.setUsername("fish");
    dataSource.setPassword("fish");


    Schema schema = JdbcSchema.create(rootSchema, "prova", dataSource,
            null, "prova");
    SchemaPlus result = rootSchema.add("prova", schema);
    return result;
  }

  public static Frameworks.ConfigBuilder config(Program...programs) throws ClassNotFoundException, SQLException {
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    SchemaPlus schema = createSchema(rootSchema);

    return Frameworks.newConfigBuilder()
            .parserConfig(SqlParser.configBuilder().setLex(Lex.MYSQL).build())
            .defaultSchema(schema)
            .traitDefs((List<RelTraitDef>) null)
            .programs(programs)
            ;
  }

  public static void main(String[] args) throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    RelBuilderInnerJoinUnionTransposeExampleDavide exampler = new RelBuilderInnerJoinUnionTransposeExampleDavide();
    exampler.innerJoinUnionTransposeExample();
  }

   /**
   * Davide>
   *
   * (A UNION B) JOIN C -> A JOIN B
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   * @throws SQLException
   */
  private void innerJoinUnionTransposeExample() throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    final FrameworkConfig config = config(Programs.joinUnionTrasposeRuleDavide()).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql =
            "SELECT * FROM " +
                    "((SELECT `col1A` AS col1 FROM A) " +
                    "UNION " +
                    "(SELECT `col1B` as col1 FROM B)) " +
                    "QVIEW INNER JOIN C ON " +
                    "QVIEW.col1=C.`col1C`";

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

};