package org.rexo.span;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.FeatureVector;
import edu.umass.cs.mallet.base.types.PropertyHolder;
import edu.umass.cs.mallet.base.util.PropertyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: saunders Created Nov 10, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class CompositeSpan implements Span, PropertyHolder {
	private List _spans = new ArrayList();
	private CharSequence _document;

	public static CompositeSpan createSpan(CharSequence document) {
		return new CompositeSpan( document );
	}

	private CompositeSpan(CharSequence document) {
		_document = document;
	}

	public void appendSpan(Span s) {
		_spans.add( s );
	}

	public String getText() {
		StringBuffer rangeText = new StringBuffer();
		for (int i = 0; i < _spans.size(); i++) {
			StringSpan s = (StringSpan)_spans.get( i );
			boolean isVisible = s.getNumericProperty( "invisible" ) == 0;
			if (isVisible) {
				rangeText.append( s.getText() );
				double numericProperty = s.getNumericProperty( "trailing-ws-1" ) + 1;
				for (int j = 0; j < numericProperty; j++) {
					rangeText.append( " " );
				}
			}
		}
		return rangeText.toString();
	}

	public Span intersection(Span r) {
		return null;
	}

	public Object getDocument() {
		return _document;
	}

	public boolean intersects(Span r) {
		return false;
	}

	public boolean isSubspan(Span r) {
		return false;
	}

	public int getStartIdx() {
		if (_spans.isEmpty()) {
			return 0;
		}
		Span s = (Span)_spans.get( 0 );
		return s.getStartIdx();
	}

	public int getEndIdx() {
		if (_spans.isEmpty()) {
			return 0;
		}
		Span s = (Span)_spans.get( _spans.size() - 1 );
		return s.getEndIdx();
	}

	public String toString() {
		return "[" + _spans.size() + "]: " + getText();
	}


	public void setProperty(String key, Object value) {
		throw new UnsupportedOperationException( "" );
	}

	public Object getProperty(String key) {
		for (int i = 0; i < _spans.size(); i++) {
			StringSpan s = (StringSpan)_spans.get( i );
			if (s.hasProperty( key )) {
				return s.getProperty( key );
			}
		}
		return null;
	}

	public void setNumericProperty(String key, double value) {
		throw new UnsupportedOperationException( "" );
	}


	public double getNumericProperty(String key) {
		for (int i = 0; i < _spans.size(); i++) {
			StringSpan s = (StringSpan)_spans.get( i );
			if (s.hasProperty( key )) {
				return s.getNumericProperty( key );
			}
		}
		return 0.0;
	}


	public PropertyList getProperties() {
		throw new UnsupportedOperationException( "" );
	}

	public void setProperties(PropertyList newProperties) {
		throw new UnsupportedOperationException( "" );
	}


	public boolean hasProperty(String key) {
		for (int i = 0; i < _spans.size(); i++) {
			StringSpan s = (StringSpan)_spans.get( i );
			if (s.hasProperty( key )) {
				return true;
			}
		}
		return false;
	}

	public void setFeatureValue(String key, double value) {
		for (int i = 0; i < _spans.size(); i++) {
			StringSpan s = (StringSpan)_spans.get( i );
			s.setFeatureValue( key, value );
		}
	}

	public double getFeatureValue(String key) {
		if (!_spans.isEmpty()) {
			StringSpan s = (StringSpan)_spans.get( 0 );
			return s.getFeatureValue( key );
		}
		return 0.0;
	}

	public PropertyList getFeatures() {
		if (!_spans.isEmpty()) {
			StringSpan s = (StringSpan)_spans.get( 0 );
			return s.getFeatures();
		}
		return null;
	}

	public void setFeatures(PropertyList pl) {
		for (int i = 0; i < _spans.size(); i++) {
			StringSpan s = (StringSpan)_spans.get( i );
			s.setFeatures( pl );
		}
	}

	public FeatureVector toFeatureVector(Alphabet dict, boolean binary) {
		if (!_spans.isEmpty()) {
			StringSpan s = (StringSpan)_spans.get( 0 );
			return s.toFeatureVector( dict, binary );
		}
		return null;
	}

	public int getBeginTokenIndex() {
		if (_spans.isEmpty()) {
			return 0;
		}
		StringSpan s = (StringSpan)_spans.get( 0 );
		return 0; // s.getBeginTokenIndex();
	}

	public int getEndTokenIndex() {
		if (_spans.isEmpty()) {
			return 0;
		}
		StringSpan s = (StringSpan)_spans.get( 0 );
		return 0; // s.getEndTokenIndex();
	}
}
