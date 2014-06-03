/**
 * Created on Jan 20, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */

package org.rexo.extraction;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.iterator.AbstractPipeInputIterator;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.rexo.referencetagging.NewHtmlTokenization;
import org.rexo.util.EnglishDictionary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.io.*;

/**
 * Iterates over a pair of directories, one containing the source docs and one containing annotations for the source docs.
 * <p/>
 * For each annotation/source file pair, create 0 or more NewHtmlTokenizations that correspond to regions in the document labeled by the contextTag. The resulting instances have the annotation/source
 * file pair stored in the source field.
 * <p/>
 * This rather awkward functionality (can be seen both as a pipe and as an iterator) is needed to ensure the resulting NewHtmlTokenizations free up unneeded memory as soon as possible.
 */
public class FilePair2NewHtmlTokenizationIterator extends AbstractPipeInputIterator {
	static SAXBuilder saxBuilder = new SAXBuilder();  // XXX is this thread-safe?

	Iterator m_iter;

	/**
	 * @param inputInsts           the input instances, whose data fields contain File[] objects of length 2, such as those created by SourceAnnoListsIterator
	 * @param contextTag           the label in the source documents from which to create token sequences
	 * @param dictForDehyphenation the dictionary file used to dehyphenation
	 * @param postProcessPipe      a Pipe through which each token sequence is processed prior to being returned
	 */
	public FilePair2NewHtmlTokenizationIterator(InstanceList inputInsts, String contextTag, File dictForDehyphenation, Pipe postProcessPipe) {
		// Read in the dictionary
		EnglishDictionary dict  = EnglishDictionary.create( dictForDehyphenation );

		// We need to build the entire list of NewHtmlTokenization's here because
		// there might be ones that have 0 subtokenizations inside the context tag,
		// making hasNext() inaccurate
		List outputInsts = new ArrayList();

		for (int ii = 0; ii < inputInsts.size(); ii++) {
			Instance inputInst = inputInsts.getInstance( ii );
			String currInstName = inputInst.getName().toString();
			Object currInstTarget = inputInst.getTarget();
			Object currInstSource = inputInst.getData();  // the file pair
			File[] pair = (File[])currInstSource;
			File spanFile = pair[0];
			File docFile = pair[1];
			int subTokenizationNum = 0;

			System.out.println( ">>>>>>>>>>>>>>> " + ii + ": " + docFile.toString());

			try {
				Document doc = saxBuilder.build( new FileInputStream( docFile ) );
				BufferedReader spanReader = new BufferedReader( new FileReader( spanFile ) );
				NewHtmlTokenization tokenization = new NewHtmlTokenization( doc, dict, spanReader );

				//  Add all sub tokenizations marked by the context tag
				List subTokList = tokenization.getSubtokenizationsByName( contextTag );
				Iterator it = subTokList.iterator();

				while (it.hasNext()) {
					NewHtmlTokenization subTok = (NewHtmlTokenization)it.next();
					Instance ret = new Instance( subTok, currInstTarget, currInstName + "_SUBSPAN_" + subTokenizationNum, currInstSource );
					subTokenizationNum++;

					if (postProcessPipe != null)
						ret = postProcessPipe.pipe( ret );

					outputInsts.add( ret );
				}

			}
			catch (Exception e) {
				e.printStackTrace();
				throw new IllegalStateException( e.toString());
			}
		}

		m_iter = outputInsts.iterator();
	}

	public Instance nextInstance() {
		assert(hasNext());
		return (Instance)m_iter.next();
	}

	public boolean hasNext() {
		return m_iter.hasNext();
	}

}
