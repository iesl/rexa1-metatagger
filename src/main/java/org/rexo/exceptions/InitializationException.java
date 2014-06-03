/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Feb 16, 2005
 * author: saunders
 */

package org.rexo.exceptions;


public class InitializationException extends ForwardedException {
	public InitializationException(String msg) {
		super( msg );
	}

	public InitializationException(Throwable cause) {
		super( cause );
	}

	public InitializationException(String msg, Throwable cause) {
		super( msg, cause );
	}
}
