package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.types.*;
import org.apache.log4j.Logger;
import org.rexo.extraction.NewHtmlTokenization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Use rules to segment references.
 * 
 * @author kzaporojets: based on CRF segmentor (CRFBibliographySegmentor)
 * 
 */
public class RulesBibliographySegmentor
{
	private static Logger log = Logger.getLogger(RulesBibliographySegmentor.class);

//    ArraySequence arraySequence = new ArraySequence();

//	private CRF4 m_crf;


//	public RulesBibliographySegmentor(CRF4 crf)
//	{
//		m_crf = crf;
//	}

    public RulesBibliographySegmentor(){}

    private Pipe transformFromCrfToRulesPipe(Pipe crfInputPipe)
    {
//       edu.umass.cs.rexo.ghuang.segmentation.NewHtmlTokenization2LineInfo@5ef80a07

        List newPipeList = new ArrayList();


        for(Object pipe: ((edu.umass.cs.mallet.base.pipe.SerialPipes)crfInputPipe).getPipes())
        {
            if(pipe instanceof LineInfo2TokenSequence)
            {
                newPipeList.add(new LineInfo2TokenSequenceV2());
            }
            else
            {
                //((Pipe)pipe).setParent(null);
                newPipeList.add(pipe);
            }
        }


        List pipes = new ArrayList();
        pipes.add(new NewHtmlTokenization2LineInfo());
        Pipe pli = new LineInfo2TokenSequenceV2();
        pli.setTargetProcessing(false);
        pipes.add(pli);
        SerialPipes serialPipes = new SerialPipes(pipes);
        //SerialPipes serialPipes = new SerialPipes(newPipeList);
//        //serialPipes.
        return serialPipes;


    }

    //here go the rules to identify the references
    private Sequence transduce(Sequence data)
    {
        Sequence transducedData = new TokenSequence();
        Iterator iter = ((TokenSequence)data).iterator();
        String label;
        for(int i=0; i<((TokenSequence)data).size(); i++)
        {
            Token tkn = (Token)(((TokenSequence)data).get(i));
            if (tkn.getText().toUpperCase().trim().equals("REFERENCES"))
            {
                label = "biblioPrologue";
            }
            else if(tkn.getFeatures().hasProperty("possibleInit")
            //tkn.getFeatures().hasProperty("samePatternAsInFirst")
            /*tkn.getFeatures().hasProperty("beginNumericBrackets") || tkn.getFeatures().hasProperty("beginsNumberCapital")*/)
            {
                label = "biblio-B";
            }
            else if(tkn.getFeatures().hasProperty("ignore"))
            {
                label = "junk";
            }
            else
            {
                label = "biblio-I";
            }

            ((TokenSequence)transducedData).add(label);
        }

        return transducedData;
    }
	public ReferenceData segmentReferences(NewHtmlTokenization htmlTokenization, Pipe crfInputPipe)
	{

        Pipe rulesInputPipe = transformFromCrfToRulesPipe(crfInputPipe);
		Instance inst = new Instance(htmlTokenization, null, null, null, rulesInputPipe);
        //todo: kzaporojets: here another instance with LineInfo2TokenSequence pipe, adding also data such as
        //average line width
		Sequence predictedLabels = transduce((Sequence)inst.getData()); // m_crf.transduce ((Sequence) inst.getData());


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
