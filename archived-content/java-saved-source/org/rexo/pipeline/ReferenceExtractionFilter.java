/*
 * Created on Feb 11, 2004
 *
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * 
 */
package org.rexo.pipeline;

import edu.umass.cs.mallet.base.extract.CRFExtractor;
import edu.umass.cs.mallet.base.extract.Extraction;
import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import edu.umass.cs.mallet.base.types.Sequence;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.rexo.extraction.CRFOutputFormatter;
import org.rexo.referencetagging.NewHtmlTokenization;
import org.rexo.pipeline.components.RxDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 */
public class ReferenceExtractionFilter extends AbstractFilter {
	private static Logger log = Logger.getLogger( ReferenceExtractionFilter.class );

	private CRFExtractor _referencesExtractor;
	private CRFExtractor _headersExtractor;

	public ReferenceExtractionFilter(File referenceCrfFile, File headerCrfFile) {
		initCrfs( referenceCrfFile, headerCrfFile );
	}

	private void initCrfs(File referenceCrfFile, File headerCrfFile) {
		try {
			if (referenceCrfFile != null) {
				if (referenceCrfFile.getName().endsWith( ".partial" )) {
					_referencesExtractor = loadPartiallyTrainedModel( referenceCrfFile );
					log.info( "loaded partial reference crf '" + referenceCrfFile.getPath() + "'" );
				}
				else {
					_referencesExtractor = loadCrfExtor( referenceCrfFile );
					Pipe pipe = _referencesExtractor.getFeaturePipe();
					pipe.setTargetProcessing( false );
					log.info( "loaded reference crf '" + referenceCrfFile.getPath() + "'" );
				}
			}
		}
		catch (Exception e) {
			log.error( "couldn't init crf '" + referenceCrfFile.getPath() + "', continuing..." + e );
			_referencesExtractor = null;
		}

		try {
			if (headerCrfFile != null) {
				if (headerCrfFile.getName().endsWith( ".partial" )) {
					_headersExtractor = loadPartiallyTrainedModel( headerCrfFile );
					log.info( "loaded partial header crf '" + headerCrfFile.getPath() + "'" );
				}
				else {
					_headersExtractor = loadCrfExtor( headerCrfFile );
					Pipe pipe = _headersExtractor.getFeaturePipe();
					pipe.setTargetProcessing( false );
					log.info( "loaded header crf '" + headerCrfFile.getPath() + "'" );
				}
			}
		}
		catch (Exception e) {
			log.error( "couldn't init crf '" + headerCrfFile.getPath() + "', continuing..." );
			_headersExtractor = null;
		}

	}

	private CRFExtractor loadPartiallyTrainedModel(File crfFile) throws IOException, ClassNotFoundException {
		CRF4 crf = (CRF4)new ObjectInputStream( new BufferedInputStream( new FileInputStream( crfFile ) ) ).readObject();
		// Create the trivial tokenization pipe
		Pipe tokPipe = new SerialPipes( new Pipe[]{
				new Noop(),
		} );
		tokPipe.setTargetProcessing( false );
		return new CRFExtractor( crf, tokPipe );
	}

