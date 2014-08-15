package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.rexo.ghuang.segmentation.utils.LayoutUtils;
import edu.umass.cs.rexo.ghuang.segmentation.utils.LayoutUtils.ColumnData;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.span.CompositeSpan;
import org.rexo.util.EnglishDictionary;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by klimzaporojets on 8/4/14.
 * Adds useful features to identify parts of the body of a particular paper.
 */
public class Token2BodyFeatureSequence  extends Pipe implements Serializable {

    private static String lonelyNumbers = "[1-9][\\.]{0,1}[\\s]+]"; //"[1-9][\\.]{0,1}";
    private static String lonelyLetters = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ][\\.]{0,1}[\\s]+]"; //"[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ][\\.]{0,1}";



    static Pattern ptrnLonelyNumbers = Pattern.compile(lonelyNumbers);
    static Pattern ptrnLonelyLetters = Pattern.compile(lonelyLetters);
    static List <LayoutUtils.Entry<Integer>> wordsInDictionaryPerLine = new ArrayList<LayoutUtils.Entry<Integer>>();


    private static int columnAcceptableError = 5; //pixels of sloppiness within a column accepted

    @Override
    public Instance pipe(Instance carrier) {
        wordsInDictionaryPerLine = new ArrayList<LayoutUtils.Entry<Integer>>();
        NewHtmlTokenization data = (NewHtmlTokenization)carrier.getData();

        //List<CompositeSpan> lineSpans = data.getLineSpans();
        NewHtmlTokenization2LineInfo nhtml2LineInfo = new NewHtmlTokenization2LineInfo();
        Instance onlyLines =  nhtml2LineInfo.pipe(carrier);
        EnglishDictionary dictionary = EnglishDictionary.createDefault();
        computeFeatures((LineInfo [])onlyLines.getData(),data, dictionary);

        carrier.setData(data);

        return carrier;
    }


    private void computeFeatures(LineInfo[] lineInfos, NewHtmlTokenization data, EnglishDictionary dictionary)
    {
        computeLexiconFeatures(data, dictionary);
        computeLayoutFeatures(lineInfos, data);
    }

    private static void computeLayoutFeatures(LineInfo[] lineInfos, NewHtmlTokenization data) {
        List <LayoutUtils.Entry<Integer>> verticalDistance = new ArrayList<LayoutUtils.Entry<Integer>>();
        List <LayoutUtils.Entry<Integer>> lineWidth = new ArrayList<LayoutUtils.Entry<Integer>>();
        List <LayoutUtils.Entry<Integer>> pixelsPerCharacter = new ArrayList<LayoutUtils.Entry<Integer>>();


        //it can be the case when the first page has one column and the rest of the pages two for example, this is why it is important
        //to have a per-page width stats
        Map<Integer, List<LayoutUtils.Entry<Integer>>> widthLinePerPage = new HashMap <Integer, List<LayoutUtils.Entry<Integer>>>();
        Map <Integer, List<LayoutUtils.Entry<ColumnData>>> columnsData = new HashMap<Integer,List<LayoutUtils.Entry<ColumnData>>>();
        Map <Integer, List<LayoutUtils.Entry<ColumnData>>> leftMarginsData = new HashMap<Integer,List<LayoutUtils.Entry<ColumnData>>>();
        Map <Integer, LayoutUtils.PageData> pagesData = new HashMap<Integer,LayoutUtils.PageData>();
        Map <Integer, List<ColumnData>> columns = new HashMap<Integer,List<ColumnData>>();
        List<LayoutUtils.Entry<Integer>> lineHeight = new ArrayList<LayoutUtils.Entry<Integer>>();


        int prevPageNum = 0;
        int lineSpanCount = -1;

        //first scan to calculate general statistics of the paper (such as line width, or vertical distances between lines)
        for(int i =0; i<lineInfos.length; i++ )
        {
            lineSpanCount = updateLineSpanCounter(data, lineSpanCount + 1);

            Span lineSpan = (Span)data.getLineSpans().get(lineSpanCount);


            if (lineInfos[i].page != prevPageNum) {
                LayoutUtils.setFeatureValue(lineSpan,"newPage",1.0);
                prevPageNum = lineInfos[i].page;
                if (i > 0) {
                    LayoutUtils.setFeatureValue((Span) data.getLineSpans().get(i - 1), "lastLineOnPage", 1.0);
                }
            }
            else if (i > 0 && (lineInfos[i].llx > lineInfos[i-1].urx && lineInfos[i].lly > lineInfos[i-1].lly)) {
                LayoutUtils.setFeatureValue(lineSpan, "newColumn", 1.0);
            }
            else if (i>0 && (lineInfos[i].llx <= lineInfos[i-1].urx && lineInfos[i].lly > lineInfos[i-1].lly))
            {
                LayoutUtils.setFeatureValue(lineSpan,"upAndToTheLeft", 1.0);
            }

            if (i>0 && (lineInfos[i].lly > lineInfos[i-1].lly))
            {
                LayoutUtils.setFeatureValue(lineSpan,"up", 1.0);

                if(lineInfos[i].lly - lineInfos[i-1].lly > 20)
                {
                    LayoutUtils.setFeatureValue(lineSpan,"up20PxGreater", 1.0);
                }
            }

            if (i>0 && (lineInfos[i].llx > lineInfos[i-1].urx))
            {
                LayoutUtils.setFeatureValue(lineSpan,"right", 1.0);
            }

            LayoutUtils.adjustLineHeight(lineInfos, i, lineHeight);
            LayoutUtils.adjustVerticalDistance(lineInfos, i, verticalDistance);
            LayoutUtils.adjustLineWidth(lineInfos, i, lineWidth);
            LayoutUtils.adjustLineWidthPerPage(lineInfos, i, widthLinePerPage);
            LayoutUtils.adjustColumnData(lineInfos, i, columnsData, true, columnAcceptableError);
            LayoutUtils.adjustColumnData(lineInfos, i, leftMarginsData, false, 0);
            LayoutUtils.adjustPageData(lineInfos, i, pagesData);
            LayoutUtils.adjustPixelsPerCharacter(lineInfos, i, pixelsPerCharacter);
        }
        Collections.sort(verticalDistance);
        Collections.sort(lineHeight);
        Collections.sort(lineWidth);
        Collections.sort(pixelsPerCharacter);
        for(Integer page: widthLinePerPage.keySet())
        {
            Collections.sort(widthLinePerPage.get(page));
            Collections.sort(columnsData.get(page));
            Collections.sort(leftMarginsData.get(page));

//            List<ColumnData> currentPageCols = LayoutUtils.getColumns(columnsData.get(page),pagesData.get(page));
            List<ColumnData> currentPageCols = LayoutUtils.getColumnsV2(columnsData.get(page), pagesData.get(page));

            Collections.sort(currentPageCols, new Comparator<ColumnData>() {
                @Override
                public int compare(ColumnData o1, ColumnData o2) {
                    if(o1.getLeftX()>o2.getLeftX())
                    {
                        return 1;
                    }
                    else if (o1.getLeftX()<o2.getLeftX())
                    {
                        return -1;
                    }
                    else
                    {
                        return 0;
                    }

                }
            });
            columns.put(page,currentPageCols);
        }

        ColumnData lastLineColumn = null;
        lineSpanCount = -1;
        //second scan to calculate more detailed features based on the statistics of the first scan
        for(int i =0; i<lineInfos.length; i++ )
        {
            ColumnData currentLineColumn  = LayoutUtils.getCurrentLineColumn(lineInfos,i,columns.get(lineInfos[i].page),
                    true, columnAcceptableError, lastLineColumn, verticalDistance.get(0).getKey()) ;
            lineSpanCount = updateLineSpanCounter(data, lineSpanCount + 1);
            Span lineSpan = (Span)data.getLineSpans().get(lineSpanCount); // (Span)data.getLineSpans().get(i);
            ColumnData sloppyColumn = null;
            if(currentLineColumn == null)
            {
                LayoutUtils.setFeatureValue(lineSpan,"noColumnAssociated",1.0);

                sloppyColumn = LayoutUtils.getClosestCurrentLineColumn(lineInfos, i, columns.get(lineInfos[i].page),
                        true, columnAcceptableError, true);
                if(sloppyColumn!=null)
                {
                    LayoutUtils.setFeatureValue(lineSpan,"sloppyStrictLeft",1.0);
                }
                else
                {
                    sloppyColumn = LayoutUtils.getClosestCurrentLineColumn(lineInfos, i, columns.get(lineInfos[i].page),
                            true, columnAcceptableError, false);
                    if(sloppyColumn != null)
                    {
                        LayoutUtils.setFeatureValue(lineSpan,"onlySloppy",1.0);
                    }
                }

                if(sloppyColumn!=null)
                {
                    int vertMarginColumnDiff = 0;
                    if (lineInfos[i].lly < sloppyColumn.getBottomY()) {
                        vertMarginColumnDiff = sloppyColumn.getBottomY() - lineInfos[i].lly;
                        LayoutUtils.setFeatureValue(lineSpan, "lineBelowColumn", 1.0);
                    }
                    if (lineInfos[i].ury > sloppyColumn.getTopY()) {
                        vertMarginColumnDiff = lineInfos[i].ury - sloppyColumn.getTopY();
                        LayoutUtils.setFeatureValue(lineSpan, "lineAboveColumn", 1.0);
                    }

                    if (vertMarginColumnDiff > 0) {
                        if (vertMarginColumnDiff >= 10) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn10px", 1.0);
                        }
                        if (vertMarginColumnDiff >= 20) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn20px", 1.0);
                        }
                        if (vertMarginColumnDiff >= 30) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn30px", 1.0);
                        }
                        if (vertMarginColumnDiff >= 40) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn40px", 1.0);
                        }
                        if (vertMarginColumnDiff >= 50) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn50px", 1.0);
                        }
                        if (vertMarginColumnDiff >= 60) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn60px", 1.0);
                        }
                        if (vertMarginColumnDiff >= 100) {
                            LayoutUtils.setFeatureValue(lineSpan, "vertOutsideColumn100px", 1.0);
                        }
                    }
                }
            }
            else
            {
                if(lastLineColumn!=null){
                    if(!lastLineColumn.equals(currentLineColumn))
                    {
                        LayoutUtils.setFeatureValue(lineSpan,"columnLayoutChange",1.0);
                    }
                }
                else
                {
                    LayoutUtils.setFeatureValue(lineSpan,"columnLayoutChange",1.0);
                }
                lastLineColumn = currentLineColumn;
            }

            //the following are additional features to implement:

            //- if it is near the top of the page
            Boolean isContentNearTheTop = LayoutUtils.isNearTheTop(lineInfos[i], pagesData.get(lineInfos[i].page), 100);

            if(isContentNearTheTop)
            {
                LayoutUtils.setFeatureValue(lineSpan,"nearThe100PxOfTop", 1.0);
            }

            isContentNearTheTop = LayoutUtils.isNearTheTop(lineInfos[i], pagesData.get(lineInfos[i].page), 150);

            if(isContentNearTheTop)
            {
                LayoutUtils.setFeatureValue(lineSpan,"nearThe150PxOfTop", 1.0);
            }

            //- the character width
            int pxlsXCharacter = LayoutUtils.getPixelsPerCharacter(lineInfos, i);

            int mostCommonPxlsXCharacter = pixelsPerCharacter.get(0).getKey();
            if(pxlsXCharacter > mostCommonPxlsXCharacter)
            {
                LayoutUtils.setFeatureValue(lineSpan,"pixelsPerCharacter1pxGreater", 1.0);
            }
            if(pxlsXCharacter > mostCommonPxlsXCharacter+1)
            {
                LayoutUtils.setFeatureValue(lineSpan,"pixelsPerCharacter2pxGreater", 1.0);
            }
            if(pxlsXCharacter > mostCommonPxlsXCharacter+2)
            {
                LayoutUtils.setFeatureValue(lineSpan,"pixelsPerCharacter3pxGreater", 1.0);
            }
            if(pxlsXCharacter > mostCommonPxlsXCharacter+3)
            {
                LayoutUtils.setFeatureValue(lineSpan,"pixelsPerCharacter4pxGreater", 1.0);
            }
            if(pxlsXCharacter > mostCommonPxlsXCharacter+4)
            {
                LayoutUtils.setFeatureValue(lineSpan,"pixelsPerCharacter5pxGreater", 1.0);
            }

            if(pxlsXCharacter == -1)
            {
                LayoutUtils.setFeatureValue(lineSpan,"pixelsPerCharacterUndefined", 1.0);
            }
            //- vertical distance outliers
            Integer mostCommonVertDistance = verticalDistance.get(0).getKey();
            Integer currentVertDistance = LayoutUtils.getCurrentVerticalDistance(lineInfos, i);

            if(currentVertDistance > mostCommonVertDistance){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance1pxGreater", 1.0);
            }

            if(currentVertDistance > mostCommonVertDistance+1){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance2pxGreater", 1.0);
            }

            if(currentVertDistance > mostCommonVertDistance+3){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance4pxGreater", 1.0);
            }

            if(currentVertDistance > mostCommonVertDistance+5){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance6pxGreater", 1.0);
            }

            if(currentVertDistance > mostCommonVertDistance+7){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance8pxGreater", 1.0);
            }

            if(currentVertDistance > mostCommonVertDistance+9){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance10pxGreater", 1.0);
            }
            if(currentVertDistance > mostCommonVertDistance+11){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance12pxGreater", 1.0);
            }

            if(currentVertDistance > mostCommonVertDistance+99){
                LayoutUtils.setFeatureValue(lineSpan,"verticalDistance100pxGreater", 1.0);
            }

            //-height of the line font
            Integer mostCommonLineHeight = lineHeight.get(0).getKey();
            Integer currentLineHeight = LayoutUtils.getCurrentLineHeight(lineInfos, i);

            if(currentLineHeight > mostCommonLineHeight){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight1pxGreater", 1.0);
            }

            if(currentLineHeight > mostCommonLineHeight+1){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight2pxGreater", 1.0);
            }
            if(currentLineHeight > mostCommonLineHeight + 3){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight4pxGreater", 1.0);
            }
            if(currentLineHeight > mostCommonLineHeight + 5){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight6pxGreater", 1.0);
            }
            if(currentLineHeight > mostCommonLineHeight + 7){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight8pxGreater", 1.0);
            }
            if(currentLineHeight > mostCommonLineHeight + 9){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight10pxGreater", 1.0);
            }
            if(currentLineHeight > mostCommonLineHeight + 29){
                LayoutUtils.setFeatureValue(lineSpan,"lineHeight30pxGreater", 1.0);
            }

            //- centered or not
            int leftMarginColumn = currentLineColumn!=null?currentLineColumn.getLeftX():sloppyColumn!=null?sloppyColumn.getLeftX():-1;
            int rightMarginColumn = currentLineColumn!=null?currentLineColumn.getRightX():sloppyColumn!=null?sloppyColumn.getRightX():-1;

            if(LayoutUtils.isCentered(lineInfos[i],leftMarginColumn,rightMarginColumn,3))
            {
                LayoutUtils.setFeatureValue(lineSpan,"centeredLine",1.0);
            }

            //- if the left margin is the same as column, but the right margin is to the left.
            if(LayoutUtils.isRightMarginToTheLeft(lineInfos[i], currentLineColumn!=null?currentLineColumn:sloppyColumn, 10))
            {
                LayoutUtils.setFeatureValue(lineSpan, "rightMarginToTheLeft", 1.0);
            }

            //- if the left margin is to the left when the right margin is the same as column.
            if(LayoutUtils.isLeftMarginTabbed(lineInfos[i], currentLineColumn!=null?currentLineColumn:sloppyColumn, 5))
            {
                LayoutUtils.setFeatureValue(lineSpan, "tabbedLeftMargin", 1.0);
            }



        }

        System.out.print("sorted vertical distances");

    }

    private static int updateLineSpanCounter(NewHtmlTokenization data, int currentCounter)
    {
        while(currentCounter < data.getLineSpans().size() &&  LayoutUtils.isPropertySet((Span)data.getLineSpans().get(currentCounter),"isHeaderFooterLine"))
        {
            currentCounter++;
        }
        return currentCounter;
    }

    private static void computeLexiconFeatures(/*LineInfo[] lineInfos,*/ NewHtmlTokenization data,  EnglishDictionary dictionary) {

        //R(?i:eferences)
        // high correlation with non-bibliographic content
        String[] tableWords = {"^T[\\s]{0,5}(?i:a[\\s]{0,5}b[\\s]{0,5}l[\\s]{0,5}e).*"};
        String[] figureWords = {"^F(?i:igure).*", "^F(?:ig\\.).*"};

        //TODO:add more special characters in allCaps if it is necessary
        String allCaps = "[#\\[\\]\\(\\);:\\.,'\"\\*A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ1-9]+";
        String initCap = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ].*";
        String finalDot = "((.*)\\.)$";
        String firstLevelSection = "^((\\s)*([\\d]+)([\\.]{0,1})([\\s]+)[A-Z0-9].*)";
        String secondLevelSection = "^((\\s)*([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+)[A-Z0-9].*)";
        String thirdLevelSection = "^((\\s)*([\\d]+)(\\.)([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+)[A-Z0-9].*)";


        //firs scan/loop to gather basic lexical statistics in the document
        for(int i =0; i<data.getLineSpans().size(); i++ )
        {

//            lineSpanCount = updateLineSpanCounter(data, lineSpanCount + 1);

            List<String> lexiconFeatures = new ArrayList<String>();
            Span ls = (Span)data.getLineSpans().get(i);

            String currentLineText = ls.getText().trim();
            String squishedLineText = currentLineText.replaceAll("\\s", "");

            for (int j = 0; j < tableWords.length; j++) {
                if (currentLineText.matches(tableWords[j])) {
                    LayoutUtils.setFeatureValue(ls, "startsTableWord", 1.0);
                    break;
                }
            }

            for (int j = 0; j < figureWords.length; j++) {
                if (currentLineText.matches(figureWords[j])) {
                    LayoutUtils.setFeatureValue(ls, "startsFigureWord", 1.0);
                    break;
                }
            }

            if(squishedLineText.matches(allCaps))
            {
                LayoutUtils.setFeatureValue(ls, "allCaps", 1.0);
            }
            if(currentLineText.matches(initCap))
            {
                LayoutUtils.setFeatureValue(ls, "startsCap", 1.0);
            }
            if(currentLineText.matches(finalDot))
            {
                LayoutUtils.setFeatureValue(ls, "endsInDot", 1.0);
            }

            if(isUpFlagCount(currentLineText,ptrnLonelyLetters,0.5))
            {
                LayoutUtils.setFeatureValue(ls, "manyLonelyLetters", 1.0);
            }

            if(isUpFlagCount(currentLineText,ptrnLonelyNumbers,0.5))
            {
                LayoutUtils.setFeatureValue(ls, "manyLonelyNumbers", 1.0);
            }

            if(currentLineText.matches(firstLevelSection))
            {
                LayoutUtils.setFeatureValue(ls, "firstLevelSectionPtrn", 1.0);
            }

            if(currentLineText.matches(secondLevelSection))
            {
                LayoutUtils.setFeatureValue(ls, "secondLevelSectionPtrn", 1.0);
            }

            if(currentLineText.matches(thirdLevelSection))
            {
                LayoutUtils.setFeatureValue(ls, "thirdLevelSectionPtrn", 1.0);
            }

            //- the number of words in dictionary per line
            LayoutUtils.adjustWordsInDictionaryPerLine(currentLineText, wordsInDictionaryPerLine, dictionary);

        }
        Collections.sort(wordsInDictionaryPerLine);

        //second scan to compute the properties using statistics gathered in previous loop
        for(int i =0; i<data.getLineSpans().size(); i++ ) {
            Span ls = (Span)data.getLineSpans().get(i);

            String currentLineText = ls.getText().trim();

            int mostCommonNumberOfDictWords = wordsInDictionaryPerLine.get(0).getKey();
            int currLineDictWords = LayoutUtils.getWordsInDictionary(currentLineText, dictionary, true);
            if(currLineDictWords==0)
            {
                LayoutUtils.setFeatureValue(ls, "noWordsFromDictionary", 1.0);
            }
            if(currLineDictWords==1)
            {
                LayoutUtils.setFeatureValue(ls, "oneWordFromDictionary", 1.0);
            }

            if(mostCommonNumberOfDictWords>currLineDictWords)
            {
                LayoutUtils.setFeatureValue(ls, "1wordFromDictLess", 1.0);
            }

            if(mostCommonNumberOfDictWords>currLineDictWords+1)
            {
                LayoutUtils.setFeatureValue(ls, "2wordFromDictLess", 1.0);
            }

            if(mostCommonNumberOfDictWords>currLineDictWords+2)
            {
                LayoutUtils.setFeatureValue(ls, "3wordFromDictLess", 1.0);
            }

            if(mostCommonNumberOfDictWords>currLineDictWords+3)
            {
                LayoutUtils.setFeatureValue(ls, "4wordFromDictLess", 1.0);
            }

            if(mostCommonNumberOfDictWords>currLineDictWords+4)
            {
                LayoutUtils.setFeatureValue(ls, "5wordFromDictLess", 1.0);
            }
        }
    }
    private static boolean isUpFlagCount(String text, Pattern pattern, Double ratioActivation)
    {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while(matcher.find())
        {
            count++;
        }
        if(Double.valueOf(count)/Double.valueOf(text.split(" ").length) > ratioActivation)
        {
            return true;
        }
        return false;
    }

}
