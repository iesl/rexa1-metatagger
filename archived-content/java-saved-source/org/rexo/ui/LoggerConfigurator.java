/**
 * Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Created on May 27, 2005
 * author: saunders
 */

package org.rexo.ui;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LoggerConfigurator {

	/**
	 * create a useful default configuration for logging
	 */
	public static void configure(Class clazz) throws IOException {
		BasicConfigurator.resetConfiguration();
		Properties log4jProperties = new Properties();
		String className = clazz.getName();
		String classPackageName = clazz.getPackage().getName();
		String classUQName = className.substring( classPackageName.length() + 1 );

		InputStream stream = clazz.getResourceAsStream( classUQName + ".log4j.properties" );

		if (stream == null ) {
			stream = clazz.getResourceAsStream( "log4j.properties" );
		}
		if (stream == null) {
			stream = LoggerConfigurator.class.getResourceAsStream( "log4j.properties" );
		}
		
		log4jProperties.load( stream );
		if (log4jProperties.containsKey( "log4j.appender.R.File" )) {
			log4jProperties.remove( "log4j.appender.R.File" );
			log4jProperties.setProperty( "log4j.appender.R.File", classUQName + ".log" );
		}
		PropertyConfigurator.configure( log4jProperties );
	}
}
