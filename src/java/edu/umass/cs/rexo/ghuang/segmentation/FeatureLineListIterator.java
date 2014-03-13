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
 * Read from a text file and iterate over its lines containing binary features names followed by its label. 
 * Each sequence is separated by a blank line.
 * The resulting instances have a String array of the lines as its data field (excluding labels)
 * and a String array of labels as its target field.
 *  
 * @author ghuang
 */
public class FeatureLineListIterator extends AbstractPipeInputIterator
{
	char m_commentLineStartChar = '#';
	Iterator m_iterator;


	public FeatureLineListIterator(File infile)
	{
		try {
			BufferedReader reader = new BufferedReader(new FileReader(infile));
			ArrayList instLines = new ArrayList();
			ArrayList instList = new ArrayList();
			Pattern emptyLine = Pattern.compile("\\s*");
			String line;

			while ((line = reader.readLine()) != null) {
				if (emptyLine.matcher(line).matches()) {
					if (instLines.size() > 0) {
						instList.add(instLines);
						instLines = new ArrayList();
					}
				}
				else if (line.charAt(0) != m_commentLineStartChar) {
					instLines.add(line);
				}
			}

			m_iterator = instList.iterator();
		}
		catch (IOException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	
	public Instance nextInstance() 
	{
		assert(hasNext());

		ArrayList instLines = (ArrayList) m_iterator.next();
		String[][] instData = new String[instLines.size()][];
		String[] target = new String[instLines.size()];

		for (int i = 0; i < instLines.size(); i++) {
			String line = (String) instLines.get(i);
			String[] toks = line.split("\\s+");
			target[i] = toks[toks.length - 1];
			instData[i] = new String[toks.length - 1];
			System.arraycopy(toks, 0, instData[i], 0, toks.length - 1);
		}

		Instance ret = new Instance(instData, target, null, null);
		
		return ret;
	}

	
	public boolean hasNext() 
	{
		return m_iterator.hasNext();
	}
}