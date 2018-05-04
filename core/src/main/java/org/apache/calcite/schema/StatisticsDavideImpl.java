package org.apache.calcite.schema;

import com.google.common.collect.ImmutableMap;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.List;

public class StatisticsDavideImpl implements StatisticsDavide {

//  private final ImmutableMap<String, Integer> distinctForCol;
  private final Double rowCount;

  public StatisticsDavideImpl(double rowCount){
    this.rowCount = rowCount;
  }

  @Override
  public int getDistinctForCol(int colID) {
    return 0;
  }

  @Override
  public Double getRowCount() {
    return null;
  }

  @Override
  public boolean isKey(ImmutableBitSet columns) {
    return false;
  }

  @Override
  public List<RelReferentialConstraint> getReferentialConstraints() {
    return null;
  }

  @Override
  public List<RelCollation> getCollations() {
    return null;
  }

  @Override
  public RelDistribution getDistribution() {
    return null;
  }
}
