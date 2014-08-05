package edu.umass.cs.rexo.ghuang.segmentation.utils;

import edu.umass.cs.rexo.ghuang.segmentation.LineInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by klimzaporojets on 8/5/14.
 *
 */
public class LayoutUtils {


    public static void adjustLineWidth(LineInfo[] lineInfos, int i, List <Entry<Integer>> widthLine)
    {
        int width = lineInfos[i].urx - lineInfos[i].llx;
        Entry<Integer> currentWidthEntry = new Entry<Integer>(width,1);
        int iOf = widthLine.indexOf(currentWidthEntry);
        if(iOf>-1)
        {
            Entry actualData = widthLine.get(iOf);
            actualData.setQty(actualData.getQty()+1);
        }
        else
        {
            widthLine.add(currentWidthEntry);
        }
    }

    public static void adjustLineWidthPerPage(LineInfo[] lineInfos, int i, Map<Integer, List<Entry<Integer>>> widthLinePerPage)
    {
        LineInfo lineInfo = lineInfos[i];
        List<Entry<Integer>> entry = widthLinePerPage.get(lineInfo.page);

        if(entry == null)
        {
            entry = new ArrayList<Entry<Integer>>();
        }
//        else
//        {
//
//        }
        adjustLineWidth(lineInfos, i, entry);
        widthLinePerPage.put(lineInfo.page,entry);
    }

    public static void adjustVerticalDistance(LineInfo[] lineInfos, int i,  List<Entry<Integer>> verticalDistance)
    {

        if (i+1 < lineInfos.length && lineInfos[i].page == lineInfos[i + 1].page && lineInfos[i].lly > lineInfos[i+1].lly) {
            Integer vertDistance = lineInfos[i].lly - lineInfos[i + 1].lly;
            Entry<Integer > initialEntry = new Entry<Integer>(vertDistance,1);
            int iOf = verticalDistance.indexOf(initialEntry);
            if(iOf > -1)
            {
                //verticalDistance.put(vertDistance,verticalDistance.get(vertDistance)+1);
                Entry<Integer> exEntry = verticalDistance.get(iOf);
                exEntry.setQty(exEntry.getQty()+1);
            }
            else
            {
                verticalDistance.add(initialEntry);
            }
        }
    }
    public static class Entry<T1> implements Comparable<Entry<T1>>
    {
        T1 key;
        Integer qty;

        public Entry(T1 key, Integer qty)
        {
            this.key = key;
            this.qty = qty;
        }

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }

        public T1 getKey() {
            return key;
        }

        public void setKey(T1 key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object obj)
        {
            return ((Entry)obj).getKey().equals(this.key);
        }

        @Override
        public int hashCode()
        {
            return key.hashCode();
        }

        @Override
        public int compareTo(Entry<T1> entry)
        {
            int otherQty = entry.getQty();
            if(this.qty > otherQty)
            {
                return -1;
            }
            else if (this.qty == otherQty)
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
    }
}
