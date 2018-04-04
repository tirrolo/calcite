package org.apache.calcite.rel.rules;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.core.SetOp;
import org.apache.calcite.rel.core.Union;
import org.apache.calcite.tools.RelBuilderFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TirxSg3ng
 */
public class InnerJoinUnionTransposeRuleDavide extends RelOptRule {
  /**
 * Planner rule that pushes a
 * {@link org.apache.calcite.rel.core.Join}
 * past a non-distinct {@link org.apache.calcite.rel.core.Union}.
   */
    public static final InnerJoinUnionTransposeRuleDavide LEFT_UNION =
      new InnerJoinUnionTransposeRuleDavide(
          operand(Join.class,
              operand(Union.class, any()), // Davide> Union on left
              operand(RelNode.class, any())),
          RelFactories.LOGICAL_BUILDER,
          "InnerJoinUnionTransposeRuleDavide(Union-Other)");

  public static final InnerJoinUnionTransposeRuleDavide RIGHT_UNION =
      new InnerJoinUnionTransposeRuleDavide(
          operand(Join.class,
              operand(RelNode.class, any()),
              operand(Union.class, any())), // Davide> Union on the right
          RelFactories.LOGICAL_BUILDER,
          "InnerJoinUnionTransposeRuleDavide(Other-Union)");

  /**
   * Creates a JoinUnionTransposeRule.
   *
   * @param operand           root operand, must not be null
   * @param description       Description, or null to guess description
   * @param relBuilderFactory Builder for relational expressions
   */
  public InnerJoinUnionTransposeRuleDavide(RelOptRuleOperand operand,
                                           RelBuilderFactory relBuilderFactory, String description) {
    super(operand, relBuilderFactory, description);
  }

  public void onMatch(RelOptRuleCall call) {
    final Join join = call.rel(0);
    final Union unionRel;
    RelNode otherInput;
    boolean unionOnLeft;
    if (call.rel(1) instanceof Union) {
      unionRel = call.rel(1);
      otherInput = call.rel(2);
      unionOnLeft = true;
    } else {
      otherInput = call.rel(1);
      unionRel = call.rel(2);
      unionOnLeft = false;
    }
    if (!join.getVariablesSet().isEmpty()) {
      return;
    }
    List<RelNode> newUnionInputs = new ArrayList<RelNode>();
    for (RelNode input : unionRel.getInputs()) {
      RelNode joinLeft;
      RelNode joinRight;
      if (unionOnLeft) {
        joinLeft = input;
        joinRight = otherInput;
      } else {
        joinLeft = otherInput;
        joinRight = input;
      }
      newUnionInputs.add(
          join.copy(
              join.getTraitSet(),
              join.getCondition(),
              joinLeft,
              joinRight,
              join.getJoinType(),
              join.isSemiJoinDone()));
    }
    final SetOp newUnionRel =
        unionRel.copy(unionRel.getTraitSet(), newUnionInputs, true);
    call.transformTo(newUnionRel);
  }
  }
