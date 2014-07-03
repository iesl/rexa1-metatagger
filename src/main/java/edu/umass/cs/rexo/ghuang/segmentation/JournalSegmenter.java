package edu.umass.cs.rexo.ghuang.segmentation;

import org.apache.commons.collections.map.AbstractHashedMap;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by klimzaporojets on 6/30/14.
 *
 */
public abstract class JournalSegmenter {

/*
	static {
		INTRODUCTION_PATTERN = Pattern.compile("^[\\s\\.\\d]*I(?i:ntroduction)");
		ABSTRACT_PATTERN = Pattern.compile("^[\\s]*A(?i:bstract)");
		BIBLIOGRAPHY_PATTERN = Pattern
				.compile("^[#iIvVxX\\d\\.\\s]{0,5}(R(?i:eferences)|B(?i:ibliography)|R(?i:eferences and Notes))\\s*$");
	}
* */

    static HashMap<Pattern, JournalSegmenter> journalSegmenters = new HashMap<Pattern,JournalSegmenter>();
    static
    {
//        journalSegmenters.put(Pattern.compile("©.*The Electrochemical Society"), new ECSJournalSegmenter());
        journalSegmenters.put(Pattern.compile("The Electrochemical Society"), new ECSJournalSegmenter());
    }

    static JournalSegmenter getSegmenter(List lineSpans)
    {
//        System.out.println(Arrays.toString(lineSpans.toArray()));

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
