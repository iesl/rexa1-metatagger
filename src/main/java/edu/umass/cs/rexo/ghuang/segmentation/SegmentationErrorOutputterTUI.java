package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;
import edu.umass.cs.mallet.base.types.LabelSequence;
import edu.umass.cs.mallet.base.types.Sequence;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * 
 * @author ghuang
 *
 */
public class SegmentationErrorOutputterTUI
{
	private static final Logger logger = MalletLogger.getLogger (SegmentationErrorOutputterTUI.class.getName ());

    private static CommandOption.File crfOption = new CommandOption.File
    (SegmentationErrorOutputterTUI.class, "crf", null, true, null,
    		"serialized CRF", "");
	
    private static CommandOption.File testDataOption = new CommandOption.File
    (SegmentationErrorOutputterTUI.class, "test-data", null, true, null,
    		"file containing test data in the \"list of lines\" format..", "");
    
	public static void main(String[] args) throws Exception 
	{
		CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
		options.add (SegmentationErrorOutputterTUI.class);
		options.process (args);
		options.logOptions (logger);

		File crfFile = crfOption.value;
		ObjectInputStream ois = new ObjectInputStream (new FileInputStream (crfFile));
		CRF4 crf = (CRF4) ois.readObject();
		ois.close ();

		ArrayList pipes = ((SerialPipes) crf.getInputPipe()).getPipes();

		pipes.remove(0);
		pipes.add(0, new LabeledSegmentationData2LineInfo());
		pipes.add(1, new LineInfo2BiblioSelection());
		pipes.add(2, new LineInfo2BiblioBIOLabels());
		crf.getInputPipe().setTargetProcessing(true);

		InstanceList testList = new InstanceList(crf.getInputPipe());
		
		testList.add(new LabeledSegmentationDataIterator(testDataOption.value));

		System.out.println("Num test insts=" + testList.size());

		for (int i = 0; i < testList.size(); i++) {
			Instance inst = testList.getInstance(i);
			LabelSequence ls = (LabelSequence) inst.getTarget();
			Sequence predictedLabels = crf.transduce ((Sequence) inst.getData());
			TokenSequence source = (TokenSequence) inst.getSource();
			
			for (int j = 0; j < predictedLabels.size(); j++) {
				if (! predictedLabels.get(j).toString().equals(ls.get(j).toString())) {
					
					System.out.println("Instance " + i + ":");
					for (int k = 0; k < predictedLabels.size(); k++) {
						String predLab = predictedLabels.get(k).toString();
						String trueLab = ls.get(k).toString();
						String lineText = source.getToken(k).getText();
						String x = predLab.equals(trueLab) ? "+++" : "---";

						System.out.println(x + " " + predLab + " " + trueLab + "\t" + lineText);
					}
					System.out.println();
					break;
				}
			}
		}
		
		
	}

}
