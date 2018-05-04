package org.apache.calcite.schema;

import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.List;

/**
 * @author Davide Lanti
 *
 * Here I put additional stats
 */
public interface StatisticsDavide extends Statistic {

  public int getDistinctForCol(int colID);

}
