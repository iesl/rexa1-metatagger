/*
 * Created on Aug 24, 2004
 *
 */
package org.rexo.pipeline.components;

import java.util.Iterator;

/**
 * @author adingle
 */
public interface RxDocumentSource {

	/**
	 * Sets a limit on the number of documents for the iterator to return.
	 */
	public void setMaxDocuments(long max);

	/**
	 * Returns an iterator of initialized RxDocuments.
	 */
	public Iterator iterator();

	/**
	 * Performs any necessary extra processing to close the input associated with this RxDocument.
	 */
	public void closeDocument(RxDocument rdoc);

	/**
	 * Performs any necessary processing to close down this document source prior exiting the program (such as clearing any
	 * locks held in the DB).
	 *
	 * @param lastDoc The last document processed.
	 */
	public void closeSource(RxDocument lastDoc);
}
