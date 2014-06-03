package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;

/**
 * 
 * @author ghuang
 *
 */
public class PrintLabeledLineInfo extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;

	public Instance pipe(Instance carrier)
	{
		if (! isTargetProcessing()) return carrier; 

		LineInfo[] data = (LineInfo[]) carrier.getData();
		String prevLab = null;

		for (int i = 0; i < data.length; i++) {
			String lab = data[i].trueLabel;
			
			if (lab.equals("biblio-B") || (prevLab != null && ! lab.equals(prevLab) && ! lab.equals("biblio-I")))
				System.out.println();

			System.out.println(lab + " " + data[i].text);

			prevLab = lab;
		}

		System.out.println("\n\n");

		return carrier;
	}

}
