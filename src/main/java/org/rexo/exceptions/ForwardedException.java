/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Oct 26, 2004
 * author: saunders
 */

package org.rexo.exceptions;

public class ForwardedException extends Exception {
	protected ForwardedException(String msg) {
		super( msg );
	}

	protected ForwardedException(Throwable cause) {
		super( constructForwardedMessage( cause ), cause );
	}

	private static String constructForwardedMessage(Throwable cause) {
		String classname = cause.getClass().getName();
		int i = classname.lastIndexOf( "." );
		String uqClassname = null;
		if (i >= 0) {
			uqClassname = classname.substring( i + 1 );
		}
		else {
			uqClassname = classname;
		}

		return
		    new StringBuffer()
		    .append( "" )
		    .append( uqClassname )
		    .append( "::" )
		    .append( cause.getMessage() ).toString();
	}

	protected ForwardedException(String msg, Throwable cause) {
		super( constructForwardedMessage( cause ) + ":" + msg, cause );
	}

	/**
	 * throw the first exception in the cause-chain that is not a descendant of ForwardedException
	 */
	public void rethrowCause() throws Throwable {
		Throwable parent = this;
		while (parent != null && parent instanceof ForwardedException) {
			parent = parent.getCause();
		}
		if (parent != null) {
			throw parent;
		}
	}
}
