package edu.umass.cs.rexo.ghuang.segmentation.utils;

import edu.umass.cs.rexo.ghuang.segmentation.LineInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by klimzaporojets on 8/5/14.
 * Some utility functions related to layout.
 */
public class LayoutUtils {



    public static List<ColumnData> getColumns(List<Entry<ColumnData>> allSpans, PageData pageData)
    {
        List<ColumnData> columnList = new ArrayList<ColumnData>();

        //by default, considers that the first column, is the one that appears first in the list of allSpans grouped by qty
        ColumnData firstColumn = allSpans.get(0).getKey();
        columnList.add(firstColumn);

//        int columnsSoFar = 1;
        int widthSoFar = firstColumn.getWidth();

        if(firstColumn.getWidth()>((double)pageData.getWidth())/2.0)
        {
            return columnList;
        }

        for(Entry<ColumnData> colData:allSpans)
        {
            if(!isOverlapping(columnList,colData.getKey()) && isWidthSimilar(columnList,colData.getKey(),0.95))
            {
                //add column
                columnList.add(colData.getKey());
                //update the accumulated width and counter of cols
                widthSoFar =+ colData.getKey().getWidth();
//                columnsSoFar++;
                //check if continue the loop by checking the accumulatedWidth
                if(widthSoFar + firstColumn.getWidth() > pageData.getWidth())
                {
                    break;
                }
            }
        }
        return columnList;
    }

