package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.types.Sequence;
import org.rexo.span.CompositeSpan;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by klimzaporojets on 7/15/14.
 * Statistics for bibliography
 */
public class BibliographyStats {

    public enum AlignType{
        FIRST_RIGHT,
        FIRST_LEFT,
        EQUAL
    }

    public static BibliographyStats getStats(LinkedList references)
    {
        BibliographyStats biblioStats = new BibliographyStats();

        for(LinkedList reference : (LinkedList<LinkedList>)references)
        {
            if(reference.get(0) instanceof CompositeSpan)
            {
//                System.out.println("First x: " + ((CompositeSpan) reference.get(0)).getProperty("llx"));
                if(reference.size()>1 && reference.get(1) instanceof CompositeSpan)
                {
//                    System.out.println("Second x: " + ((CompositeSpan) reference.get(1)).getProperty("llx"));
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
                        biblioStats.increaseFirstRightQty();
                    }
                    if(firstX < secondX && firstPageNumber == secondPageNumber && secondX<firstXR)
                    {
                        biblioStats.increaseFirstLeftQty();
                    }
                }
            }
        }

        return biblioStats;
    }
    private int firstRightQty;
    private int firstLeftQty;

    private int referenceQty;

    private double avgDistanceRightLeft;
    private double sDevRightLeft;

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
    public int getFirstRightQty() {
        return firstRightQty;
    }

    public void setFirstRightQty(int firstRightQty) {
        this.firstRightQty = firstRightQty;
    }

    public void increaseFirstRightQty()
    {
        this.firstRightQty++;
    }

    public void increaseFirstLeftQty()
    {
        this.firstLeftQty++;
    }
    public int getFirstLeftQty() {
        return firstLeftQty;
    }

    public void setFirstLeftQty(int firstLeftQty) {
        this.firstLeftQty = firstLeftQty;
    }


}
