/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Oct 11, 2004
 * author: saunders
 */

package org.rexo.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortingAndCountingUtils {

  public static final int SORT_ASCENDING = 1;
  public static final int SORT_DESCENDING = -1;

  /**
   * Sort a list of arrays according to one of the array fields, as specified by
   * fieldIndex. May sort ascending or descending
   * @param list list to be sorted
   * @param fieldIndex which member of array should be used as basis for sorting
   * @param order SORT_ASCENDING or  SORT_DESCENDING
   */
  public static void sortListOfArrays(List list, final int fieldIndex, final int order) {
    Collections.sort( list, new Comparator() {
      public int compare(Object o1, Object o2) {
        Object[] lhs = (Object[])o1;
        Object[] rhs = (Object[])o2;
        Comparable lhsString = (Comparable)lhs[fieldIndex];
        Comparable rhsString = (Comparable)rhs[fieldIndex];
        return (lhsString.compareTo( rhsString )) * order;
      }
    } );
  }


  /**
   * modeled after unix 'uniq'. Takes a list of arrays and an index into
   * the array, and returns a list of pairs{Integer, Integer}, where
   * pair[0] = uniq count, pair[1] = starting index in the input list
   * for the uniq field.
   *
   * @param list list of arrays to be uniq-counted according to one of the array members
   * @param fieldIndex which array member to uniq-count
   * @return list of pairs,
   */
  public static List uniqCountListOfArrays(List list, final int fieldIndex) {
    ArrayList uniqCountList = new ArrayList();
    if (!list.isEmpty()) {
      Object[] firstRow = (Object[])list.get( 0 );
      Object firstField = firstRow[fieldIndex];
      int firstFieldIndex = 0;
      int uniqCount = 1;
      for (int i = 1; i < list.size(); i++) {
        Object[] row = (Object[])list.get( i );
        Object field = row[fieldIndex];
        if (field.equals( firstField )) {
          uniqCount++;
        }
        else {
          Integer[] countIndex = new Integer[]{new Integer( uniqCount ), new Integer( firstFieldIndex )};
          uniqCountList.add( countIndex );
          uniqCount = 1;
          firstFieldIndex = i;
          firstField = field;
        }
      }
      uniqCountList.add( new Integer[]{new Integer( uniqCount ), new Integer( firstFieldIndex )} );
    }
    return uniqCountList;
  }

	/**
	 * 
	 * @param list
	 * @return
	 */
	public static List uniqCountListOfObjects(List list) {
		ArrayList uniqCountList = new ArrayList();
		if (!list.isEmpty()) {
			Object firstField = list.get( 0 );
			int firstFieldIndex = 0;
			int uniqCount = 1;
			for (int i = 1; i < list.size(); i++) {
				Object field = list.get( i );
				if (field.equals( firstField )) {
					uniqCount++;
				}
				else {
					Integer[] countIndex = new Integer[]{new Integer( uniqCount ), new Integer( firstFieldIndex )};
					uniqCountList.add( countIndex );
					uniqCount = 1;
					firstFieldIndex = i;
					firstField = field;
				}
			}
			uniqCountList.add( new Integer[]{new Integer( uniqCount ), new Integer( firstFieldIndex )} );
		}
		return uniqCountList;
	}
}