    private static boolean isOverlapping(List<ColumnData> columns, ColumnData columnToCheck)
    {
        for(ColumnData col: columns)
        {
            if((col.getLeftX()>=columnToCheck.getLeftX() && col.getLeftX()<=columnToCheck.getRightX()) ||
                    (col.getRightX()>=columnToCheck.getLeftX() && col.getRightX()<=columnToCheck.getRightX()) ||
                    (col.getLeftX()<=columnToCheck.getLeftX() && col.getRightX()>=columnToCheck.getRightX()) ||
                    (col.getLeftX()>=columnToCheck.getLeftX() && col.getRightX()<=columnToCheck.getRightX()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isWidthSimilar(List<ColumnData> columns, ColumnData columnToCheck, double errorRatio)
    {
        for(ColumnData col: columns)
        {
            if(columnToCheck.getWidth() < ((double)col.getWidth())*errorRatio ||
                    columnToCheck.getWidth() > ((double)col.getWidth())*(2.0 - errorRatio))
            {
                return false;
            }
        }
        return true;

    }




    public static void adjustPageData(LineInfo[] lineInfos, int i, Map <Integer, PageData> pagesData)
    {
        PageData pageData = pagesData.get(lineInfos[i].page);
        if(pageData==null)
        {
            pageData = new PageData();
            pageData.setBottomY(lineInfos[i].lly);
            pageData.setTopY(lineInfos[i].ury);
            pageData.setLeftX(lineInfos[i].llx);
            pageData.setRightX(lineInfos[i].urx);
            pageData.setPageNumber(lineInfos[i].page);
            pagesData.put(lineInfos[i].page,pageData);
        }
        else
        {
            pageData.setBottomY(pageData.getBottomY()>lineInfos[i].lly?lineInfos[i].lly:pageData.getBottomY());
            pageData.setTopY(pageData.getTopY()<lineInfos[i].ury?lineInfos[i].ury:pageData.getTopY());
            pageData.setLeftX(pageData.getLeftX()>lineInfos[i].llx?lineInfos[i].llx:pageData.getLeftX());
            pageData.setRightX(pageData.getRightX()<lineInfos[i].urx?lineInfos[i].urx:pageData.getRightX());
        }

    }


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




    public static void adjustColumnData(LineInfo[]lineInfos, int i, Map <Integer, List<Entry<ColumnData>>> columnsData, boolean equalsBothMargins)
    {
        ColumnData columnData = new ColumnData(equalsBothMargins);
        columnData.setLeftX(lineInfos[i].llx);
        columnData.setRightX(lineInfos[i].urx);
        columnData.setTopY(lineInfos[i].ury);
        columnData.setBottomY(lineInfos[i].lly);

        if(columnsData.get(lineInfos[i].page)==null)
        {
            List <Entry<ColumnData>> colData = new ArrayList<Entry<ColumnData>>();
            colData.add(new Entry<ColumnData>(columnData, 1));
            columnsData.put(lineInfos[i].page,colData);
        }
        else
        {
            Entry<ColumnData> currEntry = new Entry<ColumnData>(columnData,1);
            List entriesInThePage = columnsData.get(lineInfos[i].page);
            int iOe = entriesInThePage.indexOf(currEntry);
            if(iOe>-1)
            {
                Entry<ColumnData> existentEntry = columnsData.get(lineInfos[i].page).get(iOe);
                existentEntry.setQty(existentEntry.getQty()+1);
                if(lineInfos[i].ury>existentEntry.getKey().getTopY()) {
                    existentEntry.getKey().setTopY(lineInfos[i].ury);
                }
                if(lineInfos[i].lly<existentEntry.getKey().getBottomY()) {
                    existentEntry.getKey().setBottomY(lineInfos[i].lly);
                }
            }
            else
            {
                columnsData.get(lineInfos[i].page).add(currEntry);
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


    public static class ColumnData
    {

        private int topY=-1;
        private int bottomY=-1;

        private int leftX=-1;
        private int rightX=-1;
        boolean equalsBothMargins = false;

        public ColumnData()
        {

        }


        public ColumnData(boolean equalsBothMargins)
        {

        }
        public boolean isInitialized()
        {
            return !(topY==-1 && bottomY==-1 && leftX == -1 && rightX == -1);
        }
        public boolean isEqualsBothMargins() {
            return equalsBothMargins;
        }

        public void setEqualsBothMargins(boolean equalsBothMargins) {
            this.equalsBothMargins = equalsBothMargins;
        }
        public int getTopY() {
            return topY;
        }

        public void setTopY(int topY) {
            this.topY = topY;
        }

        public int getBottomY() {
            return bottomY;
        }

        public void setBottomY(int bottomY) {
            this.bottomY = bottomY;
        }

        public int getLeftX() {
            return leftX;
        }

        public void setLeftX(int leftX) {
            this.leftX = leftX;
        }

        public int getRightX() {
            return rightX;
        }

        public void setRightX(int rightX) {
            this.rightX = rightX;
        }

        public int getWidth() {
            return (rightX - leftX);
        }

        @Override
        public boolean equals(Object obj )
        {
            if(!equalsBothMargins) {
                return //((ColumnData)obj).rightX == this.rightX &&
                        ((ColumnData) obj).leftX == this.leftX;
            }
            else
            {
                return ((ColumnData)obj).rightX == this.rightX &&
                        ((ColumnData) obj).leftX == this.leftX;
            }
        }

        @Override
        public int hashCode()
        {
            return Integer.valueOf(leftX).hashCode();
        }
    }


    public static class PageData
    {

        private int topY;
        private int bottomY;

        private int leftX;
        private int rightX;

        private int pageNumber;

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public int getTopY() {
            return topY;
        }

        public void setTopY(int topY) {
            this.topY = topY;
        }

        public int getBottomY() {
            return bottomY;
        }

        public void setBottomY(int bottomY) {
            this.bottomY = bottomY;
        }

        public int getLeftX() {
            return leftX;
        }

        public void setLeftX(int leftX) {
            this.leftX = leftX;
        }

        public int getRightX() {
            return rightX;
        }

        public void setRightX(int rightX) {
            this.rightX = rightX;
        }

        public int getWidth() {
            return (rightX - leftX);
        }
        public int getHeight(){
            return (topY - bottomY);
        }

    }
}
