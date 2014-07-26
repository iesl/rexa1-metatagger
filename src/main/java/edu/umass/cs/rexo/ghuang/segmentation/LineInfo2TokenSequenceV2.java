package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;

import java.io.Serializable;
import java.util.*;

/**
 * Some added features to the original LineInfo2TokenSequence to help identify the references
 * 
 * @author kzaporojets based on ghuang's version of LineInfo2TokenSequence
 * 
 */
public class LineInfo2TokenSequenceV2 extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;

    LineInfo2TokenSequence tokenSequence ;


	public LineInfo2TokenSequenceV2()	{
        /*LineInfo2TokenSequence tokenSequence = new LineInfo2TokenSequence();*/
    }

	public Instance pipe(Instance carrier)
	{
		LineInfo[] oldData = (LineInfo[]) carrier.getData();
		Token[] dataTokens = new Token[oldData.length];
		Token[] sourceTokens = new Token[oldData.length];

//            tokenSequence.computeFeatures(oldData);

        try {
	    	computeFeatures(oldData);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }

		for (int i = 0; i < oldData.length; i++) {
			dataTokens[i] = new Token(oldData[i].text);
			Iterator iter = oldData[i].presentFeatures.iterator();
			
			sourceTokens[i] = new Token(oldData[i].text);
			
//			System.out.println(oldData[i].text);
			
			while (iter.hasNext()) {
				String featName = (String) iter.next();
				dataTokens[i].setFeatureValue(featName, 1);
				
//				System.out.println("\t" + featName);
			}
			
		}

		TokenSequence dataTS = new TokenSequence(dataTokens);
		TokenSequence sourceTS = new TokenSequence(sourceTokens);

		carrier.setData(dataTS);
		carrier.setSource(sourceTS);

		if (isTargetProcessing()) {
			Token[] targetTokens = new Token[dataTokens.length];
			
			for (int i = 0; i < dataTokens.length; i++)
				targetTokens[i] = new Token(oldData[i].trueLabel);

			TokenSequence targetTS = new TokenSequence(targetTokens);
			carrier.setTarget(targetTS);
		}

		return carrier;
	}

	
	
	private void computeFeatures(LineInfo[] lineInfos)
	{
		computeLexiconFeatures(lineInfos);
		computeLayoutFeatures(lineInfos);
	}
	
	private enum EnumerationType
    {
        PARENTHESIS,
        SQUARE_BRACKETS,
        NUMBER_CAPITAL,
        CAP_INITIALS,
        NONE
    }

    private enum IndentationType
    {
        INDENTED,
        UNTABBED,
        SAME
    }

	private static void computeLayoutFeatures(LineInfo[] lineInfos)
	{

        /*kzaporojets: some additional data with respect to layout include:
         - mode of the width (for all of the lines)
         - mode of llx a urx (for the lines on a particular page) to help identify the columns
         - mode of vertical distance between the lines for all the lines.
        */
        List <Entry<Integer>> verticalDistance = new ArrayList<Entry<Integer>>();
        List <Entry<Integer>> widthLine = new ArrayList<Entry<Integer>>();
        Map <Integer, List<Entry<ColumnData>>> columnsData = new HashMap<Integer,List<Entry<ColumnData>>>();
        //dimension of each of the pages
        Map <Integer, PageData> pagesData = new HashMap<Integer,PageData>();

        /*kzaporojetes: end of additional data, further in the code it will get completed*/

        int prevPageNum = 0;
        int prevFontNum = -1;

        int sumLineLengths = 0;
        int[] dist2prevLine = new int[lineInfos.length];
        HashMap refFontCounts = new HashMap();


        int indentedAfterFirst = 0;
        int untabbedAfterFirst = 0;
        int sameAfterFirst = 0;

        EnumerationType enumerationType = EnumerationType.NONE;
        for (int i = 0; i < lineInfos.length; i++) {

            if(lineInfos[i].presentFeatures.contains("firstReferenceLine") && lineInfos[i].presentFeatures.contains("seqHasBeginSquareBrackets") &&
                    lineInfos[i].presentFeatures.contains("beginSquareBrackets") )
            {
                enumerationType = EnumerationType.SQUARE_BRACKETS;
            }
            else if(lineInfos[i].presentFeatures.contains("firstReferenceLine") && lineInfos[i].presentFeatures.contains("seqHasBeginParenthesis") &&
                    lineInfos[i].presentFeatures.contains("beginParenthesis") )
            {
                enumerationType = EnumerationType.PARENTHESIS;
            }
            else if (lineInfos[i].presentFeatures.contains("firstReferenceLine") && lineInfos[i].presentFeatures.contains("seqHasBeginNumberCapital") &&
                    lineInfos[i].presentFeatures.contains("beginsNumberCapital") )
            {
                enumerationType = EnumerationType.NUMBER_CAPITAL;
            }

            //TODO: add the information about alignment
            if((enumerationType == EnumerationType.SQUARE_BRACKETS && lineInfos[i].presentFeatures.contains("beginSquareBrackets")) ||
                    (enumerationType == EnumerationType.PARENTHESIS && lineInfos[i].presentFeatures.contains("beginParenthesis")) ||
                    (enumerationType == EnumerationType.NUMBER_CAPITAL && lineInfos[i].presentFeatures.contains("beginsNumberCapital")))
            {
                lineInfos[i].presentFeatures.add("samePatternAsInFirst");
            }

            //todo: to be considered indented or tabbed, the intentation/tab should be at least of 3 px, see if it works and if more stats is needed
            if (lineInfos[i].page != prevPageNum) {
                lineInfos[i].presentFeatures.add("newPage");
                prevPageNum = lineInfos[i].page;

                if (i > 0)
                    lineInfos[i-1].presentFeatures.add("lastLineOnPage");
            }

            else if (i > 0 && (lineInfos[i].llx > lineInfos[i-1].urx && lineInfos[i].lly > lineInfos[i-1].lly))
                lineInfos[i].presentFeatures.add("newColumn");
            else if (i > 0 && lineInfos[i].llx > lineInfos[i-1].llx && (lineInfos[i].llx - lineInfos[i-1].llx)>2) {
                lineInfos[i].presentFeatures.add("indentedFromPrevLine");
                if(lineInfos[i-1].presentFeatures.contains("samePatternAsInFirst"))
                {
                    indentedAfterFirst++;
                }
            }
            else if (i > 0 && lineInfos[i].llx < lineInfos[i-1].llx && (lineInfos[i-1].llx - lineInfos[i].llx)>2) {
                lineInfos[i].presentFeatures.add("unTabbedFromPrevLine");
                if(lineInfos[i-1].presentFeatures.contains("samePatternAsInFirst") && (!lineInfos[i].presentFeatures.contains("samePatternAsInFirst"))) {
                    untabbedAfterFirst++;
                }
            }
            else if (i > 0 && lineInfos[i].llx == lineInfos[i-1].llx) {
                lineInfos[i].presentFeatures.add("sameIndentationAsPrevLine");
                if(lineInfos[i-1].presentFeatures.contains("samePatternAsInFirst") && (!lineInfos[i].presentFeatures.contains("samePatternAsInFirst"))) {
                    sameAfterFirst++;
                }
            }

            //detects possibly same line
            if(i>0 && lineInfos[i].lly == lineInfos[i-1].lly && lineInfos[i].llx > lineInfos[i-1].llx && lineInfos[i].urx > lineInfos[i-1].urx)
            {
                lineInfos[i].presentFeatures.add("sameLine");
            }

//            if(!lineInfos[i].presentFeatures.contains("newColumn") && )
//            {
//
//            }


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

//        Map <Integer, HashMap<ColumnData, Integer>> columnsData = new HashMap<Integer,HashMap<ColumnData,Integer>>();


            PageData pageData = pagesData.get(lineInfos[i].page);
            if(pageData==null)
            {
                pageData = new PageData();
                pageData.setBottomY(lineInfos[i].lly);
                pageData.setTopY(lineInfos[i].ury);
                pageData.setLeftX(lineInfos[i].llx);
                pageData.setRightX(lineInfos[i].urx);

                pagesData.put(lineInfos[i].page,pageData);
            }
            else
            {
                pageData.setBottomY(pageData.getBottomY()>lineInfos[i].lly?lineInfos[i].lly:pageData.getBottomY());
                pageData.setTopY(pageData.getTopY()<lineInfos[i].ury?lineInfos[i].ury:pageData.getTopY());
                pageData.setLeftX(pageData.getLeftX()>lineInfos[i].llx?lineInfos[i].llx:pageData.getLeftX());
                pageData.setRightX(pageData.getRightX()<lineInfos[i].urx?lineInfos[i].urx:pageData.getRightX());
            }

            ColumnData columnData = new ColumnData();
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

            if (i+1 < lineInfos.length && lineInfos[i].page == lineInfos[i + 1].page && lineInfos[i].lly > lineInfos[i+1].lly) {
                Integer vertDistance = lineInfos[i].lly - lineInfos[i + 1].lly;
                Entry<Integer > initialEntry = new Entry<Integer>(vertDistance,1);
                iOf = verticalDistance.indexOf(initialEntry);
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


            if (lineInfos[i].multibox)
                lineInfos[i].presentFeatures.add("containsMultiFonts");

            if (i == 0)
                prevFontNum = lineInfos[i].font;
            else if (lineInfos[i].font != prevFontNum) {
                prevFontNum = lineInfos[i].font;
                lineInfos[i].presentFeatures.add("startsNewFont");
            }

            if (lineInfos[i].presentFeatures.contains("beginBrackets")
                    || (! lineInfos[i].presentFeatures.contains("seqHasBeginBrackets")
                    && lineInfos[i].presentFeatures.contains("beginsNumberCapital"))
                    || (! lineInfos[i].presentFeatures.contains("seqHasBeginBrackets")
                    && ! lineInfos[i].presentFeatures.contains("seqHasBeginNumberCapital")
                    && lineInfos[i].presentFeatures.contains("beginsCapitalInitials"))) {

                String fontNum = Integer.toString(lineInfos[i].font);

                if (! refFontCounts.containsKey(fontNum))
                    refFontCounts.put(fontNum, "0");
                int newCount = 1 + Integer.parseInt((String) refFontCounts.get(fontNum));
                refFontCounts.put(fontNum, Integer.toString(newCount));
            }

            sumLineLengths += lineInfos[i].urx - lineInfos[i].llx;

            if (i > 0)
                dist2prevLine[i] = Math.abs(lineInfos[i-1].lly - lineInfos[i].lly);
        }

        IndentationType indentationType = IndentationType.SAME;
        if((Double.valueOf(sameAfterFirst)/Double.valueOf(sameAfterFirst+indentedAfterFirst+untabbedAfterFirst))>0.5)
        {
            indentationType = IndentationType.SAME;
        }
        else if ((Double.valueOf(indentedAfterFirst)/Double.valueOf(sameAfterFirst+indentedAfterFirst+untabbedAfterFirst))>0.5)
        {
            indentationType = IndentationType.INDENTED;
        }
        else if ((Double.valueOf(indentedAfterFirst)/Double.valueOf(sameAfterFirst+indentedAfterFirst+untabbedAfterFirst))>0.5)
        {
            indentationType = IndentationType.UNTABBED;
        }

        final int tolerance = 1; // difference in baseline y-coordinates must be greater than this to have "bigVertSpaceBefore" feature
        double avgLineLength = sumLineLengths / lineInfos.length;

        // Find the most common font number for probable reference begin lines
        int refFont = -1;
        int maxCount = 0;
        Iterator iter = refFontCounts.keySet().iterator();

        while (iter.hasNext()) {
            String key = (String) iter.next();
            int fontNum = Integer.parseInt(key);
            int count = Integer.parseInt((String) refFontCounts.get(key));

            if (count > maxCount) {
                maxCount = count;
                refFont = fontNum;
            }
        }


        Collections.sort(verticalDistance); //sortByValue(verticalDistance);
        //widthLine = sortByValue(widthLine);
        Collections.sort(widthLine);

        int refsEndingInPoint=0;
        int refsNotEndingInPoint=0;
        int totRefsSoFar = 0;
        int sumVertDistRefs = 0;


        int currentPage = lineInfos[0].page;
        boolean movedMargin = false;
        // A second pass of feature computations
        for (int i = 0; i < lineInfos.length; i++) {
            boolean ignore = false;


            if (lineInfos[i].urx - lineInfos[i].llx <= 0.75 * avgLineLength)
                lineInfos[i].presentFeatures.add("shortLineLength");

            if (i > 0
                    && !lineInfos[i].presentFeatures.contains("newPage")
                    && !lineInfos[i].presentFeatures.contains("newColumn")
                    && dist2prevLine[i] - dist2prevLine[i-1] > tolerance)
                lineInfos[i].presentFeatures.add("bigVertSpaceBefore");
            if (lineInfos[i].font == refFont)
                lineInfos[i].presentFeatures.add("usesRefFont");
            else if (refFont != -1 && ! lineInfos[i].presentFeatures.contains("containsMultiFonts"))
                lineInfos[i].presentFeatures.add("doesntUseRefFont");




            //width analysis "firstCommonWidth" && "secondCommonWidth"
            int currentWidth = lineInfos[i].urx - lineInfos[i].llx;

            int iOf = widthLine.indexOf(new Entry(currentWidth,0));
            if(iOf==0)
            {
                lineInfos[i].presentFeatures.add("firstCommonWidth");
            }
            else if(iOf==1)
            {
                lineInfos[i].presentFeatures.add("secondCommonWidth");
            }
            else if(iOf==2)
            {
                lineInfos[i].presentFeatures.add("thirdCommonWidth");
            }


            //vertical line analysis
            if(i+1<lineInfos.length && lineInfos[i].page == lineInfos[i+1].page && lineInfos[i].lly > lineInfos[i+1].lly)
            {
                int currVertDistance = lineInfos[i].lly - lineInfos[i+1].lly;
                //can be improved by checking the number of elements in each key. The spaces between the references should represent
                //at least 15% (maybe much more, try with up to 50%...) of the lines in references , if it is less, then can be header-footer
                if((verticalDistance.size()>1&&verticalDistance.indexOf(new Entry<Integer>(currVertDistance,0))>1))
                {
                    lineInfos[i].presentFeatures.add("verticalOutlier");
                }
                else if ((verticalDistance.size()>1&&verticalDistance.indexOf(new Entry<Integer>(currVertDistance,0))==1) &&
                        ( (double)verticalDistance.get(0).getQty()/(double)verticalDistance.get(1).getQty() > 0.15))
                {
                    lineInfos[i].presentFeatures.add("verticalSpace");
                }
            }

            //todo: see if this can be added in previous loop
            if(i>0 && !lineInfos[i].presentFeatures.contains("newColumn") && lineInfos[i].page==lineInfos[i-1].page && lineInfos[i].lly>lineInfos[i-1].lly)
            {
                ignore = true;
            }
            //if column starts on the left, ignore everything after that
            //todo: see it generalizes well
            if(i>0 && lineInfos[i].page==lineInfos[i-1].page && lineInfos[i].urx < lineInfos[i-1].llx && !lineInfos[i-1].presentFeatures.contains("sameLine"))
            {
                ignore = true;
                lineInfos[i].presentFeatures.add("ignoreAllPosteriorOnPage");
            }

            if(i>0 && lineInfos[i].page==lineInfos[i-1].page && lineInfos[i-1].presentFeatures.contains("ignoreAllPosteriorOnPage"))
            {
                ignore = true;
                lineInfos[i].presentFeatures.add("ignoreAllPosteriorOnPage");
            }

            //if for identifying the footer, and ignoring everything that's after it
            //todo: for now it is only adapted IndentationType.INDENTED, adapt to other indentations
            //todo: also track numbers in case (if the number from which footer begins, break the sequence, then ignore)
            //signs to look for:
            //1- previous line ends in point and to the left.
            //2- average vertical distance BETWEEN the references, if it escapes +- 2 px ?  (todo: calculate)
            //3- if the references in general end in point (the percentage ended in point)
            //4- the percentile of lower web page y . < 0.08
            //5- what else?

            if(totRefsSoFar>0) {
                int avgDistBetwRef = (int) (Double.valueOf(sumVertDistRefs) / Double.valueOf(totRefsSoFar));
                //add 10%
                int maxLimitDist = avgDistBetwRef + (int) (Double.valueOf(avgDistBetwRef) * 0.1);
                double percentile = (Double.valueOf(lineInfos[i].lly - pagesData.get(lineInfos[i].page).getBottomY())) /
                        (Double.valueOf(pagesData.get(lineInfos[i].page).getHeight()));
                if (i>0 && indentationType == IndentationType.INDENTED && !lineInfos[i].presentFeatures.contains("newColumn") && lineInfos[i].page ==
                        lineInfos[i-1].page &&
                    //2-
                        lineInfos[i-1].lly > lineInfos[i].lly && lineInfos[i-1].lly - lineInfos[i].lly > maxLimitDist
                    //4-
                        && percentile < 0.08
                        ) {
                    ignore = true;
                    lineInfos[i].presentFeatures.add("ignoreAllPosteriorOnPage");
                }
            }

            //todo: think in something else, this feature has multiple values, not only one
            if(lineInfos[i].presentFeatures.contains("verticalOutlier"))
            {
                //ignore = true;
            }

            if(!ignore) {

                //todo:redo the two following if, so they are exclusive
                if ((!movedMargin && lineInfos[i].presentFeatures.contains("samePatternAsInFirst")) ||
                        //the following is for deal well with ref[3] of 1997Fey_The_affects_of_stoichiometry..., see how it works, if not delete delete? :
                        (!movedMargin && i > 0 && !lineInfos[i].presentFeatures.contains("newPage") && !lineInfos[i].presentFeatures.contains("newColumn")
                                && lineInfos[i - 1].presentFeatures.contains("possibleInit") &&
                                !lineInfos[i].presentFeatures.contains("indentedFromPrevLine") && indentationType == IndentationType.INDENTED)
                        )
                //end: the following...
                {
                    lineInfos[i].presentFeatures.add("possibleInit");
                }

                if (!movedMargin && indentationType == IndentationType.INDENTED && (i + 1) < lineInfos.length && lineInfos[i + 1].presentFeatures.contains("indentedFromPrevLine") &&
                        (!lineInfos[i].presentFeatures.contains("bibliography")) //&&
                        //this latter is to prevent footers to be taken as references as in 1301.3781.pdf
                        //lineInfos[i].presentFeatures.contains("samePatternAsInFirst")
                        ) {

                    lineInfos[i].presentFeatures.add("possibleInit");
                    movedMargin = true;
//                    int sumVertDistRefs = 0;
                }

                if(lineInfos[i].presentFeatures.contains("possibleInit"))
                {
                    refsEndingInPoint = refsEndingInPoint + refEndsInPoint(i,lineInfos);
                    refsNotEndingInPoint = refsNotEndingInPoint + refNotEndsInPoint(i,lineInfos);
                    int vertDist=vertDifFromPrevRef(i,lineInfos);
                    if(vertDist>0) {
                        totRefsSoFar++;
                        sumVertDistRefs += vertDifFromPrevRef(i, lineInfos);
                    }
                }




            }
            else
            {
                lineInfos[i].presentFeatures.add("ignore");
            }

            if (
                    //sometimes the two "lines" actually belong to the same line , this first condition takes it into account
//                    (i==0 || (i>0 && lineInfos[i-1].llx != lineInfos[i].llx))
//            &&
            ((indentationType == IndentationType.INDENTED && (i+1)<lineInfos.length && lineInfos[i+1].presentFeatures.contains("unTabbedFromPrevLine")) ||
                    ((i+1)<lineInfos.length && lineInfos[i+1].presentFeatures.contains("newColumn") && lineInfos[i+1].presentFeatures.contains("samePatternAsInFirst")) ||
                    ((i+1)<lineInfos.length && lineInfos[i+1].presentFeatures.contains("newPage") && lineInfos[i+1].presentFeatures.contains("samePatternAsInFirst")) ) )
            {
                movedMargin = false;
            }

            //todo: "possibleColumn" (implement only if necessary based on pdfs tests), the first widest that is not overlapping with the second? See overlapColumnDatas method
            //certain % with respect to the first most common and overlapping: the indent shouldn't be wider/narrower
            //than 10% with respect to the most common? check it!
            //also check the total page width
            //also take into account that the width of each of the columns should be practically the same, maybe with +-5% of diff?
            //also take into account the x initial position of the line, that should be the same that one of the coordinates of the column...
            // not in all cases :( , but equally should heavily base on this (maybe with +-5% of tolerance), as well as in the vertical distances! Be careful, sometimes when there is
            // enumeration, such as in 2013Twu_LiMnF4.pdf, the numbers are differently left-aligned according to the range: 1-9; 10-99; 100-999, can include
            //check for this!
            //it the vertical distance also should be taken into account
            //in some cases there is no specific width, such as in p1361-whissell.pdf
            //Try to do references from paper_2_10.pdf!!!: check if the sentence ends in a point! ("noEndingPeriod"), but some of the references don't end in a
            // point such as paper3_10.pdf or paper4_3.pdf
            // paper5_1.pdf examples of when not all lines aligned on the left on second line of the reference.


            //function shape should be as follows
            //columns = getColumnsCoordinates

            currentPage = lineInfos[i].page;
        }

        //also see if it is possible to get a notion where do they start (the columns)
	}

    private static int refEndsInPoint(int i, LineInfo[] lineInfos)
    {
        if(i>0 && !lineInfos[i-1].presentFeatures.contains("bibliography") && !lineInfos[i-1].presentFeatures.contains("noEndingPeriod"))
        {
            return 1;
        }
        return 0;
    }
    private static int refNotEndsInPoint(int i, LineInfo[] lineInfos)
    {
        if(i>0 && !lineInfos[i-1].presentFeatures.contains("bibliography") && lineInfos[i-1].presentFeatures.contains("noEndingPeriod"))
        {
            return 1;
        }
        return 0;
    }
    private static int vertDifFromPrevRef(int i, LineInfo[] lineInfos)
    {
        if(i>0 && !lineInfos[i-1].presentFeatures.contains("bibliography") && lineInfos[i].page == lineInfos[i-1].page &&
                !lineInfos[i].presentFeatures.contains("newColumn") && lineInfos[i].lly < lineInfos[i-1].lly)
        {
            return lineInfos[i-1].lly - lineInfos[i].lly;
        }
        return 0;
    }

//
//    public static <K, V extends Comparable<? super V>> Map<K, V>
//                                                    sortByValue( Map<K, V> map )
//    {
//        List<Map.Entry<K, V>> list =
//                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
//        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
//        {
//            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
//            {
//                return (o1.getValue()).compareTo( o2.getValue() );
//            }
//        } );
//
//        Map<K, V> result = new LinkedHashMap<K, V>();
//        for (Map.Entry<K, V> entry : list)
//        {
//            result.put( entry.getKey(), entry.getValue() );
//        }
//        return result;
//    }

    private boolean overlapColumnDatas(ColumnData columnData1, ColumnData columnData2)
    {
        if((columnData1.getLeftX()>=columnData2.getLeftX() && columnData1.getLeftX()<=columnData2.getRightX()) ||
                (columnData1.getRightX()>=columnData2.getLeftX() && columnData1.getRightX()<=columnData2.getRightX()) ||
                (columnData1.getLeftX()<=columnData2.getLeftX() && columnData1.getRightX()>=columnData2.getRightX()) ||
                (columnData1.getLeftX()>=columnData2.getLeftX() && columnData1.getRightX()<=columnData2.getRightX()))
        {
            return true;
        }
        return false;
    }

	private void computeLexiconFeatures(LineInfo[] lineInfos)
	{
        String[] keywords = { "Proceedings", "Proc\\.", "Conference",
                "Workshop", "Technical ", "Tech\\. ", "Report", "Symposium",
                "Symp\\.", "Journal", "Lecture ", "Lect\\. ", "Notes ",
                "Computer ", "Science " };
        // high correlation with non-bibliographic content
        String[] postwords = { "^[^A-Za-z]*Received[^A-Za-z]",
                "^[A-Za-z]*Figure(s)?[^A-Za-z]",
                "^[A-Za-z]*Table(s)?[^A-Za-z]", "^[A-Za-z]*Graph(s)?[^A-Za-z]",
                "We ", " we ", "She ", " she ", "He ", " he ", "Our ", " our ",
                "Her ", " her ", "His ", " his ", "These ", " these ", "Acknowledgements" };
        // moderate correlation with non-bibliographic content
        String[] lowPostwords = { "They ", " they ", "This ", " this ", " is ",
                " are ", " was ", " have ", " but ", "[a-z]+\\s+[a-z]+ed " };
        String[] months = {"January", "Jan\\.?\\s", "February", "Feb\\.?\\s", "March", "Mar\\.?\\s",
                "April", "Apr\\.?\\s", "May", "June", "Jun\\.?\\s", "July", "Jul\\.\\s?",  "August", "Aug\\.?\\s",
                "September", "Sept?\\.?\\s", "October", "Oct\\.?\\s",  "November", "Nov\\.?\\s", "December", "Dec\\.?\\s" };

        int numBeginBrackets = 0;
        int numBeginSquareBrackets = 0;
        int numBeginParenthesis = 0;
        int numBeginNumberCapital = 0;
        int numBeginCapInitials = 0;
        int numPages = 1;
        int prevPage = 0;

        int biblioTitleIndex = -1;

        for (int i = 0; i < lineInfos.length; i++) {

            if (i == 0)
                prevPage = lineInfos[i].page;
            else if (lineInfos[i].page != prevPage) {
                numPages++;
                prevPage = lineInfos[i].page;
            }



            String squishedText = lineInfos[i].text.replaceAll("\\s", "");

            if (squishedText.length() == 0) continue;

            int numPeriodCommas = specialPunctCounter(squishedText);

            if (numPeriodCommas == 0)
                lineInfos[i].presentFeatures.add("noSpecialPuncts");
            else if (numPeriodCommas > 3)
                lineInfos[i].presentFeatures.add("manySpecialPuncts");
            else
                lineInfos[i].presentFeatures.add("someSpecialPuncts");

            if (squishedText.matches("^\\[.+\\].*")) {
                //"beginBrackets" is needed to work with crf as it is, "beginSquareBrackets" is for rules.
                lineInfos[i].presentFeatures.add("beginBrackets");
                lineInfos[i].presentFeatures.add("beginSquareBrackets");
                numBeginBrackets++;
                numBeginSquareBrackets++;
            }
            //kzaporojets: the numbering of some references start with parenthesis
            if (squishedText.matches("^\\(.+\\).*")) {
                //"beginBrackets" is needed to work with crf as it is, "beginParenthesis" is for rules.
                lineInfos[i].presentFeatures.add("beginBrackets");
                lineInfos[i].presentFeatures.add("beginParenthesis");
                numBeginBrackets++;
                numBeginParenthesis++;
            }
            //kzaporojets: some other features
            if (squishedText.matches("^\\([0-9]+\\).*")) {
                lineInfos[i].presentFeatures.add("beginNumericBrackets");
                //numBeginBrackets++;
            }

            if (biblioTitleIndex>-1 && (i-biblioTitleIndex)==1) {
                lineInfos[i].presentFeatures.add("firstReferenceLine");
            }

            if (biblioTitleIndex==-1 &&
                    squishedText.matches("^[#iIvVxX\\d\\.\\s]{0,5}(R(?i:eferences)|B(?i:ibliography)|R(?i:eferencesandNotes)|L(?i:iteratureCited))\\s*$")) {
                biblioTitleIndex=i;
                lineInfos[i].presentFeatures.add("bibliography");
            }

//            if (squishedText.matches("(^)[0-9]+\\.?\\p{Lu}.*")) {
//                lineInfos[i].presentFeatures.add("beginsNumber");
//               // numBeginNumberCapital++;
//            }

            //kzaporojets: end some other features
            if (squishedText.matches("^[0-9]+\\.?\\p{Lu}.*")) {
                lineInfos[i].presentFeatures.add("beginsNumberCapital");
                numBeginNumberCapital++;
            }
            if (! squishedText.endsWith("."))
                lineInfos[i].presentFeatures.add("noEndingPeriod");
            if (squishedText.matches(".*[^\\p{Ll}\\p{Lu}]\\p{Lu}\\.$"))
                lineInfos[i].presentFeatures.add("endsWithCapPeriod");
            if (squishedText.matches(".*[0-9]+-(-)?[0-9]+.*"))
                lineInfos[i].presentFeatures.add("containsPageRange");
            if (squishedText.matches(".*(19|20)\\d{2}.*"))
                lineInfos[i].presentFeatures.add("containsYear");
            if (squishedText.matches(".*(?i)appendix.*"))
                lineInfos[i].presentFeatures.add("containsAppendix");
            if (squishedText.matches(".*(?i)received.*"))
                lineInfos[i].presentFeatures.add("containsReceived");
            if (squishedText.matches(".*(?i)address.*"))
                lineInfos[i].presentFeatures.add("containsAddress");
            if (squishedText.matches(".*\\w+@\\w+\\.\\w+.*"))
                lineInfos[i].presentFeatures.add("containsEmail");
            if (squishedText.matches(".*(ftp|http)\\://\\w+\\.\\w+.*"))
                lineInfos[i].presentFeatures.add("containsURL");
            if (squishedText.matches(".*[,\\-\\:]$"))
                lineInfos[i].presentFeatures.add("endsWithPunctNotPeriod");
            if (squishedText.matches(".*\\d.*"))
                lineInfos[i].presentFeatures.add("containsDigit");

            if (lineInfos[i].text.matches(".*et(\\.)?\\sal.*"))
                lineInfos[i].presentFeatures.add("containsEtAl");
            if (lineInfos[i].text.matches("^(\\p{Lu}\\.\\s*)+\\s+[\\p{Lu}\\p{Ll}]+.*")  // M. I. Jordan
                    || squishedText.matches("^\\p{Lu}[\\p{Lu}\\p{Ll}]+\\,\\p{Lu}[\\.,].*")) {  // Jordan, M. or Jordan,M. or Jordan, M,
                lineInfos[i].presentFeatures.add("beginsCapitalInitials");
                numBeginCapInitials++;
            }

            for (int j = 0; j < keywords.length; j++) {
                if (lineInfos[i].text.matches(".*" + keywords[j] + ".*")) {
                    lineInfos[i].presentFeatures.add("containsKeyword");
                    break;
                }
            }
            for (int j = 0; j < postwords.length; j++) {
                if (lineInfos[i].text.matches(".*" + postwords[j] + ".*")) {
                    lineInfos[i].presentFeatures.add("containsPostword1");
                    break;
                }
            }
            for (int j = 0; j < lowPostwords.length; j++) {
                if (lineInfos[i].text.matches(".*" + lowPostwords[j] + ".*")) {
                    lineInfos[i].presentFeatures.add("containsPostword2");
                    break;
                }
            }
            for (int j = 0; j < months.length; j++) {
                if (lineInfos[i].text.matches(".*" + months[j] + ".*")) {
                    lineInfos[i].presentFeatures.add("containsMonth");
                    break;
                }
            }
        }

//		System.out.println("ppppppppp numPages=" + numPages);

        // Features based on the entire sequence
        // Very long biblio->end section probably means there's an appendix, so
        // there's more chance of encountering spurious reference begin markers.
        // If a paper contains an appendix, assume it cites at least 5 papers
        double threshold = (numPages > 2) ? 4  : 1;
        boolean seqHasBeginBrackets = false;
        boolean seqHasBeginNumberCapital = false;
        boolean seqHasBeginSquareBrackets = false;
        boolean seqHasBeginParenthesis = false;
        int max = 0;

        if (numBeginSquareBrackets > numBeginNumberCapital && numBeginSquareBrackets > numBeginCapInitials && numBeginSquareBrackets > numBeginParenthesis) {
            seqHasBeginSquareBrackets = true;
            max = numBeginSquareBrackets;
        }
        else if (numBeginParenthesis > numBeginNumberCapital && numBeginParenthesis > numBeginCapInitials && numBeginParenthesis > numBeginSquareBrackets)
        {
            seqHasBeginParenthesis = true;
            max = numBeginParenthesis;
        }
        else if (numBeginNumberCapital > numBeginSquareBrackets && numBeginNumberCapital > numBeginCapInitials && numBeginNumberCapital > numBeginParenthesis) {
            seqHasBeginNumberCapital = true;
            max = numBeginNumberCapital;
        }
        else
            max = numBeginCapInitials;

        if (max <= threshold)
            return;

        for (int i = 0; i < lineInfos.length; i++) {
            if (seqHasBeginSquareBrackets)
                lineInfos[i].presentFeatures.add("seqHasBeginSquareBrackets");
            else if (seqHasBeginParenthesis)
            {
                lineInfos[i].presentFeatures.add("seqHasBeginParenthesis");
            }
            else if (seqHasBeginNumberCapital)
                lineInfos[i].presentFeatures.add("seqHasBeginNumberCapital");
            else
                lineInfos[i].presentFeatures.add("seqHasBeginCapInitials");
        }
	}

	private static int specialPunctCounter(String s)
	{
		int count = 0; 
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == ',' || c == '.' || c == ':') 
				count++;
		}
		
		return count;
	}
	
}

class Entry<T1> implements Comparable<Entry<T1>>
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

class ColumnData
{

    private int topY;
    private int bottomY;

    private int leftX;
    private int rightX;

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
        return ((ColumnData)obj).rightX == this.rightX &&
                ((ColumnData)obj).leftX == this.leftX;
    }

    @Override
    public int hashCode()
    {
        return Integer.valueOf(leftX).hashCode();
    }
}

class PageData
{

    private int topY;
    private int bottomY;

    private int leftX;
    private int rightX;

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

//    @Override
//    public boolean equals(Object obj )
//    {
//        return ((ColumnData)obj).rightX == this.rightX &&
//                ((ColumnData)obj).leftX == this.leftX;
//    }

//    @Override
//    public int hashCode()
//    {
//        return Integer.valueOf(leftX).hashCode();
//    }
}