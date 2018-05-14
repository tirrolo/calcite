package org.apache.calcite.rel.rules;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.*;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.tools.RelBuilderFactory;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author TirxSg3ng
 *
 * I keep this class as a proof of concept
 */
public class UnionInnerJoinTransposeRuleDavide extends RelOptRule {

  // Constants
  final static int ROOT_UNION=0;
  final static int PROJ_LEFT=1;
  final static int JOIN_LEFT=2;
  final static int PROJ_RIGHT=3;
  final static int JOIN_RIGHT=4;

  // Constructor
  public UnionInnerJoinTransposeRuleDavide(
          RelOptRuleOperand operand,
          RelBuilderFactory relBuilderFactory,
          String description){

    super(operand, relBuilderFactory, description);
  }

  // Default constructors
  public static final UnionInnerJoinTransposeRuleDavide LEFT_UNION =
          new UnionInnerJoinTransposeRuleDavide(
                  operand(Union.class,
                          operand(Join.class,any()), // Join with any operand
                          operand(Join.class,any())),
                  RelFactories.LOGICAL_BUILDER,
                  "UnionInnerJoinTransposeRuleDavide(Union-Left)");

  // Default constructors
  public static final UnionInnerJoinTransposeRuleDavide LEFT_UNION_PROJ =
          new UnionInnerJoinTransposeRuleDavide(
                  operand(Union.class,
                          operand(Project.class,some(operand(Join.class, any()))),
                          operand(Project.class,some(operand(Join.class, any())))),
                  RelFactories.LOGICAL_BUILDER,
                  "UnionInnerJoinTransposeRuleDavide(Union-Left)");

  @Override
  public void onMatch(RelOptRuleCall call) {
    // Davide> Yeah, I am close to the magic stick

    final Union rootUnion = call.rel(ROOT_UNION);
    final Project projLeft = call.rel(PROJ_LEFT);
    final Join joinLeft = call.rel(JOIN_LEFT);
    final Project projRight = call.rel(PROJ_RIGHT);
    final Join joinRight = call.rel(JOIN_RIGHT);

    // Selectivities
    final RelMetadataQuery mq = call.getMetadataQuery();

    RelNode projOverScan = joinLeft.getInput(0);
    Double rowCountProjOverScan = mq.getRowCount(projOverScan);
    RelNode scanC = joinLeft.getInput(1);
    Double resultScanC = mq.getRowCount(scanC);
    Double rootEst = rootUnion.estimateRowCount(mq);

    final ImmutableBitSet.Builder joinKeys = ImmutableBitSet.builder();

    System.out.println(" projOverScan " + rowCountProjOverScan);
    System.out.println(" scanC " + resultScanC);
    System.out.println(" rootCount" + rootEst);

    List<RelNode> newUnionInputs = new ArrayList<RelNode>();
    newUnionInputs.add(joinLeft.getLeft());
    newUnionInputs.add(joinRight.getLeft());

//    newUnionInputs.add(joinLeft.getLeft()); // Garbage TO TEST
//    newUnionInputs.add(joinRight.getLeft()); // COST

    final SetOp newUnionRel =
            rootUnion.copy(rootUnion.getTraitSet(), newUnionInputs, rootUnion.all);


    Join mainJoin = joinLeft.copy(
            joinLeft.getTraitSet(),
            joinLeft.getCondition(),
            newUnionRel,
            joinRight.getRight(),
            joinLeft.getJoinType(),
             joinLeft.isSemiJoinDone());

    call.transformTo(mainJoin);
  }
}