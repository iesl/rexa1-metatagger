package edu.umass.cs.rexo.ghuang.segmentation;

import org.apache.commons.collections.map.AbstractHashedMap;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by klimzaporojets on 6/30/14.
 *
 */
public abstract class JournalSegmenter {


    static HashMap<Pattern, JournalSegmenter> journalSegmenters = new HashMap<Pattern,JournalSegmenter>();
    static
    {
        journalSegmenters.put(Pattern.compile("The Electrochemical Society"), new ECSJournalSegmenter());
    }

    static JournalSegmenter getSegmenter(List lineSpans)
    {
        for (Map.Entry<Pattern, JournalSegmenter> curr :journalSegmenters.entrySet() )
        {
            if(curr.getKey().matcher(Arrays.toString(lineSpans.toArray())).find()){
                return curr.getValue();
            }

        }
        return null;
     }
    abstract List getAbstract(List lineSpans);
    abstract List getBody(List lineSpans);
    abstract List getReferences(List lineSpans);
}
