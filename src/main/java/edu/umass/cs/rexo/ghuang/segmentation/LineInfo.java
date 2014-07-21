package edu.umass.cs.rexo.ghuang.segmentation;

import java.util.HashSet;

/**
 * Intermediate representation for both labeled segmentation data and pstotext data.
 * 
 * @author adingle, ghuang
 *
 */
public class LineInfo 
{
	String text;
	int page;
	int llx, lly, urx, ury;
	int font;
	boolean multibox;

	boolean newRef; // unused
	String trueLabel;

	HashSet presentFeatures = new HashSet();  // contains names of binary features that are "on" in this line 
	String[] textTokens;  // tokenized text of this line
	//kzaporojets: here add one with reference stats (ex: averageLineWidth, averageVerticalDistanceLines...)



	public LineInfo() {}



	// Constructed from a line of the form output by 'MakeLabeledData.java'.
	public LineInfo(String line) 
	{
		// Hack -- reverse the string before splitting so we can match the last '@' on
		// the line.  Alternatively we could change the textline format to put list labels
		// first (instead of last)
		line = new StringBuffer(line).reverse().toString();
		String[] parts = line.split("@", 2);
		assert (parts.length == 2);

		parts[0] = new StringBuffer(parts[0]).reverse().toString();
		parts[1] = new StringBuffer(parts[1]).reverse().toString();

		String[] subParts = parts[0].split(",");
		this.page = Integer.parseInt(subParts[0]);
		this.llx = Integer.parseInt(subParts[1]);
		this.lly = Integer.parseInt(subParts[2]);
		this.urx = Integer.parseInt(subParts[3]);
		this.ury = Integer.parseInt(subParts[4]);
		this.font = Integer.parseInt(subParts[5]);
		this.multibox = subParts[6].equals("true");

		// newRef is really an extension of the label so shouldn't be used in features.
		this.newRef = subParts[7].equals("true");
		this.trueLabel = subParts[8];

		this.text = parts[1];
	}
}
