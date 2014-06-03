/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on Feb 15, 2005
 * author: saunders
 */

package org.rexo.store;

import org.rexo.enums.ValuedEnumeration;
import org.rexo.enums.NoSuchEnumerationValueException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FeatureType extends ValuedEnumeration {

	public static final int NULL_C = 0;
	public static final int ABSTRACT_C = 1;
	public static final int ADDRESS_C = 2;
	public static final int AFFILIATION_C = 3;
	// public static final int = 4;
	public static final int BOOKTITLE_C = 5;
	public static final int DATE_C = 6;
	public static final int DEGREE_C = 7;
	public static final int EDITOR_C = 8;
	public static final int EMAIL_C = 9;
	public static final int INSTITUTION_C = 10;
	public static final int INTRO_C = 11;
	public static final int JOURNAL_C = 12;
	public static final int KEYWORD_C = 13;
	public static final int LOCATION_C = 14;
	public static final int NOTE_C = 15;
	public static final int PAGES_C = 16;
	public static final int PHONE_C = 17;
	public static final int PUBLISHER_C = 18;
	public static final int PUBNUM_C = 19;
	public static final int TECH_C = 20;
	public static final int TITLE_C = 21;
	public static final int VOLUME_C = 22;
	public static final int WEB_C = 23;
	public static final int BODY_C = 24;
	public static final int SOURCE_TEXT_C = 25;
	public static final int AUTHORLIST_C = 26;
	public static final int GRANT_NUMBER_C = 44;
	public static final int VENUE_C = 54;
	public static final int MENTION_TYPE_C = 55;
	public static final int MENTION_SPECIFIER_C = 56;
	// public static final int = 57
	public static final int DEDUPLICATION_STRING_C = 58;
	public static final int FILE_SHA1_C = 59;
	public static final int CITE_COUNT_C = 60;
	public static final int ALT_AUTHORLIST_C = 61;


	public static final int ALT_TITLE_C = 100;

	public static final int AUTHOR_IN_FOCUS_C = 101;
	public static final FeatureType AUTHOR_IN_FOCUS = new FeatureType( "author-in-focus", AUTHOR_IN_FOCUS_C  );

	public static final int AUTHOR_IN_FOCUS_SCORE_C = 102;
	public static final FeatureType AUTHOR_IN_FOCUS_SCORE = new FeatureType( "author-in-focus-score", AUTHOR_IN_FOCUS_SCORE_C  );

	public static final int PERSISTENT_ALIAS_C = 103;
	public static final FeatureType PERSISTENT_ALIAS = new FeatureType( "persistent-alias", PERSISTENT_ALIAS_C );


	public static final FeatureType NULL = new FeatureType( "null", NULL_C );
	public static final FeatureType ABSTRACT = new FeatureType( "abstract", ABSTRACT_C );
	public static final FeatureType ADDRESS = new FeatureType( "address", ADDRESS_C );
	public static final FeatureType AFFILIATION = new FeatureType( "affiliation", AFFILIATION_C );
	public static final FeatureType BOOKTITLE = new FeatureType( "booktitle", BOOKTITLE_C );
	public static final FeatureType DATE = new FeatureType( "date", DATE_C );
	public static final FeatureType DEGREE = new FeatureType( "degree", DEGREE_C );
	public static final FeatureType EDITOR = new FeatureType( "editor", EDITOR_C );
	public static final FeatureType EMAIL = new FeatureType( "email", EMAIL_C );
	public static final FeatureType INSTITUTION = new FeatureType( "institution", INSTITUTION_C );
	public static final FeatureType INTRO = new FeatureType( "intro", INTRO_C );
	public static final FeatureType JOURNAL = new FeatureType( "journal", JOURNAL_C );
	public static final FeatureType KEYWORD = new FeatureType( "keyword", KEYWORD_C );
	public static final FeatureType LOCATION = new FeatureType( "location", LOCATION_C );
	public static final FeatureType NOTE = new FeatureType( "note", NOTE_C );
	public static final FeatureType PAGES = new FeatureType( "pages", PAGES_C );
	public static final FeatureType PHONE = new FeatureType( "phone", PHONE_C );
	public static final FeatureType PUBLISHER = new FeatureType( "publisher", PUBLISHER_C );
	public static final FeatureType PUBNUM = new FeatureType( "pubnum", PUBNUM_C );
	public static final FeatureType TECH = new FeatureType( "tech", TECH_C );
	public static final FeatureType TITLE = new FeatureType( "title", TITLE_C );
	public static final FeatureType VOLUME = new FeatureType( "volume", VOLUME_C );
	public static final FeatureType WEB = new FeatureType( "web", WEB_C );
	public static final FeatureType BODY = new FeatureType( "body", BODY_C );
	public static final FeatureType SOURCE_TEXT = new FeatureType( "sourceText", SOURCE_TEXT_C );
	public static final FeatureType AUTHORLIST = new FeatureType( "authorlist", AUTHORLIST_C );
	public static final FeatureType CONFERENCE = new FeatureType( "conference", 27 );
	public static final FeatureType REF_MARKER = new FeatureType( "refMarker", 28 );
	public static final FeatureType SERIES = new FeatureType( "series", 29 );
	public static final FeatureType THESIS = new FeatureType( "thesis", 30 );
	public static final FeatureType NUMBER = new FeatureType( "number", 31 );

	// DBLP dataset
	public static final FeatureType EE = new FeatureType( "ee", 32 );
	public static final FeatureType CDROM = new FeatureType( "cdrom", 33 );
	public static final FeatureType CITE = new FeatureType( "cite", 34 );
	public static final FeatureType CROSSREF = new FeatureType( "crossref", 35 );
	public static final FeatureType ISBN = new FeatureType( "isbn", 36 );
	public static final FeatureType CHAPTER = new FeatureType( "chapter", 37 );
	public static final FeatureType LAYOUT = new FeatureType( "layout", 38 );
	public static final FeatureType REF = new FeatureType( "ref", 39 );

	// More features
	public static final FeatureType YEAR = new FeatureType( "year", 40 );
	public static final FeatureType MONTH = new FeatureType( "month", 41 );
	public static final FeatureType AUTHOR_HOMEPAGE = new FeatureType( "authorHomepage", 42 );
	public static final FeatureType VENUE_TYPE = new FeatureType( "venueType", 43 );

	public static final int GRANT_TITLE_C = 45;
	public static final int GRANT_ABSTRACT_C = 50;

	public static final FeatureType GRANT_NUMBER = new FeatureType( "grantNumber", GRANT_NUMBER_C );
	public static final FeatureType GRANT_TITLE = new FeatureType( "grantTitle", GRANT_TITLE_C  );
	public static final FeatureType GRANT_BEGIN_DATE = new FeatureType( "grantBeginDate", 46 );
	public static final FeatureType GRANT_PI = new FeatureType( "grantPI", 47 );
	public static final FeatureType GRANT_MANAGER = new FeatureType( "grantManager", 48 );
	public static final FeatureType GRANT_COPIS = new FeatureType( "grantCOPIs", 49 );
	public static final FeatureType GRANT_ABSTRACT = new FeatureType( "grantAbstract", GRANT_ABSTRACT_C );
	public static final FeatureType GRANT_AGENCY = new FeatureType( "grantAgency", 51 );
	public static final FeatureType GRANT_END_DATE = new FeatureType( "grantEndDate", 52 );
	public static final FeatureType GRANT_TYPE = new FeatureType( "grantType", 53 );

	//
	public static final FeatureType MENTION_TYPE = new FeatureType( "mentionType", MENTION_TYPE_C );
	public static final FeatureType MENTION_SPECIFIER = new FeatureType( "mentionSpecifier", MENTION_SPECIFIER_C );

	public static final FeatureType ALT_TITLE = new FeatureType( "altTitle", ALT_TITLE_C );
	public static final FeatureType VENUE = new FeatureType( "venue", VENUE_C );
	public static final FeatureType DEDUPLICATION_STRING = new FeatureType( "deduplicationString",
	                                                                        DEDUPLICATION_STRING_C );
	public static final FeatureType FILE_SHA1 = new FeatureType( "fileSHA1", FILE_SHA1_C );
	public static final FeatureType CITE_COUNT = new FeatureType( "citeCount", CITE_COUNT_C );
	public static final FeatureType ALT_AUTHORLIST = new FeatureType( "alt-authorlist", ALT_AUTHORLIST_C );


	public static FeatureType getEnum(String name) {
		return (FeatureType)getEnum( FeatureType.class, name );
	}

	public static FeatureType getEnum(int id) {
		return (FeatureType)getEnum( FeatureType.class, id );
	}

	public static Map getEnumMap() {
		return getEnumMap( FeatureType.class );
	}

	public static List getEnumList() {
		return getEnumList( FeatureType.class );
	}

	public static Iterator iterator() {
		return iterator( FeatureType.class );
	}

	private FeatureType(String name, int value) {
		super( name, value );
	}

	public static boolean isValid(String name) {
		return getEnum( name ) != null;
	}

	public static FeatureType forName(String name) {
		return getEnum( name );
	}

	public static FeatureType forID(final int id) throws NoSuchEnumerationValueException {
		FeatureType enm = getEnum( id );
		if (enm == null) {
			throw new NoSuchEnumerationValueException( "id=" + id );
		}
		return enm;
	}

	public static FeatureType forID(final Integer id) throws NoSuchEnumerationValueException {
		return forID( id.intValue() );
	}

	public String toString() {
		return getName();
	}

	public Integer getId() {
		return new Integer( getValue() );
	}
}
