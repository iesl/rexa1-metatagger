package org.rexo.extraction;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;

/**
 * A simple pipe that copies whatever was in the data field of each instance to the source field  
 * 
 * @author ghuang
 *
 */
public class CopyData2Source extends Pipe
{
	private static final long serialVersionUID = 1L;

	public CopyData2Source()
	{
		super();
	}

	public Instance pipe(Instance carrier)
	{
		carrier.setSource(carrier.getData());
		
		return carrier;
	}

}
