package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.rexo.ghuang.segmentation.LineInfo;

/**
 * Given an instance with String[] in its data field from the "list of lines"
 * format, output an instance with a LineInfo[] in its data field. 
 * 
 * This pipe excludes a line if it is a header/footer that is repeated 
 * throughout the document.  This is done via methods analog to 
 * NewHtmlTokenization.initHeaderFooterLineCounts and 
 * NewHtmlTokenization.isPaginationText
 * 
 * @author ghuang
 *
 */
public class LabeledSegmentationData2LineInfo extends Pipe implements Serializable
{
	private static final Pattern DIGIT_OR_SPACE = Pattern.compile( "\\d+| " );
	private static final long serialVersionUID = 1L;


	public Instance pipe(Instance carrier)
	{
		String[] data = (String[]) carrier.getData();
		LineInfo[] lineInfo =  new LineInfo[data.length];

		for (int i = 0; i < data.length; i++) 
			lineInfo[i] = new LineInfo(data[i]);
			
		Object[] oa = initHeaderFooterLineCounts(lineInfo);
		HashMap headerFooterLineCounts = (HashMap) oa[0];
		boolean[] isTopOrBottomLine = (boolean[]) oa[1];
		ArrayList goodLines = getGoodLineInfos(lineInfo, headerFooterLineCounts, isTopOrBottomLine);
		LineInfo[] goodLineInfos = new LineInfo[goodLines.size()];
	
		for (int i = 0; i < goodLines.size(); i++) 
			goodLineInfos[i] = (LineInfo) goodLines.get(i);
		
//		System.err.println("old count=" + lineInfo.length + ", new count=" + goodLines.size());
		
		carrier.setData(lineInfo);

		return carrier;
	}
		
	
	private static ArrayList getGoodLineInfos(LineInfo[] lineInfos, HashMap headerFooterLineCounts, boolean[] isTopOrBottomLine)
	{
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < lineInfos.length; i++) {
			String normalizedLine = headerFooterNormalize(lineInfos[i], isTopOrBottomLine[i]);

			// To try to avoid false positives such as page ranges in
			// bibliographies, require that "middle" header/footer lines to
			// be skipped contain some alphabetical characters.
			// FIXME: is this too strict?
//			if (! isTopOrBottomLine[i] && ! normalizedLine.matches( ".*[a-zA-Z].*" )) {
//				System.err.println("  " + isTopOrBottomLine[i] + " " + lineInfos[i].text);
//				ret.add(lineInfos[i]);
//				continue;
//			}

			int count = ((Integer)headerFooterLineCounts.get( normalizedLine )).intValue();

			if (count < 3) {
				ret.add(lineInfos[i]);
//				System.err.println("  " + isTopOrBottomLine[i] + " " + lineInfos[i].text);
			}
//			else 
//				System.err.println("X " + isTopOrBottomLine[i] + " " + lineInfos[i].text);
		}
			
//		System.err.println("\n\n");
		
		return ret;
	}
	
		
	private static Object[] initHeaderFooterLineCounts(LineInfo[] lineInfos)
	{
		boolean[] isTopOrBottom = new boolean[lineInfos.length];
		int prevPage = 0;
		
		for (int i = 0; i < lineInfos.length; i++) {
			if (lineInfos[i].page != prevPage) {
				isTopOrBottom[i] = true;
				prevPage = lineInfos[i].page;
				
				if (i > 0)
					isTopOrBottom[i-1] = true;
			}
		}

		HashMap counts = new HashMap();

		for (int i = 0; i < lineInfos.length; i++) {
			String normalizedLine = headerFooterNormalize(lineInfos[i], isTopOrBottom[i]);
			int count = 1;
			
			if (counts.containsKey( normalizedLine )) {
				count += ((Integer)counts.get( normalizedLine )).intValue();
			}
			counts.put( normalizedLine, new Integer( count ) );
		}
		
		Object[] ret = new Object[2];
		ret[0] = counts;
		ret[1] = isTopOrBottom;
		
		return ret;
	}
	
	
	private static String headerFooterNormalize(LineInfo lineInfo, boolean isTopOrBottomLine)
	{
		// strip all numbers and white space from the string
		// the following is (a more efficient) equivalent to doing string.replaceAll( "\\d+| ", "" );
		String ret = DIGIT_OR_SPACE.matcher( lineInfo.text ).replaceAll( "" );

		// The line needs to have the same position and font to match.
		ret = ret + " " + lineInfo.llx + "," + lineInfo.lly + "," + lineInfo.font;

		return ret;
	}

}
