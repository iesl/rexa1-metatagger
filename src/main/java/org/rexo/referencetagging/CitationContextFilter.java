/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 */

package org.rexo.referencetagging;

import org.apache.log4j.Logger;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;
import org.rexo.pipeline.components.RxPipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* Scans the body and biblioPrologue of a marked up document for all
 * citations in context and appropriately surrounds citations occurences
 * with 'refroup' and 'reftag' tags.
 */

public class CitationContextFilter implements RxFilter {
	private static Logger log = Logger.getLogger( CitationContextFilter.class );

	public CitationContextFilter() {
	}

	public int accept(RxDocument rdoc) {
		log.info( "CitationContextFilter" );
		Map segmentations = (Map)rdoc.getScope( "document" ).get( "segmentation" );

		List referenceElements = (List)segmentations.get( "referenceElements" );
		NewHtmlTokenization bodyTokenization = (NewHtmlTokenization)segmentations.get( "bodyTokenization" );
		CICExtractor cicExtractor = new CICExtractor( referenceElements );
		try {
			ArrayList citationList = cicExtractor.findCitationsInContext( bodyTokenization );
			segmentations.put("citationList", citationList );
		}
		catch (CitationFinderException e) {
			log.info( "(citation finder) " + e.getClass().getName() + ": " + e.getMessage() );
		}
		catch (Throwable e) {
			log.info( "(citation finder) " + e.getClass().getName() + ": " + e.getMessage() );
		}
		return ReturnCode.OK;
	}

	public int init(RxPipeline pipeline) {
		return ReturnCode.OK;
	}
}
