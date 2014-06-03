package org.rexo.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.base.types.Instance;

/**
 * Given 2 lists of filenames, one filename per line, iterate both files
 * line by line and set the next Instance's data field to contain the pair
 * of filenames
 * 
 * @author ghuang
 *
 */
public class SourceAnnoListsIterator extends AbstractPipeInputIterator
{
	File m_srcFileList;
	File m_annoFileList;
	HashMap m_map;
	Iterator m_iterator;
	
	public SourceAnnoListsIterator(File srcFileList, File annoFileList)
	{
		m_srcFileList = srcFileList;
		m_annoFileList = annoFileList;
		m_map = new HashMap();
	
		try {
			BufferedReader reader1 = new BufferedReader(new FileReader(srcFileList));
			BufferedReader reader2 = new BufferedReader(new FileReader(annoFileList));
			String s1 = reader1.readLine();
			String s2 = reader2.readLine();
			
			while (s1 != null && s2 != null) {
				m_map.put(s1, s2);
				s1 = reader1.readLine();
				s2 = reader2.readLine();
			}
			
			assert(s1 == null && s2 == null);
			m_iterator = m_map.keySet().iterator();
		}
		catch (IOException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	
	public Instance nextInstance()
	{
		assert(m_iterator.hasNext());

		Object key = m_iterator.next();
		File src = new File((String) key);
		File anno = new File((String) m_map.get(key));
		File[] pair = new File[] { anno, src };
		Instance inst = new Instance(pair, null, src.getPath(), pair);
		
		return inst;
	}

	
	public boolean hasNext()
	{
		return m_iterator.hasNext();
	}

}
