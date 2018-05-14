package org.apache.calcite.examples;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.adapter.jdbc.JdbcSchemaWithStatsDavide;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rules.JoinUnionTransposeRule;
import org.apache.calcite.rel.rules.UnionInnerJoinTransposeRuleDavide;
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

import static org.apache.calcite.tools.Programs.ofRules;

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
    JdbcSchemaWithStatsDavide decoratedSchema =
            new JdbcSchemaWithStatsDavide(
                    schema,null, "prova",
                    dataSource, null, "prova");
    SchemaPlus result = rootSchema.add("prova", decoratedSchema);
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
            .costFactory(null)
            ;
  }

  public static void main(String[] args) throws ClassNotFoundException, RelConversionException, SqlParseException, ValidationException, SQLException {
    RelBuilderInnerJoinUnionTransposeExampleDavide exampler = new RelBuilderInnerJoinUnionTransposeExampleDavide();
//    exampler.innerJoinUnionTransposeExampleNoHEP();
//    exampler.unionInnerJoinTransposeExample();
    exampler.unionInnerJoinTransposeExampleNoHEP();
//    exampler.unionInnerJoinTransposeAndBackExample();
  }

   /**
   * Davide>
   *
   * (A UNION B) JOIN C -> (A JOIN C) UNION (B JOIN C)
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   * @throws SQLException
   */
  private void innerJoinUnionTransposeExample() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
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
   * Davide> I do not use a newly created HepPlanner
   *
   * (A UNION B) JOIN C -> (A JOIN C) UNION (B JOIN C)
   *
   * @throws ClassNotFoundException
   * @throws RelConversionException
   * @throws SqlParseException
   * @throws ValidationException
   * @throws SQLException
   */
  private void innerJoinUnionTransposeExampleNoHEP() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(ofRules(JoinUnionTransposeRule.LEFT_UNION)).build();
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

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Transformed plan cost: " + mq.getCumulativeCost(transformedPlan));

//    System.out.println("Transformed SQL"); FIXME: It gives error
//    String mysql1 = convert(transformedPlan);
//    System.out.println(mysql1);
//    System.out.println("End Transformed SQL");
  }

  private void unionInnerJoinTransposeExample() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(Programs.unionInnerJoinTransposeRuleDavide()).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql = "SELECT * FROM " +
            "(SELECT `col1A` AS `col1` FROM `prova`.`A`) AS `t`" +
            "INNER JOIN `prova`.`C` ON `t`.`col1` = `C`.`col1C`" +
            "UNION " +
            "SELECT * FROM (SELECT `col1B` AS `col1` FROM `prova`.`B`) AS `t0`" +
            "INNER JOIN `prova`.`C` AS `C0` ON `t0`.`col1` = `C0`.`col1C`";

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

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();
    Double result = mq.getRowCount(logicalPlan);
    Double result1 = mq.getRowCount(transformedPlan);

    System.out.println("Logical Plan root rowcount: " + result);
    System.out.println("Transformed plan root rowcount: " + result1);

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Transformed plan cost: " + mq.getCumulativeCost(transformedPlan));
  }

  /**
   * Questo lo trasforma!! Evviva.
   *
   * Mh, ok. Quindi l'unico problema rimasto ora e' generare SQL...
   * che nn me lo genera... ma vabbe', per ora teniamoci astratti.
   *
   * @throws SQLException
   * @throws ClassNotFoundException
   * @throws SqlParseException
   * @throws ValidationException
   * @throws RelConversionException
   */
  private void unionInnerJoinTransposeExampleNoHEP() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(ofRules(UnionInnerJoinTransposeRuleDavide.LEFT_UNION_PROJ)).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql = "SELECT * FROM " +
            "(SELECT `col1A` AS `col1` FROM `prova`.`A`) AS `t`" +
            "INNER JOIN `prova`.`C` ON `t`.`col1` = `C`.`col1C`" +
            "UNION " +
            "SELECT * FROM (SELECT `col1B` AS `col1` FROM `prova`.`B`) AS `t0`" +
            "INNER JOIN `prova`.`C` AS `C0` ON `t0`.`col1` = `C0`.`col1C`";

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

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();
    Double result = mq.getRowCount(logicalPlan);
    Double result1 = mq.getRowCount(transformedPlan);

    System.out.println("Logical Plan root rowcount: " + result);
    System.out.println("Transformed plan root rowcount: " + result1);

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Transformed plan cost: " + mq.getCumulativeCost(transformedPlan));
  }

  private void unionInnerJoinTransposeAndBackExample() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(Programs.unionInnerJoinTransposeRuleDavide(), Programs.joinUnionTrasposeRuleDavide()).build();
    this.planner = Frameworks.getPlanner(config);

    String mysql = "SELECT * FROM " +
            "(SELECT `col1A` AS `col1` FROM `prova`.`A`) AS `t`" +
            "INNER JOIN `prova`.`C` ON `t`.`col1` = `C`.`col1C`" +
            "UNION " +
            "SELECT * FROM (SELECT `col1B` AS `col1` FROM `prova`.`B`) AS `t0`" +
            "INNER JOIN `prova`.`C` AS `C0` ON `t0`.`col1` = `C0`.`col1C`";

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

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();
    Double result = mq.getRowCount(logicalPlan);
    Double result1 = mq.getRowCount(transformedPlan);

    System.out.println("Logical Plan root rowcount: " + result);
    System.out.println("Transformed plan root rowcount: " + result1);

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Transformed plan cost: " + mq.getCumulativeCost(transformedPlan));
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