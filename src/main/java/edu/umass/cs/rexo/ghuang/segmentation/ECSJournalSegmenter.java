package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.util.PropertyList;
import org.rexo.span.CompositeSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by klimzaporojets on 6/30/14.
 */
public class ECSJournalSegmenter extends JournalSegmenter{
    List getAbstract(List lineSpan)
    {
        int count = 0;
        Double prevSpan = 0.0;
        Double avgX = 0.0;
        Double sumX = 0.0;
        for(Object obj: lineSpan)
        {
            if(obj instanceof CompositeSpan)
            {
                count++;
                List<StringSpan> pl = ((CompositeSpan)obj).getSpans();
                StringSpan firstSp = (StringSpan)((CompositeSpan)obj).getSpans().get(0);
                StringSpan secondSp = (StringSpan)((CompositeSpan)obj).getSpans().get(((CompositeSpan)obj).getSpans().size()-1);

                Double xLeft = (Double)firstSp.getProperty("llx");
                Double xRight = (Double)firstSp.getProperty("urx");


                if(count > 10)
                {
                    //System.out.println(xLeft);
                    if(/*(prevSpan) > 400.0 && (xRight - xLeft)<280 &&*/ xLeft < avgX - 20 )
                    {
                        return lineSpan.subList(0, count-1);
                    }
                    sumX += xLeft;
                    avgX = sumX/(count - 10);
                }
                prevSpan = xRight - xLeft;

            }
        }
        return lineSpan.subList(0, 0);
    }
    List getBody(List lineSpan)
    {
        return null;
    }
    List getReferences(List lineSpan)
    {
        return null;
    }
}