	private CRFExtractor loadCrfExtor(File crfFile) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream( new GZIPInputStream(new BufferedInputStream( new FileInputStream( crfFile ))));
		return (CRFExtractor)ois.readObject();
	}

	/* (non-Javadoc)
	 * @see org.rexo.pipeline.components.RxFilter#accept(org.rexo.pipeline.components.RxDocument)
	 */
	public int accept(RxDocument rdoc) {
		int errorCode = ReturnCode.OK;
		try {
			errorCode = doExtraction( rdoc ) ? ReturnCode.OK : ReturnCode.ABORT_PAPER;
		}
		catch (Exception e) {
			errorCode = ReturnCode.ABORT_PAPER;
			log.info( "(crf) " + e.getClass().getName() + ": " + e.getMessage() );
		}
		return errorCode;
	}


	/**
	 * @param rdoc
	 */
	private boolean doExtraction(RxDocument rdoc) {
		NewHtmlTokenization tokenization = rdoc.getTokenization();
		ArrayList referenceElements = new ArrayList();
		Map segmentations = (Map)rdoc.getScope( "document" ).get( "segmentation" );

		if (tokenization == null) {
			getLogger( rdoc ).error( "Partitioner found nothing to partition..." );
			rdoc.docErrorString( "Partitioner found nothing to partition" );
			return false;
		}

		// Markup header
		if (_headersExtractor != null) {
			NewHtmlTokenization header = (NewHtmlTokenization)segmentations.get( "headerTokenization" );
			if (header != null) {
				log.info("running crf on header" );
				if( header.clearTokenFeatures() ) {
					log.warn( "header tokens had features set before crf extraction" );
				}

				Extraction extraction = _headersExtractor.extract( header );
				//log.info("done.");
				Sequence predictedLabels = extraction.getDocumentExtraction( 0 ).getPredictedLabels();
				CRFOutputFormatter crfOutputFormatter = new CRFOutputFormatter();
				Element element = crfOutputFormatter.toXmlElement( header, predictedLabels, "headers" );

				// Get the first token in the document
				PropertyHolder firstHeaderToken = header.getToken( 0 );
				// Get the token's position
				double llx = firstHeaderToken.getNumericProperty( "llx" );
				double lly = firstHeaderToken.getNumericProperty( "lly" );
				int pageNum = (int)firstHeaderToken.getNumericProperty( "pageNum" );
				String persistentMentionID = "p" + pageNum + "x" + llx + "y" + lly;
				element.setAttribute( "headerID", persistentMentionID );
				segmentations.put( "headerElement", element );
			}
		}

		// Markup references
		if (_referencesExtractor != null) {
			List refList = (List)segmentations.get( "referenceList" );
			if (refList == null) {
				getLogger( rdoc ).error( "no biblio to extract" );
				rdoc.docErrorString( "no biblio to extract" );
				return false;
			}

			Iterator referenceIterator = refList.iterator();

			// For outputing full file paths in the reference warnings
			int refNum = 1;
			while (referenceIterator.hasNext()) {
				// Extract reference
				NewHtmlTokenization reference = (NewHtmlTokenization)referenceIterator.next();
				if( reference.clearTokenFeatures() ) {
					log.warn( "reference tokens had features set before crf extraction" );
				}
				log.info( "running crf on reference " + refNum + " of " + refList.size() );
				Extraction extraction = _referencesExtractor.extract( reference );
				Sequence predictedLabels = extraction.getDocumentExtraction( 0 ).getPredictedLabels();

				// Check extracted reference for validity
				String warning = checkReference( reference, predictedLabels );
				if (!warning.equals( "" )) {
					log.error( "Suspicous reference (" + refNum + "):" + warning );
				}
				CRFOutputFormatter crfOutputFormatter = new CRFOutputFormatter();
				Element element = crfOutputFormatter.toXmlElement( reference, predictedLabels, "reference" );

				// Get the first token in the reference
				PropertyHolder firstRefToken = (PropertyHolder)reference.getToken( 0 );
				// Get the token's position
				double llx = firstRefToken.getNumericProperty( "llx" );
				double lly = firstRefToken.getNumericProperty( "lly" );
				int pageNum = (int)firstRefToken.getNumericProperty( "pageNum" );
				String persistentMentionID = "p" + pageNum + "x" + llx + "y" + lly;
				element.setAttribute( "refID", persistentMentionID );

				referenceElements.add( element );
				refNum++;
			}
		}
		segmentations.put( "referenceElements", referenceElements );

		return true;
	}

	// Analyze the predicted labels for a reference in an attempt to find improperly segmented
	// (i.e. too long) references.  If a suspect reference is found, return a truncated NewHtmlTokenization;
	// otherwise return null.
	//private NewHtmlTokenization fixReference(NewHtmlTokenization tokens, Sequence predictedTags) {
	private String checkReference(NewHtmlTokenization tokens, Sequence predictedTags) {
		assert tokens.size() == predictedTags.size();

		boolean seenMarker = false;
		boolean seenAuthors = false;
		boolean seenTitle = false;
		String warning = "";

		String previousTag = "";

		for (int i = 0; i < predictedTags.size(); i++) {
			String tag = predictedTags.get( i ).toString();
			boolean truncateHere = false;

			if (previousTag.startsWith( "ref-marker" ) && !tag.startsWith( "ref-marker" )) {
				seenMarker = true;
			}
			if (previousTag.startsWith( "author" ) && !tag.startsWith( "author" )) {
				seenAuthors = true;
			}
			if (previousTag.startsWith( "title" ) && !tag.startsWith( "title" )) {
				seenTitle = true;
			}
			// FIXME: should we really truncate on duplicate ref-marker if we haven't seen authors or title yet?
			boolean newMarker = (tag.startsWith( "ref-marker" ) && !previousTag.startsWith( "ref-marker" ));
			if ((seenMarker || seenAuthors || seenTitle) && newMarker) {
				truncateHere = true;
				warning = warning + "duplicate ref-marker;";
			}
			boolean newAuthor = (tag.startsWith( "author" ) && !previousTag.startsWith( "author" ));
			if (seenAuthors && newAuthor) {
				truncateHere = true;
				warning = warning + "duplicate authors;";
			}
			boolean newTitle = (tag.startsWith( "title" ) && !previousTag.startsWith( "title" ));
			if (seenTitle && newTitle) {
				truncateHere = true;
				warning = warning + "duplicate title;";
			}

			previousTag = tag;
		}
		return warning;
	}

}
