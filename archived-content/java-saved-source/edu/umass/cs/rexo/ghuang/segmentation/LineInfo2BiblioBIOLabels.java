package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.rexo.ghuang.segmentation.LineInfo;

/**
 * Convert the sequence of labels contained in the LineInfo[] to BIO format. 
 * 
 * @author ghuang
 *
 */
public class LineInfo2BiblioBIOLabels extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String m_biblioLabel;
	

	public LineInfo2BiblioBIOLabels()
	{
		this("biblio");
	}
	
	
	public LineInfo2BiblioBIOLabels(String biblioLabel)
	{
		super();
		m_biblioLabel = biblioLabel;
	}
	

	public Instance pipe(Instance carrier)
	{
		LineInfo[] data = (LineInfo[]) carrier.getData();
		
		for (int i = 0; i < data.length; i++) {
			if (data[i].trueLabel.equals(m_biblioLabel)) {
				if (data[i].newRef) {
					data[i].trueLabel = m_biblioLabel + "-B";
				}
				else {
					data[i].trueLabel = m_biblioLabel + "-I";
				}
			}
		}

		return carrier;
	}

}
