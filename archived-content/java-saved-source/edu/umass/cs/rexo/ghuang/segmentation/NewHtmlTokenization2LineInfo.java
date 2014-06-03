package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;
import java.util.ArrayList;

import org.rexo.referencetagging.NewHtmlTokenization;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.rexo.ghuang.segmentation.LineInfo;

/**
 * Convert a NewHtmlTokenization in the data field of an Instance to LineInfo[] 
 * 
 * @author ghuang
 *
 */
public class NewHtmlTokenization2LineInfo extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;

	
	public Instance pipe(Instance carrier)
	{
		NewHtmlTokenization htmlTokenization = (NewHtmlTokenization) carrier.getData();
		ArrayList lineInfos = new ArrayList();
		int prevLineNum = -1;
		LineInfo lineInfo = null;
		StringBuffer lineText = new StringBuffer();
		
		for (int ti = 0; ti < htmlTokenization.size(); ti++) {
			Token token = htmlTokenization.getToken(ti);
			int tokLineNum = (int) token.getNumericProperty("lineNum");

			if (tokLineNum == 0) { // I don't know why this happens
				continue;
			}
			else if (tokLineNum != prevLineNum) {
				prevLineNum = tokLineNum;

				if (lineText.length() > 0) {
					lineInfo.text = lineText.toString();
					lineInfos.add(lineInfo);
				}

				lineInfo = new LineInfo();
				lineText = new StringBuffer(token.getText() + " ");
				lineInfo.page = (int) token.getNumericProperty("pageNum");
				lineInfo.llx = (int) token.getNumericProperty("llx");
				lineInfo.lly = (int) token.getNumericProperty("lly");
				lineInfo.urx = (int) token.getNumericProperty("urx");
				lineInfo.ury = (int) token.getNumericProperty("ury");
				lineInfo.font = (int) token.getNumericProperty("fontnumber");
			}
			else {
				lineText.append(token.getText() + " ");
				
				if (token.getNumericProperty("firstInTextBox") > 0) {
					lineInfo.multibox = true;
					lineInfo.llx = (int) Math.min(lineInfo.llx, token.getNumericProperty("llx"));
					lineInfo.lly = (int) Math.min(lineInfo.lly, token.getNumericProperty("lly"));
					lineInfo.urx = (int) Math.max(lineInfo.urx, token.getNumericProperty("urx"));
					lineInfo.ury = (int) Math.max(lineInfo.ury, token.getNumericProperty("ury"));
				}
			}

		}

		assert (lineInfo != null);
		
		if (lineText.toString() != null) {
			lineInfo.text = lineText.toString();
			lineInfos.add(lineInfo);
		}

		LineInfo[] newData = new LineInfo[lineInfos.size()];
		
		for (int i = 0; i < lineInfos.size(); i++)
			newData[i] = (LineInfo) lineInfos.get(i);

		carrier.setData(newData);
		
		return carrier;
	}

}
