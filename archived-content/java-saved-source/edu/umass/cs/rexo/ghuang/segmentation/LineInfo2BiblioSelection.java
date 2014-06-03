package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.rexo.ghuang.segmentation.LineInfo;

/**
 * Given an instance with LineInfo[] in its data field, output an instance with
 * only those lines that are not before the bibliography section.
 * 
 * @author ghuang
 *
 */
public class LineInfo2BiblioSelection extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;

	
	public Instance pipe(Instance carrier)
	{
		LineInfo[] oldData = (LineInfo[]) carrier.getData();
		int startIdx = 0;
		
		for (int i = 0; i < oldData.length; i++) {
			if (! oldData[i].trueLabel.equals("pre")) {
				startIdx = i;
				break;
			}
		}

		for (int i = startIdx; i < oldData.length; i++)
			if (oldData[i].trueLabel.equals("pre"))
				throw new IllegalArgumentException("pre label appears after pre section! " + oldData[i].text);
		
		LineInfo[] newData = new LineInfo[oldData.length - startIdx];
		System.arraycopy(oldData, startIdx, newData, 0, oldData.length - startIdx);

		carrier.setData(newData);

		return carrier;
	}

}
