/**
 * Created on Jan 24, 2005
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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
   Iterates over an <tt>InstanceList</tt> whose instances contain <tt>NewHtmlTokenization</tt> 
   as data.  Returns one instance per subtokenization within the given context tag.
   <p>
   The resulting number of instances may be smaller than, equal to, to larger than the input list.
   <p>
   One can provide an optional pipe that will be used to process each subtokenization.
*/
public class NewHtmlTokenizationIterator extends AbstractPipeInputIterator
{
    String m_contextTag;
    InstanceList m_inputInsts;
    ArrayList m_outputInsts;
    Iterator m_iter;
    Pipe m_postProcessPipe;
    
    public NewHtmlTokenizationIterator (InstanceList inputInsts, String contextTag, Pipe postProcessPipe)
    {
    	m_inputInsts = inputInsts;
    	m_contextTag = contextTag;
    	m_outputInsts = new ArrayList();
    	m_postProcessPipe = postProcessPipe;

    	for (int ii = 0; ii < inputInsts.size(); ii++) {
    		Instance inst = inputInsts.getInstance(ii);
    		NewHtmlTokenization tokenization = (NewHtmlTokenization) inst.getData();
    		
    		// Add all portions within the context tag
    		List subTokensList = tokenization.getSubtokenizationsByName(m_contextTag);
    		
    		System.out.println("^^^ " + inst.getName() + " " + subTokensList.size());
    		
    		for (int ti = 0; ti < subTokensList.size(); ti++) {
    			NewHtmlTokenization subTokenization = (NewHtmlTokenization) subTokensList.get(ti);
    			Instance result = new Instance(subTokenization, inst.getTarget(), inst.getName(), inst.getSource());
    			
    			if (m_postProcessPipe != null)
    				result = m_postProcessPipe.pipe(result);
    			
    			m_outputInsts.add(result);
    		}
    	}

    	m_iter = m_outputInsts.iterator();
    }
    
    // The PipeInputIterator interface

    public Instance nextInstance ()
    {
        assert(hasNext());

        return (Instance) m_iter.next();
    }
    
    public boolean hasNext ()	{ return m_iter.hasNext(); }

}
