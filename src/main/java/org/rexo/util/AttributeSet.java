package org.rexo.util;

// import com.megginson.sax.DataWriter;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.lang.SerializationException;
import org.rexo.exceptions.InitializationException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.rexo.store.FeatureType;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * Author: saunders Created Sep 9, 2005 Copyright (C) Univ. of Massachusetts Amherst, Computer Science Dept.
 */
public class AttributeSet {

	private Map _attributeMap = new MultiHashMap();

	public AttributeSet() {
	}

	/**
	 * Map lookup with a multi-value key->value mapping
	 */
	public AttributeSet(Collection c) {
		Iterator iterator = c.iterator();
		while (iterator.hasNext()) {
			Object[] keyValuePair = (Object[])iterator.next();
			put( (FeatureType)keyValuePair[0], (String)keyValuePair[1] );
		}
	}

	public String getFirst(FeatureType key) {
		List all = getAll( key );
		return (String)(all.isEmpty() ? null : all.get( 0 ));
	}

	public List getAll(FeatureType key) {
		return _attributeMap.get( key ) != null ? (List)_attributeMap.get( key ) : ListUtils.EMPTY_LIST;
	}

	public void put(FeatureType key, String value) {
		_attributeMap.put( key, value );
	}

	public boolean contains(FeatureType key) {
		return _attributeMap.get( key ) != null && !((List)_attributeMap.get( key )).isEmpty();
	}

	public Object removeAll(FeatureType key) {
		return _attributeMap.remove( key );
	}


	public int count(FeatureType key) {
		return getAll( key ).size();
	}

	public List toList() {
		Iterator iterator = iterator();
		ArrayList returnList = new ArrayList();
		while (iterator.hasNext()) {
			returnList.add( iterator.next() );
		}
		return returnList;
	}

	public Iterator iterator() {
		return new Iterator() {
			// private Iterator _mapIter = _attributeMap.keySet().iterator();
			private Iterator _mapIter = _attributeMap.entrySet().iterator();
			private Iterator _listIter = ListUtils.EMPTY_LIST.iterator();
			private FeatureType _nextKey;

			public void remove() {
				throw new UnsupportedOperationException( "use AttributeSet.remove()" );
			}

			public boolean hasNext() {
				if (_listIter.hasNext()) {
					return true;
				}
				else {
					while (_mapIter.hasNext()) {
						Entry nextEntry = (Entry)_mapIter.next();
						List nextList = (List)nextEntry.getValue();
						if (nextList != null && !nextList.isEmpty()) {
							_listIter = nextList.iterator();
							_nextKey = (FeatureType)nextEntry.getKey();
							return true;
						}
					}
					return false;
				}
			}

			public Object next() {
				return new Object[]{_nextKey, _listIter.next()};
			}
		};
	}


//  /**
//   * output an xml (String) representation of this class
//   */
//  public String toXML() {
//  	StringWriter stringWriter = new StringWriter();
//  	DataWriter dataWriter = new DataWriter( stringWriter );
//  	dataWriter.setIndentStep( 2 );
//  	try {
//  		dataWriter.startDocument();
//  		dataWriter.startElement( "Attributes" );
//  		Iterator iterator = iterator();
//  		while (iterator.hasNext()) {
//  			Object[] keyValue = (Object[])iterator.next();
//  			FeatureType key = (FeatureType)keyValue[0];
//  			String value = (String)keyValue[1];
//  			dataWriter.dataElement( key.toString(), value );
//  		}
//  		dataWriter.endElement( "Attributes" );
//  		dataWriter.endDocument();
//  		return stringWriter.toString();
//  	}
//  	catch (SAXException e) {
//  		throw new SerializationException( e.getMessage() );
//  	}
//  }

	static XMLReader xmlReader = null;

	public static AttributeSet fromXML(String xmlString) throws InitializationException {
		StringReader stringReader = new StringReader( xmlString );

		if (xmlReader == null) {
			try {
				xmlReader = XMLReaderFactory.createXMLReader();
			}
			catch (SAXException e) {
				throw new InitializationException( e );
			}
		}
		try {
			AttributeSet attributeSet = new AttributeSet();
			UnserializeXMLHandler handler = new UnserializeXMLHandler( attributeSet._attributeMap );
			xmlReader.setContentHandler( handler );
			xmlReader.parse( new InputSource( stringReader ) );
			return attributeSet;
		}
		catch (Exception e) {
			throw new InitializationException( e );
		}
	}

	static class UnserializeXMLHandler extends DefaultHandler {
		private Map _map;

		private String _key;

		public UnserializeXMLHandler(Map map) {
			_map = map;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			_key = localName;
		}

		public void characters(char ch[], int start, int len) throws SAXException {
			String content = new String( ch, start, len );
			_map.put( _key, content.trim() );
		}
	}
}
