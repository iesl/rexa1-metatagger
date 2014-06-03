/**
 * Created on Jan 20, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */

package org.rexo.extraction;

import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.base.types.Instance;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Iterator;

/**
   Iterates over a pair of directories, one containing the source docs and 
   one containing annotations for the source docs.
*/
public class DirectoryPairIterator extends AbstractPipeInputIterator
{
    File m_srcDir;
    File m_annoDir;
    String m_srcFileSuffix;
    String m_annoFileSuffix;
    HashMap m_fileMap;
    Iterator m_iter;

    public DirectoryPairIterator (File srcDir, File annoDir)
    {
        this(srcDir, annoDir, "", "");
    }
    
    /**
       Sets the directories that contatin the documents and annotation files.  
       
       <p>The suffix arguments correspond to the suffixes of each filename
       in the corresponding directories such that, when deleted from the filenames,
       results in identical prefixes that are used to match documents with  
       annotations.
    */
    public DirectoryPairIterator (File srcDir, File annoDir, String srcFileSuffix, String annoFileSuffix)
    {
        m_srcDir = srcDir;
        m_annoDir = annoDir;
        m_srcFileSuffix = srcFileSuffix;
        m_annoFileSuffix = annoFileSuffix;
        setUpFileMap();
        m_iter = m_fileMap.keySet().iterator();
    }

    private void setUpFileMap()
    {
        if (! m_srcDir.isDirectory())
	  throw new IllegalArgumentException("Source dir parameter is not a directory: " + m_srcDir);
        if (! m_annoDir.isDirectory())
	  throw new IllegalArgumentException("Annotation dir parameter is not a directory: " + m_annoDir);

        File[] annoFiles = m_annoDir.listFiles(new FileFilter() {
	      public boolean accept(File pathName) {
		String fileName = pathName.getName();
		return m_annoFileSuffix.equals("") 
		                 || 
		    (fileName.endsWith(m_annoFileSuffix) && fileName.length() > m_annoFileSuffix.length());
	      }
	  });

        m_fileMap = new HashMap();

        for (int i = 0; i < annoFiles.length; i++) {
	  String annoFilename = annoFiles[i].getName();
	  int end = m_annoFileSuffix.equals("") ? annoFilename.length() : annoFilename.lastIndexOf(m_annoFileSuffix);
	  assert(end > 0);
	  String commonPrefix = annoFilename.substring(0, end);
	  File srcFile = new File(m_srcDir, commonPrefix + m_srcFileSuffix);

	  if (! srcFile.exists() || srcFile.isDirectory())
	      throw new IllegalArgumentException("Annotation file " + annoFilename + " has no corresponding document file " + srcFile);
	  
	  m_fileMap.put(annoFiles[i], srcFile);
        }        
    }

    // The PipeInputIterator interface

    public Instance nextInstance ()
    {
        assert (m_iter != null);
        File key = (File) m_iter.next();
        File val = (File) m_fileMap.get(key);
        assert(val != null);
        File[] pair = new File[]{ key, val };
        Instance carrier = new Instance (pair, null, pair[1].getPath(), pair);

        return carrier;
    }
    
    public boolean hasNext ()	{ return m_iter.hasNext(); }
}
