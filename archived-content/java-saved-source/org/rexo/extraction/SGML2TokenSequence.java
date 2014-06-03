/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/**
	 Converts a string containing simple SGML tags into a dta TokenSequence of words,
	 paired with a target TokenSequence containing the SGML tags in effect for each word.

	 It does not handle nested SGML tags, nor gracefully handle malformed SGML.
	 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package org.rexo.extraction;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.pipe.Input2CharSequence;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;

public class SGML2TokenSequence extends Pipe implements Serializable
{
	String sgmlRegex = "</?([^>]*)>";
	Pattern sgmlPattern = Pattern.compile(sgmlRegex);
//	Pattern sgmlPattern = Pattern.compile ("</?([^>]*)>");

	CharSequenceLexer lexer;
	String lexerRegex;
	String backgroundTag;
	
	public SGML2TokenSequence (CharSequenceLexer lexer, String backgroundTag)
	{
		this.lexer = lexer;
		this.backgroundTag = backgroundTag;
		this.lexerRegex = lexer.getPattern();
	}

	public SGML2TokenSequence (String regex, String backgroundTag)
	{
		this.lexer = new CharSequenceLexer (regex);
		this.backgroundTag = backgroundTag;
	}

	public SGML2TokenSequence ()
	{
		this (new CharSequenceLexer(), "O");
	}

	public Instance pipe (Instance carrier)
	{
		TokenSequence dataTokens = new TokenSequence ();
		TokenSequence targetTokens = new TokenSequence ();
		CharSequence string = (CharSequence) carrier.getData();
		String tag = backgroundTag;
		String nextTag = backgroundTag;
		Matcher m = sgmlPattern.matcher (string);
		
		int textStart = 0;
		int textEnd = 0;
		int nextStart = 0;
		boolean done = false;

//		System.out.println(sgmlPattern.pattern());
//		System.out.println(lexer.getPattern());
//		System.out.println(backgroundTag);
//		System.out.println(string);


		while (!done) {

			done = !(m.find());
			if (done)
				textEnd = string.length()-1;
			else {
				String sgml = m.group();
//				System.out.println ("SGML = "+sgml);

				int groupCount = m.groupCount();
//				System.out.println(groupCount);

				if (sgml.charAt(1) == '/')
					nextTag = backgroundTag;
				else{
					nextTag = m.group(1);
				}
//				System.out.println("nextTag: " + nextTag);

				nextStart = m.end()+1;
				textEnd = m.start()-1;
//				System.out.println ("Text start/end "+textStart+" "+textEnd);
			}

//			if(tag.equals("abstract") || tag.equals("intro")) continue;

			if (textEnd - textStart > 0) {
//				System.out.println ("Tag = "+tag);
//				System.out.println ("Target = "+string.subSequence (textStart, textEnd));
				lexer.setCharSequence (string.subSequence (textStart, textEnd));
				while (lexer.hasNext()) {
					dataTokens.add (new Token ((String) lexer.next()));
					targetTokens.add (new Token (tag));
				}
			}
			textStart = nextStart;
			tag = nextTag;
		}

		carrier.setData(dataTokens);
		carrier.setTarget(targetTokens);
		carrier.setSource(dataTokens);

		return carrier;
	}

	public static void main (String[] args)
	{
		try {
			Pipe p = new SerialPipes (new Pipe[] {
				new Input2CharSequence (),
				new SGML2TokenSequence(new CharSequenceLexer (Pattern.compile ("\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}")), "O")
//				new SGML2TokenSequence (new CharSequenceLexer (Pattern.compile (".")), "O")
				});
			
			for (int i = 0; i < args.length; i++) {
				Instance carrier = new Instance (new File(args[i]), null, null, null, p);
				TokenSequence data = (TokenSequence) carrier.getData();
				TokenSequence target = (TokenSequence) carrier.getTarget();
				System.out.println ("===");
				System.out.println (args[i]);
				for (int j = 0; j < data.size(); j++)
					System.out.println (target.getToken(j).getText()+" "+data.getToken(j).getText());
			}
		} catch (Exception e) {
			System.out.println (e);
			e.printStackTrace();
		}
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);

//		out.writeObject(sgmlPattern);
		out.writeObject(sgmlRegex);

		out.writeObject(backgroundTag);

	//	out.writeObject(lexer);
		out.writeObject(lexerRegex);

//		System.out.println("lexRegex:" + lexerRegex);

	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
//		sgmlPattern = (Pattern) in.readObject();
		sgmlRegex = (String)in.readObject();
		sgmlPattern = Pattern.compile(sgmlRegex);

		backgroundTag = (String) in.readObject();

		lexerRegex = (String)in.readObject();
//		System.out.println("lexRegex:" + lexerRegex);

		lexer = new CharSequenceLexer(lexerRegex);	

	}
}
