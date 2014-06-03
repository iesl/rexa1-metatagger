/*
 * Created on Feb 4, 2004
 *
 */
package org.rexo.pipeline.components;

import org.omg.CORBA.Any;
import org.rexo.pipeline.components.RxFilter.ReturnCode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author asaunders
 */
public class RxPipeline {
	private RxDocumentSource _documentSource = null;
	private LinkedList _standardFilterList = new LinkedList();
	private LinkedList _errorFilterList = new LinkedList();
	private LinkedList _epilogueFilterList = new LinkedList();
	private LinkedList _workingFilterList = _standardFilterList;
	private Map _scopeMap = new HashMap();

	/**
	 *
	 */
	public RxPipeline() {
		setScope( "global", new HashMap<String, Object>() );
		setScope( "session", new HashMap<String,  Object>() );
		setScope( "document", new HashMap<String, Object>() );
		// getScope( "global" ).put( "citation.map", new HashMap() );

		getScope( "session" ).put( "pipeline.progress.iterations.integer", new Integer( 0 ) );
		getScope( "session" ).put( "pipeline.progress.last.write.integer", new Integer( 0 ) );
		getScope( "session" ).put( "continuous.execution.boolean", new Boolean( false ) );
		getScope( "session" ).put( "errorlog.boolean", new Boolean( false ) );
	}


	public Map<String, Object> getScope(String scope) {
		return (Map)_scopeMap.get( scope );
	}

	public void setScope(String scope, Map scopeMap) {
		_scopeMap.put( scope, scopeMap );
	}

	/**
	 * @param source
	 */
	public void setInputSource(RxDocumentSource source) {
		_documentSource = source;
	}

	/**
	 *
	 */
	public void execute() {
		Boolean continuous = (Boolean)getScope( "session" ).get( "continuous.execution.boolean" );
		int returnCode = ReturnCode.OK;
		int iteration = 1;
		while (returnCode != ReturnCode.ABORT_SESSION) {
			getScope( "session" ).put( "metric.corpus.iteration.documents.succeeded.integer", new Integer( 0 ) );
			Iterator iter = _documentSource.iterator();
			returnCode = execute( iter );
			if (!continuous.booleanValue()) {
				break;
			}
			getScope( "session" ).put( "metric.corpus.iteration.integer", new Integer( ++iteration ) );
		}
	}

	public int execute(Iterator iter) {
		int returnCode = ReturnCode.OK;
		while (iter.hasNext() && returnCode != ReturnCode.ABORT_SESSION) {
			RxDocument rdoc = (RxDocument)iter.next();
			returnCode = execute( rdoc );
		}
		return returnCode;
	}

	public int execute(RxDocument rdoc) {
		int returnCode = ReturnCode.OK;
		rdoc.getScope( "document" ).putAll( getScope( "document" ) );
		rdoc.setScope( "session", getScope( "session" ) );
		rdoc.setScope( "global", getScope( "global" ) );
		// Run standard filters
		for (Iterator filterIter = _standardFilterList.iterator(); filterIter.hasNext();) {
			RxFilter nextFilter = (RxFilter)filterIter.next();
			returnCode = nextFilter.accept( rdoc );
			if (returnCode == ReturnCode.ABORT_PAPER || returnCode == ReturnCode.ABORT_SESSION) {
				break;
			}
		}
		// Run 'error' filters
		if (returnCode != ReturnCode.OK && returnCode != ReturnCode.ABORT_SESSION) {
			for (Iterator filterIter = _errorFilterList.iterator(); filterIter.hasNext();) {
				RxFilter errorFilter = (RxFilter)filterIter.next();
				errorFilter.accept( rdoc );
			}
		}
		// Run 'epilogue' filters
		if (returnCode != ReturnCode.ABORT_SESSION) {
			for (Iterator filterIter = _epilogueFilterList.iterator(); filterIter.hasNext();) {
				RxFilter element = (RxFilter)filterIter.next();
				element.accept( rdoc );
			}
		}
		// Perform any extra processing (removing locks on files, commit
		// changes to the DB, etc) necessary to close the current document
		// or the entire input source if the session has been aborted.
		if (returnCode == ReturnCode.ABORT_SESSION) {
			if (_documentSource != null) {
				_documentSource.closeSource( rdoc );
			}
		}
		else {
			if (_documentSource != null) {
				_documentSource.closeDocument( rdoc );
			}
		}
		return returnCode;
	}

	public RxPipeline addStandardFilters() {
		_workingFilterList = _standardFilterList;
		return this;
	}

	public RxPipeline addErrorFilters() {
		_workingFilterList = _errorFilterList;
		return this;
	}

	public RxPipeline addEpilogueFilters() {
		_workingFilterList = _epilogueFilterList;
		return this;
	}

	public RxPipeline addPrologueFilters() {
		_workingFilterList = _epilogueFilterList;
		return this;
	}


	/**
	 * @param f
	 * @return
	 */
	public RxPipeline add(RxFilter f) {
		_workingFilterList.add( f );
		f.init( this );
		return this;
	}

	public String toString() {
		StringBuffer returnStringBuffer = new StringBuffer();

		returnStringBuffer.append( "{ standard: " );
		for (Iterator filterIter = _standardFilterList.iterator(); filterIter.hasNext();) {
			RxFilter filter = (RxFilter)filterIter.next();
			returnStringBuffer.append( filter.toString() );
			returnStringBuffer.append( " | " );
		}
		returnStringBuffer.append( "} || " );

		returnStringBuffer.append( "{ epilogue: " );
		for (Iterator filterIter = _epilogueFilterList.iterator(); filterIter.hasNext();) {
			RxFilter filter = (RxFilter)filterIter.next();
			returnStringBuffer.append( filter.toString() );
			returnStringBuffer.append( " | " );
		}
		returnStringBuffer.append( " } || " );

		returnStringBuffer.append( "{ error: " );
		for (Iterator filterIter = _errorFilterList.iterator(); filterIter.hasNext();) {
			RxFilter filter = (RxFilter)filterIter.next();
			returnStringBuffer.append( filter.toString() );
			returnStringBuffer.append( " | " );
		}
		returnStringBuffer.append( "}" );

		return returnStringBuffer.toString();
	}
}
