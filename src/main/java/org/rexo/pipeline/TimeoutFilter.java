/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 19, 2004
 * author: asaunders
 */

package org.rexo.pipeline;

import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;

/**
 * Wraps another filter to provide a timeout mechanism
 */
public class TimeoutFilter extends AbstractFilter {
	private RxFilter _filter = null;
	private int _timeout;
	private int _retval;
	private boolean _isTimeOut;

	public TimeoutFilter(RxFilter filter, int timeout) {
		_filter = filter;
		_timeout = timeout;
	}

	protected TimeoutFilter() {
	}

	public int accept(final RxDocument rdoc) {
		_retval = ReturnCode.ABORT_PAPER;
		_isTimeOut = true;

		Runnable r = new Runnable() {
			public void run() {
				// Run the main filter
				_retval = _filter.accept( rdoc );
				_isTimeOut = false;
			}
		};
		Thread t = new Thread( r );
		t.setDaemon( true );

		// Run the main filter in a child thread
		t.start();

		// wait to be woken up by thread 't' or
		// for a timeout to occur
		try {
			t.join( _timeout * 1000 );
		}
		catch (InterruptedException e) {
			getLogger( rdoc ).debug( e );
		}
		try {
			// If a timeout occurred, update the logs and wait for the child
			// thread to finish
			if (_isTimeOut) {
				getLogger( rdoc ).debug( _filter.getClass().toString() + "Timeout" + _timeout + "s" );
				rdoc.docInfoString( _filter.getClass().toString() + "Timeout" + _timeout + "s" );
				getLogger( rdoc ).debug( "Waiting..." );
				t.join();
			}
		}
		catch (InterruptedException e) {
			getLogger( rdoc ).debug( e );
		}

		return _retval;
	}
}
