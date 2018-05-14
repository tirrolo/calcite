package org.apache.calcite.examples;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.adapter.jdbc.JdbcSchemaWithStatsDavide;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.prepare.PlannerImpl;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rules.FilterProjectTransposeRule;
import org.apache.calcite.rel.rules.UnionInnerJoinTransposeRuleDavide;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.*;
import org.apache.calcite.util.Util;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class RelBuilderInnerJoinUnionTransposeVolcanoExampleDavide {

  private RelOptPlanner optPlanner; // Volcano planner for cost-based
                                     // optimization

  private Planner structPlanner;

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
    RelBuilderInnerJoinUnionTransposeVolcanoExampleDavide exampler = new RelBuilderInnerJoinUnionTransposeVolcanoExampleDavide();
    exampler.unionInnerJoinTransposeExample2();
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

  private void unionInnerJoinTransposeExample() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(Programs.unionInnerJoinTransposeRuleDavide()).build();

    String mysql = "SELECT * FROM " +
            "(SELECT `col1A` AS `col1` FROM `prova`.`A`) AS `t`" +
            "INNER JOIN `prova`.`C` ON `t`.`col1` = `C`.`col1C`" +
            "UNION " +
            "SELECT * FROM (SELECT `col1B` AS `col1` FROM `prova`.`B`) AS `t0`" +
            "INNER JOIN `prova`.`C` AS `C0` ON `t0`.`col1` = `C0`.`col1C`";

    System.out.println(mysql);

    // Create virgin planner
    this.optPlanner = new VolcanoPlanner();

    // Parse the query
    SqlParser parser = SqlParser.create(mysql, SqlParser.configBuilder().setLex(Lex.MYSQL).build());
    SqlNode sqlNode = parser.parseStmt();

    // Validate the query
    RelDataTypeFactory typeFactory =
            new SqlTypeFactoryImpl(org.apache.calcite.rel.type.RelDataTypeSystem.DEFAULT);

    CalciteCatalogReader catalogReader = createCatalogReader(config, typeFactory);

    SqlValidator validator = SqlValidatorUtil.newValidator(
            SqlStdOperatorTable.instance(), catalogReader, typeFactory, SqlConformanceEnum.MYSQL_5);


    // Convert SqlNode to RelNode
    PlannerImpl planner = (PlannerImpl) Frameworks.getPlanner(config);

    RexBuilder rexBuilder = createRexBuilder(typeFactory);
    RelOptCluster cluster = RelOptCluster.create(optPlanner, rexBuilder);
//    SqlToRelConverter sqlToRelConverter =
//            new SqlToRelConverter(
//                    new PlannerImpl.ViewExpanderImpl(), validator,
//                    catalogReader ,cluster, config.getConvertletTable()); TODO : Continue this


    SqlNode validatedSqlNode = validator.validate(sqlNode);
    RelNode logicalPlan = planner.rel(validatedSqlNode).project();
    planner.close();

    Program program = Programs.ofRules(UnionInnerJoinTransposeRuleDavide.LEFT_UNION_PROJ);
    RelTraitSet traitSet = optPlanner.emptyTraitSet().replace(EnumerableConvention.INSTANCE);

    RelNode optimizedPlan = program.run(optPlanner, logicalPlan, traitSet, new ArrayList<>(), new ArrayList<>());

    System.out.println("Logical Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("End Planner");
    System.out.println("Optimized Plan");
    System.out.println(RelOptUtil.toString(optimizedPlan));
    System.out.println("End Optimized Plan");

    System.out.println("Optimized SQL");
    String mysql1 = convert(optimizedPlan);
    System.out.println(mysql1);
    System.out.println("End Optimized SQL");

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();
    Double result = mq.getRowCount(logicalPlan);
    Double result1 = mq.getRowCount(optimizedPlan);

    System.out.println("Logical Plan root rowcount: " + result);
    System.out.println("Transformed plan root rowcount: " + result1);

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Optimized plan cost: " + mq.getCumulativeCost(optimizedPlan));
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


// CalciteCatalogReader is stateless; no need to store one
  private CalciteCatalogReader createCatalogReader(FrameworkConfig config, RelDataTypeFactory typeFactory) {
    SchemaPlus rootSchema = rootSchema(config.getDefaultSchema());
    Context context = config.getContext();
    CalciteConnectionConfig connectionConfig = null;

    if (context != null) {
      connectionConfig = context.unwrap(CalciteConnectionConfig.class);
    } else {
      SqlParser.Config parserConfig = SqlParser.configBuilder().setLex(Lex.MYSQL).build();
      Properties properties = new Properties();
      properties.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(),
              String.valueOf(parserConfig.caseSensitive()));
      connectionConfig = new CalciteConnectionConfigImpl(properties);
    }

    return new CalciteCatalogReader(
        CalciteSchema.from(rootSchema),
        CalciteSchema.from(config.getDefaultSchema()).path(null),
        typeFactory, connectionConfig);
  }

  private static SchemaPlus rootSchema(SchemaPlus schema) {
    for (;;) {
      if (schema.getParentSchema() == null) {
        return schema;
      }
      schema = schema.getParentSchema();
    }
  }

  // RexBuilder is stateless; no need to store one
  private RexBuilder createRexBuilder(RelDataTypeFactory typeFactory) {
    return new RexBuilder(typeFactory);
  }

  // Attempt with structural planner
  private void unionInnerJoinTransposeExample2() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(Programs.unionInnerJoinTransposeRuleDavide()).build();

    String mysql = "SELECT * FROM " +
            "(SELECT `col1A` AS `col1` FROM `prova`.`A`) AS `t`" +
            "INNER JOIN `prova`.`C` ON `t`.`col1` = `C`.`col1C`" +
            "UNION " +
            "SELECT * FROM (SELECT `col1B` AS `col1` FROM `prova`.`B`) AS `t0`" +
            "INNER JOIN `prova`.`C` AS `C0` ON `t0`.`col1` = `C0`.`col1C`";

    System.out.println(mysql);

    // Create virgin planner
    this.optPlanner = new VolcanoPlanner();
    this.structPlanner = Frameworks.getPlanner(config);

    // Parse the query
    SqlNode parsed = this.structPlanner.parse(mysql);

    // Convert SqlNode to RelNode

    SqlNode validatedSqlNode = this.structPlanner.validate(parsed);
    RelNode logicalPlan = this.structPlanner.rel(validatedSqlNode).project();

    Program program = Programs.ofRules(UnionInnerJoinTransposeRuleDavide.LEFT_UNION_PROJ);
    RelTraitSet traitSet = optPlanner.emptyTraitSet().replace(EnumerableConvention.INSTANCE);

    RelNode optimizedPlan = program.run(optPlanner, logicalPlan, traitSet, new ArrayList<>(), new ArrayList<>());

    System.out.println("Logical Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("End Planner");
    System.out.println("Optimized Plan");
    System.out.println(RelOptUtil.toString(optimizedPlan));
    System.out.println("End Optimized Plan");

    System.out.println("Optimized SQL");
    String mysql1 = convert(optimizedPlan);
    System.out.println(mysql1);
    System.out.println("End Optimized SQL");

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();
    Double result = mq.getRowCount(logicalPlan);
    Double result1 = mq.getRowCount(optimizedPlan);

    System.out.println("Logical Plan root rowcount: " + result);
    System.out.println("Transformed plan root rowcount: " + result1);

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Optimized plan cost: " + mq.getCumulativeCost(optimizedPlan));
  }



  // Attempt with HEP
  private void unionInnerJoinTransposeExample3() throws SQLException, ClassNotFoundException, SqlParseException, ValidationException, RelConversionException {
    final FrameworkConfig config = config(Programs.unionInnerJoinTransposeRuleDavide()).build();

    String mysql = "SELECT * FROM " +
            "(SELECT `col1A` AS `col1` FROM `prova`.`A`) AS `t`" +
            "INNER JOIN `prova`.`C` ON `t`.`col1` = `C`.`col1C`" +
            "UNION " +
            "SELECT * FROM (SELECT `col1B` AS `col1` FROM `prova`.`B`) AS `t0`" +
            "INNER JOIN `prova`.`C` AS `C0` ON `t0`.`col1` = `C0`.`col1C`";

    System.out.println(mysql);

    // Create virgin planner
    this.structPlanner = Frameworks.getPlanner(config);

    // Parse the query
    SqlNode parsed = this.structPlanner.parse(mysql);

    // Convert SqlNode to RelNode

    SqlNode validatedSqlNode = this.structPlanner.validate(parsed);
    RelNode logicalPlan = this.structPlanner.rel(validatedSqlNode).project();

    Program program = Programs.ofRules(UnionInnerJoinTransposeRuleDavide.LEFT_UNION_PROJ);
//    RelTraitSet traitSet = this.optPlanner.emptyTraitSet().replace(EnumerableConvention.INSTANCE);

    final HepProgram hep = new HepProgramBuilder()
              .addRuleInstance(UnionInnerJoinTransposeRuleDavide.LEFT_UNION_PROJ)
              .build();
    this.optPlanner = new HepPlanner(hep);

//    RelNode optimizedPlan = program.run(optPlanner, logicalPlan, traitSet, new ArrayList<>(), new ArrayList<>());

    this.optPlanner.setRoot(logicalPlan);
    RelNode optimizedPlan = this.optPlanner.findBestExp();

    System.out.println("Logical Plan: ");
    System.out.println(RelOptUtil.toString(logicalPlan));
    System.out.println("End Planner");
    System.out.println("Optimized Plan");
    System.out.println(RelOptUtil.toString(optimizedPlan));
    System.out.println("End Optimized Plan");

    System.out.println("Optimized SQL");
    String mysql1 = convert(optimizedPlan);
    System.out.println(mysql1);
    System.out.println("End Optimized SQL");

    // Selectivities
    final RelMetadataQuery mq = RelMetadataQuery.instance();
    Double result = mq.getRowCount(logicalPlan);
    Double result1 = mq.getRowCount(optimizedPlan);

    System.out.println("Logical Plan root rowcount: " + result);
    System.out.println("Transformed plan root rowcount: " + result1);

    System.out.println("Logical Plan cost: " + mq.getCumulativeCost(logicalPlan));
    System.out.println("Optimized plan cost: " + mq.getCumulativeCost(optimizedPlan));
  }

};