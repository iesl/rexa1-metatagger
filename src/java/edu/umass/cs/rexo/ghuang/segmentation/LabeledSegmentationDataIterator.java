package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.base.types.Instance;


/**
 * Iterate over a file containing sequence data in the "list of lines" format.
 * Each sequence (document) is separated by a blank line.
 *
 * From Alex Dingle:
 * 
 * "list of lines" format.  Each line in the "lines of text" format starts with 
 * the actual text of the line, and has a '@' before a bunch of auxillary information.
 * After the '@' there is a  pagenum,   four bounding box coordinates for the line, 
 * the font number, a boolean indicating if the line uses more than one CDATA section
 * (which means that it uses more than one font), a boolean indicating if the line 
 * begins a new reference, and a string (pre|biblio|junk|post) indicating if the line 
 * occurs before the bibliography (pre), in the bibliography (biblio), after the start 
 * of the bibliography but consists of non-bibliography text (junk), or after the 
 * bibliography(post).
 *
 * The resulting instances have String[] in their data field that consist of the 
 * lines in the file.
 *
 * @author ghuang
 *
 */
public class LabeledSegmentationDataIterator extends AbstractPipeInputIterator
{
	Iterator m_iter;

	public LabeledSegmentationDataIterator(File infile)
	{
		try {
			ArrayList instList = new ArrayList();
			ArrayList instLines = new ArrayList();
			Pattern emptyLine = Pattern.compile("\\s*");
			BufferedReader reader = new BufferedReader(new FileReader(infile));
			String line;

			while ((line = reader.readLine()) != null) {
				if (emptyLine.matcher(line).matches()) {
					if (instLines.size() > 0) {
						String[] data = new String[instLines.size()];
						
						for (int i = 0; i < instLines.size(); i++)
							data[i] = (String) instLines.get(i);

						instList.add(new Instance(data, null, null, null));
						instLines = new ArrayList();
					}
				}
				else {
					instLines.add(line);
				}
			}

			m_iter = instList.iterator();
		}
		catch (IOException e) {
			throw new IllegalStateException(e.toString());
		}
	}

	
	
	public Instance nextInstance()
	{
		assert(hasNext());
		
		return (Instance) m_iter.next(); 
	}

	
	
	public boolean hasNext()
	{
		return m_iter.hasNext();
	}

}