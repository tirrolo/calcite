package org.apache.calcite.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelDistributionTraitDef;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.List;

public class StatisticsDavideImpl implements StatisticsDavide {

//  private final ImmutableMap<String, Integer> distinctForCol;
  private final Double rowCount;
  private final int distinctForCol;
  private final boolean isKey;
  private final List<RelReferentialConstraint> referentialConstraints;
  private final List<RelCollation> collations;
  private final RelDistribution distribution;

  public StatisticsDavideImpl(double rowCount){
    this.distinctForCol = 0;
    this.rowCount = rowCount;
    this.isKey = false;
    this.referentialConstraints = ImmutableList.<RelReferentialConstraint>of();
    this.collations = ImmutableList.of();
    this.distribution = RelDistributionTraitDef.INSTANCE.getDefault();

  }

  @Override
  public int getDistinctForCol(int colID) {
    return this.distinctForCol;
  }

  @Override
  public Double getRowCount() {
    return this.rowCount;
  }

  @Override
  public boolean isKey(ImmutableBitSet columns) {
    return this.isKey;
  }

  @Override
  public List<RelReferentialConstraint> getReferentialConstraints() {
    return this.referentialConstraints;
  }

  @Override
  public List<RelCollation> getCollations() {
    return this.collations;
  }

  @Override
  public RelDistribution getDistribution() {
    return this.distribution;
  }
}
