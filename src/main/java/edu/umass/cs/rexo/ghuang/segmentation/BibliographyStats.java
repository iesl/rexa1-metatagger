package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.types.Sequence;
import org.rexo.span.CompositeSpan;

import java.util.*;

import edu.umass.cs.mallet.base.extract.Span;
/**
 * Created by klimzaporojets on 7/15/14.
 * Statistics for bibliography
 */
public class BibliographyStats {


    private int firstRightQty;
    private int firstLeftQty;


    private LinkedList references;


    private boolean suspiciousReferences;

    public boolean hasSuspiciousReferences() {
        return suspiciousReferences;
    }

    public void setSuspiciousReferences(boolean suspiciousReferences) {
        this.suspiciousReferences = suspiciousReferences;
    }

//    private int referenceQty;
//
//    private double avgDistanceRightLeft;
//    private double sDevRightLeft;

    public enum AlignType{
        FIRST_RIGHT,
        FIRST_LEFT,
        EQUAL,
        NONE
    }

    public HashMap<AlignType, Integer> getFirstLineAlignStats() {
        return firstLineAlignStats;
    }

    //private LinkedList AlignType.values().length;
    HashMap<AlignType,Integer> firstLineAlignStats = new HashMap<AlignType,Integer>();

    {
        firstLineAlignStats.put(AlignType.EQUAL, 0);
        firstLineAlignStats.put(AlignType.FIRST_LEFT, 0);
        firstLineAlignStats.put(AlignType.FIRST_RIGHT, 0);
    }

    public static BibliographyStats getStats(LinkedList references,List lines, Sequence predictedLabels)
    {
        BibliographyStats biblioStats = new BibliographyStats();
        biblioStats.setReferences(references);

        for(LinkedList reference : (LinkedList<LinkedList>)references)
        {
            if(reference.get(0) instanceof CompositeSpan)
            {
                if(reference.size()>1 && reference.get(1) instanceof CompositeSpan)
                {
                    long firstX = Math.round(((CompositeSpan) reference.get(0)).getNumericProperty("llx"));
                    long firstXR = Math.round(((CompositeSpan) reference.get(0)).getNumericProperty("urx"));
                    long secondX = Math.round(((CompositeSpan) reference.get(1)).getNumericProperty("llx"));

                    //gets the page number
                    long firstPageNumber = Math.round(((CompositeSpan) reference.get(1)).getNumericProperty("pageNum"));
                    long secondPageNumber = Math.round(((CompositeSpan) reference.get(1)).getNumericProperty("pageNum"));

                    //also checks if in the same column, as well as in the same page
                    if(firstX > secondX && firstPageNumber == secondPageNumber &&
                            secondX > (firstX - (firstX-firstXR)/2))
                    {
                        //biblioStats.increaseFirstRightQty();
                        biblioStats.increaseFirstMargin(AlignType.FIRST_RIGHT);
                    }
                    if(firstX < secondX && firstPageNumber == secondPageNumber && secondX<firstXR)
                    {
                        //biblioStats.increaseFirstLeftQty();
                        biblioStats.increaseFirstMargin(AlignType.FIRST_LEFT);
                    }
                }
            }
        }


        //rule1: mixed margins regarding the first two lines of a particular reference
        List<Integer> vals = new ArrayList((Collection)biblioStats.getFirstLineAlignStats().values());
        Collections.sort(vals);
        biblioStats.setSuspiciousReferences(vals.get(vals.size() - 1) > 0 && vals.get(vals.size() - 2) > 0);


        //rule2: big chunk of text ignored between references
        if(!biblioStats.hasSuspiciousReferences())
        {
            biblioStats.setSuspiciousReferences(isBigChunkIgnored(lines, predictedLabels));
        }

        //rule3: different llx after the first line of reference (should be aligned)
        if(!biblioStats.hasSuspiciousReferences())
        {
            biblioStats.setSuspiciousReferences(differentLlxInsideReference(references));
        }
        System.out.println("Is the reference suspicious?: " + biblioStats.hasSuspiciousReferences());
        return biblioStats;
    }


