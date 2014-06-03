/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Apr 30, 2004
 * author: asaunders
 */

package org.rexo.referencetagging;

import org.apache.log4j.Logger;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;
import org.rexo.pipeline.components.RxPipeline;

import java.util.Map;

public class SegmentationFilter implements RxFilter {
	private static Logger log = Logger.getLogger( SegmentationFilter.class );

	/* Extract each piece of auxillary segmentation information attached
	   to 'contentElement' and register corresponding RxDocument info
	   strings
	*/

	public int accept(RxDocument rdoc) {
		log.info( "SegmentionFilter" );

		LayoutSegmentFinder segmentationFinder = new LayoutSegmentFinder();
		try {
			Map results = segmentationFinder.markup( rdoc.getTokenization() );
			rdoc.getScope( "document" ).put( "segmentation", results );
			return ReturnCode.OK;
		}
		catch (SegmentationException exception) {
			rdoc.docErrorString( exception.getMessage() );
			log.info( "(segmentation) " + exception.getClass().getName() + ": " + exception.getMessage() );

			return ReturnCode.ABORT_PAPER;
		}
	}

	public int init(RxPipeline pipeline) {
		return ReturnCode.OK;
	}
}
