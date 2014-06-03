/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 25, 2005
 * author: saunders
 */

package org.rexo.util;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.rexo.exceptions.ForwardedException;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {
	private static Logger log = Logger.getLogger( DateParser.class );

	private static final Calendar calendar = new GregorianCalendar();
	private static final int _year = calendar.get( Calendar.YEAR );
	public static final Object[][] MONTHS = new Object[][] {
		{ Pattern.compile( "jan", Pattern.CASE_INSENSITIVE ), "Jan", },
		{ Pattern.compile( "feb", Pattern.CASE_INSENSITIVE ), "Feb", },
		{ Pattern.compile( "mar", Pattern.CASE_INSENSITIVE ), "Mar", },
		{ Pattern.compile( "apr", Pattern.CASE_INSENSITIVE ), "Apr", },
		{ Pattern.compile( "may", Pattern.CASE_INSENSITIVE ), "May", },
		{ Pattern.compile( "jun", Pattern.CASE_INSENSITIVE ), "Jun", },
		{ Pattern.compile( "jul", Pattern.CASE_INSENSITIVE ), "Jul", },
		{ Pattern.compile( "aug", Pattern.CASE_INSENSITIVE ), "Aug", },
		{ Pattern.compile( "sep", Pattern.CASE_INSENSITIVE ), "Sep", },
		{ Pattern.compile( "oct", Pattern.CASE_INSENSITIVE ), "Oct", },
		{ Pattern.compile( "nov", Pattern.CASE_INSENSITIVE ), "Nov", },
		{ Pattern.compile( "dec", Pattern.CASE_INSENSITIVE ), "Dec", },
	};

	private static String yearRE = "(?:[12][0-9]{3,3}|[']\\d{2,2})";
	private static Pattern yearPattern = Pattern.compile( yearRE );

	/**
	 *
	 * @param date
	 * @return
	 * @throws DateParseException
	 */
	static public String parseDate(final String date) throws DateParseException {
		// pull out 4-digit year
		Matcher matcher = yearPattern.matcher( date );
		ArrayList dateParts = new ArrayList();
		boolean foundYear = false;
		while (!foundYear && matcher.find()) {
			String year = date.substring( matcher.start(), matcher.end() );
			if (year.length() == 3) {
				year = year.substring( 1 );
				if (year.startsWith( "0" )) {
					year = "20" + year;
				}
				else {
					year = "19" + year;
				}
			}
			else if ( year.length() != 4 ) {
				throw new DateParseException( "couldn't parse '" + date + "'" );
			}
			Integer yearInt = new Integer( year );
			if (1700 < yearInt.intValue() && yearInt.intValue() <= _year) {
				dateParts.add( "year=" + year );
				foundYear = true;
			}
		}

		// pull out month
    for (int i = 0; i < MONTHS.length; i++) {
	    Pattern pattern = (Pattern)MONTHS[i][0];
	    String monthStr = (String)MONTHS[i][1];
	    Matcher monthMatcher = pattern.matcher( date );
	    if ( monthMatcher.find() ) {
		    dateParts.add( "month=" + monthStr );
	    }
    }
		StringBuffer dateBuffer = new StringBuffer();
		for (int i = 0; i < dateParts.size(); i++) {
			String part = (String)dateParts.get( i );
			dateBuffer.append( part );
			dateBuffer.append( "%%" );
		}
		String normalizedDate = null;
		if ( dateBuffer.length() > 0 ) {
			normalizedDate = dateBuffer.toString().substring( 0, dateBuffer.length() - 2 );
			return normalizedDate;
		}
		return "";
	}


	/**
	 *
	 */
	public static class DateParseException extends ForwardedException {
		public DateParseException(String msg) {
			super( msg );
		}

		public DateParseException(Throwable cause) {
			super( cause );
		}

		public DateParseException(String msg, Throwable cause) {
			super( msg, cause );
		}
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();

		try {
			BufferedReader bufferedReader = new BufferedReader( new FileReader( new File( args[0] ) ) );
			String nextLine = null;
			while ((nextLine = bufferedReader.readLine()) != null) {
				try {
					String parsedDate = parseDate( nextLine );
					log.info( parsedDate + " <=              " + nextLine );
				}
				catch (DateParseException e) {
					log.error( e.getMessage() );
				}
			}
		}
		catch (FileNotFoundException e) {
			log.error( e.getMessage() );
		}
		catch (IOException e) {
			log.error( e.getMessage() );
		}
	}

}
