package org.rexo.enums;


/** Author: saunders Created Aug 10, 2006 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept. */
public class NoSuchEnumerationValueException extends RuntimeException {
	public NoSuchEnumerationValueException(String msg) {
		super( msg );
	}

	public NoSuchEnumerationValueException(Throwable cause) {
		super( cause );
	}

	public NoSuchEnumerationValueException(String msg, Throwable cause) {
		super( msg, cause );
	}
}
