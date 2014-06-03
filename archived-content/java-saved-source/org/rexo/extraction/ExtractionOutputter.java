package org.rexo.extraction;

import java.io.PrintStream;

import edu.umass.cs.mallet.base.extract.DocumentExtraction;
import edu.umass.cs.mallet.base.extract.Extraction;
import edu.umass.cs.mallet.base.types.LabelSequence;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.TokenSequence;

/**
   Outputs field entries from header/reference extraction

   @author Gary Huang <a href="mailto:ghuang@cs.umass.edu">ghuang@cs.umass.edu</a>
*/
public class ExtractionOutputter
{
    public static void extraction2fields (Extraction extraction, PrintStream out)
    {
		for (int i = 0; i < extraction.getNumDocuments (); i++) {
			DocumentExtraction docextr = extraction.getDocumentExtraction (i);
			TokenSequence input = (TokenSequence) docextr.getInput();
			LabelSequence target = docextr.getTarget();
			Sequence predicted = docextr.getPredictedLabels();
			String desc = docextr.getName();

			assert (target.size() == predicted.size());
			assert (input.size() == predicted.size());
			assert (input.size() > 0);

			out.println("\n------------------------");
			out.println("Document " + desc + ":");

			String prevLabel = predicted.get(0).toString();;
			StringBuffer field = new StringBuffer(input.getToken(0).getText());
			for (int j = 1; j < input.size(); j++) {
				String curPredLabel = predicted.get(j).toString();
				if (! curPredLabel.equals(prevLabel)) {
					out.println(prevLabel + ":\t\t" + field.toString());
					prevLabel = curPredLabel;
					field = new StringBuffer(input.getToken(j).getText());
				}
				else {
					field.append(" " + input.getToken(j).getText());
				}
			}

			// Output the last field
			out.println(prevLabel + ":\t\t" + field.toString());
		}
    }
}

