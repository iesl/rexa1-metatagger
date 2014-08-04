package edu.umass.cs.rexo.ghuang.segmentation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.umass.cs.mallet.base.pipe.Pipe;
import org.apache.log4j.Logger;
import org.rexo.extraction.NewHtmlTokenization;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Sequence;

/**
 * Use a trained CRF to segment references.
 * 
 * @author ghuang
 * 
 */
public class CRFBibliographySegmentor
{
	private static Logger log = Logger.getLogger(CRFBibliographySegmentor.class);
	
	private CRF4 m_crf;


	public CRFBibliographySegmentor(CRF4 crf)
	{
		m_crf = crf;
	}

	public Pipe getInputPipe()
    {
        return m_crf.getInputPipe();
    }
	public ReferenceData segmentReferences(NewHtmlTokenization htmlTokenization)
	{
		Instance inst = new Instance(htmlTokenization, null, null, null, m_crf.getInputPipe());
        //todo: kzaporojets: here another instance with LineInfo2TokenSequence pipe, adding also data such as
        //average line width
		Sequence predictedLabels = m_crf.transduce ((Sequence) inst.getData());


		ReferenceData ret = new ReferenceData();
		ArrayList lineSpans = new ArrayList();

		lineSpans.addAll(htmlTokenization.getLineSpans());

//		System.out.println("zzzzzzzz " + lineSpans);
//		System.out.println("zzzzzzzz " + lineSpans.size() + " " + predictedLabels.size());

		
		// lineSpans may contain extra StringSpans indicating page breaks or repeating header/footer lines
		assert (lineSpans.size() >= predictedLabels.size());
		
		String warning = checkAndSegmentReferences(lineSpans, predictedLabels, ret);
		
		if (! warning.equals("")) {
			log.error(warning);
		}
		
		return ret;
}

	
	// NOTE: argument "result" is modified
	private static String checkAndSegmentReferences(List lines, Sequence predictedLabels, ReferenceData result)
	{


		String warning = "";
		boolean seenPrologue = false;
		boolean seenRef = false;
		boolean seenEpilogue = false;
		boolean seenJunk = false;
		LinkedList reference = new LinkedList();
		int lineIdx = 0;
		int labIdx = 0;



		while (labIdx < predictedLabels.size() && lineIdx < lines.size()) {
			String tag = predictedLabels.get(labIdx).toString();

			if (tag.equals("biblioPrologue"))
				seenPrologue = true;
			else if (tag.equals("post"))
				seenEpilogue = true;
			else if (tag.startsWith("biblio-"))
				seenRef = true;
			else if (tag.equals("junk"))
				seenJunk = true;
			
			if (seenEpilogue && ! seenPrologue )
				warning += "epilogue section before prologue section; ";
			if (seenEpilogue && ! seenRef)
				warning += "epilogue section before references; ";
			if (! seenRef && seenJunk)
				warning += "junk line before the first reference; ";

			Object lineTok = lines.get(lineIdx);
			
			if ((lineTok instanceof StringSpan) && ((StringSpan) lineTok).getNumericProperty("isHeaderFooterLine") > 0)
			{
				// repeating header/footer line or page number, so ignore
				lineIdx++;
				continue;
			}
			else if (tag.equals("biblioPrologue")) {
				result.prologueList.add(lines.get(lineIdx));
			}
			else if (tag.equals("post")) {
				result.epilogueList.add(lines.get(lineIdx));
			}
			else if (tag.equals("biblio-B")) {
				result.numReferences++;

				if (reference.size() > 0)
					result.referenceLineList.add(reference);

				reference = new LinkedList();
				reference.add(lines.get(lineIdx));
			}
			else if (tag.equals("biblio-I")) {
				if (reference.size() == 0)
					warning += "biblio-I not after biblio-B, line ignored; ";
				else {
					reference.add(lines.get(lineIdx));
				}
			}

			labIdx++;
			lineIdx++;
		}

		if (reference.size() > 0) {
            result.referenceLineList.add(reference);
            //kzaporojets: building additional stats to detect references
            BibliographyStats stats = BibliographyStats.getStats(result.referenceLineList, lines, predictedLabels);


            //result.referenceLineList = stats.getRevisedReferences();
            if(stats.hasSuspiciousReferences())
            {
//                result.referenceLineList = stats.getRevisedReferencesWithoutCRF();
            }
        }


		return warning;
	}
	

	public static class ReferenceData 
	{
		LinkedList prologueList = new LinkedList();
		LinkedList referenceLineList = new LinkedList();
		LinkedList epilogueList = new LinkedList();
		int numReferences = 0;
	}

}