    private static boolean differentLlxInsideReference(LinkedList<List>references)
    {
        for(List reference:references) {
            if (reference.size() > 2) {
                int lastBreakPoint = 0;
                LinkedList ref = new LinkedList();
                for (int i = 1; i < reference.size() - 1; i++) {
                    AlignType alignType = getReferenceType(reference.subList(i, reference.size()));
                    if (alignType == AlignType.FIRST_RIGHT || alignType == AlignType.FIRST_LEFT) {
//                        references.add(reference.subList(lastBreakPoint, i + 1));
//                        lastBreakPoint = i + 1;
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private static boolean isBigChunkIgnored(List lines, Sequence predictedLabels)
    {
        //just the simplest version for now, only counting "junk" and "post" without getting into the formats details
        //of each of the "lines"
        int adjacentCounts = 0;
        for(int i=0; i<predictedLabels.size(); i++)
        {
            String tag = predictedLabels.get(i).toString();
            if(tag.equals("junk") || tag.equals("post"))
            {
                adjacentCounts++;
            }
            else
            {
                adjacentCounts=0;
            }
            if(adjacentCounts>5)
            {
                return true;
            }
        }
        return true;
    }
    private static double getHorizontal(String type, List<Span> listOfSpan)
    {
        double res = type.equals("llx")?99999:0;
        for(Object sp:listOfSpan)
        {
            if(sp instanceof StringSpan) {
                double val = ((StringSpan)sp).getNumericProperty(type);
                if(type.equals("llx"))
                {
                    if(val<res)
                    {
                        res = val;
                    }
                }
                else if(type.equals("urx"))
                {
                    if(val>res)
                    {
                        res = val;
                    }
                }
            }
        }
        return res;
    }
    //based on the first two lines
    private static AlignType getReferenceType(List reference)
    {
        if(reference.size()>1 && reference.get(0) instanceof CompositeSpan && reference.get(1) instanceof CompositeSpan)
        {
            long firstX = Math.round(getHorizontal("llx", ((CompositeSpan) reference.get(0)).getSpans())); //Math.round(((CompositeSpan) reference.get(0)).getNumericProperty("llx"));
            long firstXR = Math.round(getHorizontal("urx",((CompositeSpan) reference.get(0)).getSpans())); //Math.round(((CompositeSpan) reference.get(0)).getNumericProperty("urx"));
            long secondX = Math.round(getHorizontal("llx",((CompositeSpan) reference.get(1)).getSpans())); //Math.round(((CompositeSpan) reference.get(1)).getNumericProperty("llx"));

            //gets the page number
            long firstPageNumber = Math.round(((CompositeSpan) reference.get(1)).getNumericProperty("pageNum"));
            long secondPageNumber = Math.round(((CompositeSpan) reference.get(1)).getNumericProperty("pageNum"));

            //also checks if in the same column, as well as in the same page
            if(firstX > secondX && firstPageNumber == secondPageNumber &&
                    secondX > (firstX - (firstXR-firstX)/2))
            {
                return AlignType.FIRST_RIGHT;
            }
            if(firstX < secondX && firstPageNumber == secondPageNumber && secondX<firstXR)
            {
                return AlignType.FIRST_LEFT;
            }
            //todo: consider also different columns
            if(firstX == secondX)
            {
                return AlignType.EQUAL;
            }
        }
        return AlignType.NONE ;
    }

    //returns true if the reference division is suspicious
//    public boolean isSuspiciousReferenceDivision()
//    {
//        return true;
//    }

    //code to check the references generated by CRF and create a modified references LinkedList
    public LinkedList<List> getRevisedReferences()
    {

        LinkedList<List> newReferences = new LinkedList<List>();
        LinkedList<List> newReferences2 = new LinkedList<List>();
        for(LinkedList reference: (LinkedList<LinkedList>)references)
        {
            newReferences.addAll(applySecondRule(reference));
        }
        for(List reference2:newReferences)
        {
            newReferences2.addAll(applyFirstRule(reference2));
        }
        return newReferences2;
    }

    //code that returns fixed references without re-working references created by CRF (as in getRevisedReferences() method), it does it from scratch
    //
    public LinkedList<List> getRevisedReferencesWithoutCRF(LinkedList references,List lines, Sequence predictedLabels) {
        return null;
    }

    private LinkedList<List> applySecondRule(List reference)
    {
        LinkedList<List> references = new LinkedList<List>();
        if(reference.size()>2) {
            int lastBreakPoint = 0;
            LinkedList ref = new LinkedList();
            for (int i = 1; i<reference.size()-1; i++) {
                AlignType alignType = getReferenceType(reference.subList(i, reference.size()));
                AlignType realAlignType = getAlignType();
                if ((alignType == AlignType.FIRST_RIGHT && realAlignType == AlignType.FIRST_LEFT) ||
                        (alignType == AlignType.FIRST_LEFT && realAlignType == AlignType.FIRST_RIGHT)) {
//                    if(lastBreakPoint<i) {
                        references.add(reference.subList(lastBreakPoint, i + 1));
                        lastBreakPoint = i + 1;
//                    }
                }
            }
            references.add(reference.subList(lastBreakPoint,reference.size()));
        }
        else
        {
            references.add(reference.subList(0,reference.size()));
        }
        return references;
    }



    private LinkedList<List> applyFirstRule(List reference)
    {
        AlignType alignType = getReferenceType(reference);
        if(alignType == AlignType.EQUAL && (getAlignType()==AlignType.FIRST_LEFT ||
                getAlignType()==AlignType.FIRST_RIGHT))
        {
            LinkedList<List> references = new LinkedList<List>();
            LinkedList ref = new LinkedList();
            ref = new LinkedList();
            ref.add(reference.get(0));
            references.add(ref);

            references.addAll(applyFirstRule(reference.subList(1, reference.size())));
            return references;
        }
        //if the alignment of the first element doesn't correspond to the trend, ignore it
        else if((alignType == AlignType.FIRST_LEFT && getAlignType() == AlignType.FIRST_RIGHT) ||
                (alignType == AlignType.FIRST_RIGHT && getAlignType() == AlignType.FIRST_LEFT))
        {
            return new LinkedList<List>();
        }
        //in all other cases just leave it as it is
        else
        {
            LinkedList<List> references = new LinkedList<List>();
            references.add(reference);
            return references;
        }

    }


    public BibliographyStats()
    {
        firstRightQty = 0;
        firstLeftQty = 0;
    }
    public AlignType getAlignType()
    {
        if(firstRightQty>firstLeftQty)
        {
            return AlignType.FIRST_RIGHT;
        }
        else if(firstLeftQty>firstRightQty)
        {
            return AlignType.FIRST_LEFT;
        }
        return AlignType.EQUAL;
    }
    public LinkedList getReferences() {
        return references;
    }

    public void setReferences(LinkedList references) {
        this.references = references;
    }

    public void increaseFirstMargin(AlignType margin)
    {
        firstLineAlignStats.put(margin, firstLineAlignStats.get(margin)+1);
    }
//    public int getFirstRightQty() {
//        return firstRightQty;
//    }
//
//    public void setFirstRightQty(int firstRightQty) {
//        this.firstRightQty = firstRightQty;
//    }
//
//    public void increaseFirstRightQty()
//    {
//        //AlignType.values().length;
//        this.firstRightQty++;
//    }
//
//    public void increaseFirstLeftQty()
//    {
//        this.firstLeftQty++;
//    }
//    public int getFirstLeftQty() {
//        return firstLeftQty;
//    }
//
//    public void setFirstLeftQty(int firstLeftQty) {
//        this.firstLeftQty = firstLeftQty;
//    }


}
