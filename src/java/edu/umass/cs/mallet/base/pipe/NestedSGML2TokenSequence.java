/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */





package edu.umass.cs.mallet.base.pipe;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;
import edu.umass.cs.mallet.base.util.MalletLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
         Pairs each token with all of its respective SGML tags; nested SGML tags are handled properly.
	 The constructor takes a parameter maxTags which is the maximum number of nester tags for each
	 token (0 means all tags are included). The token is placed in the data TokenSequence and corresponding 
         tags are placed in the target TokenSequence delimited by "!!" (default) or a string passed in to 
         the constructor.In order to save space, if more than one token in a row share the same set of sgml 
         tags, the first token's target will contain the sgml tag and all tokens that follow immediately and
	 share those tags will have a blank target (empty string).

         Malformed SGML may be a problem. Watch out for the <script> </script> section of 
         an HTML document as the comparators <,>,<=, or >= may throw it off. (Try
         string.toLowerCase().replaceAll("<script.*</script>","") to eliminate the scripts from the html)


   @author Michael Wick <a href="mailto:mwick@student.umass.edu">mwick@student.umass.edu</a>
 */
public class NestedSGML2TokenSequence extends Pipe implements Serializable
{
    private static Logger logger = MalletLogger.getLogger (NestedSGML2TokenSequence.class.getName());
	Pattern sgmlPattern = Pattern.compile ("<(/?[^>]*)>");
	CharSequenceLexer lexer;
	String backgroundTag;

    String delimiter="!!";
    int maxTags;
	
	public NestedSGML2TokenSequence (CharSequenceLexer lexer, String backgroundTag)
	{
		this.lexer = lexer;
		this.backgroundTag = backgroundTag;
	}

	public NestedSGML2TokenSequence (String regex, String backgroundTag)
	{
		this.lexer = new CharSequenceLexer (regex);
		this.backgroundTag = backgroundTag;
	}

	public NestedSGML2TokenSequence (int maxTags)
	{
		this (new CharSequenceLexer(), "O");
		this.maxTags=maxTags;
	}

	public NestedSGML2TokenSequence (CharSequenceLexer lexer, String backgroundTag, String delimiter)
	{
		this.lexer = lexer;
		this.backgroundTag = backgroundTag;
		this.delimiter=delimiter;
	}

	public NestedSGML2TokenSequence (String regex, String backgroundTag, String delimiter)
	{
		this.lexer = new CharSequenceLexer (regex);
		this.backgroundTag = backgroundTag;
		this.delimiter=delimiter;
	}

	public NestedSGML2TokenSequence (String delimiter, int maxTags)
	{
		this (new CharSequenceLexer(), "O");
		this.delimiter = delimiter;
		this.maxTags=maxTags;
	}


	public Instance pipe (Instance carrier)
	{
	        boolean STRIP_INSIDES=false; //this will save memory if set to true, but we sacrifice valuable information

		TokenSequence dataTokens = new TokenSequence();
		TokenSequence targetTokens = new TokenSequence();
		String string = (String) carrier.getData();
		Matcher m = sgmlPattern.matcher (string);
		boolean done = false;
		String contentNoTags = string.replaceAll("<(/?[^>]*)>","");

		logger.fine(sgmlPattern.pattern());
		logger.finer(string);
		
		int idxOffset=0;
		ArrayList tagList = new ArrayList();
		ArrayList loneTags = new ArrayList();
		while(!done)
		    {
			done=!(m.find());
			if(!done)
			    {
				//find opening tag
				String sgml=m.group(); //.replaceAll("\n"," ")???
				if(sgml.charAt(1)!='/')
				    {
					//gut the tag? "<example name='blah'>" becomes--> "<example>"
					if(STRIP_INSIDES)
					    {
						int siidx=sgml.indexOf(" ");
						if(siidx!=-1)
						    sgml=sgml.substring(0,siidx)+">";
					    }
					//find closing tag
					String closer=new String(sgml);
					int tidx=sgml.indexOf(" ");
					if(tidx!=-1)
					    closer = closer.substring(0,tidx)+">";
					closer="</"+closer.substring(1,closer.length());
					int closeIdx=string.indexOf(closer,m.end());
					if(closeIdx!=-1)
					    {
						ArrayList tagInfo = new ArrayList();
						int btwnOffset=calcOffset(string.substring(m.end(),closeIdx));
						
						tagInfo.add(sgml.substring(1,sgml.length()-1));
						tagInfo.add(new Integer(m.start()-idxOffset));
						idxOffset+=sgml.length();
						tagInfo.add(new Integer(closeIdx-idxOffset-btwnOffset+1));

						tagList.add(tagInfo);
					    }else //e.g., <p> has no closer
						{
						    loneTags.add(sgml);
						    idxOffset+=sgml.length();
						}
				    }else
					idxOffset+=sgml.length();
			    }
		    }

		lexer.setCharSequence(contentNoTags);
		String oldResult=""+delimiter;
		while(lexer.hasNext())
		    {
			String token = (String)lexer.next();
			int tokenIdx = lexer.getStartOffset();

			String result=""+delimiter;

			int numIts=maxTags;
			if(maxTags==0)numIts=-1;
			for(int i=tagList.size()-1;i>=0;i--)
			    {
				if(i<0 || numIts<=0)break;
				ArrayList tmp = (ArrayList)tagList.get(i);
				String tag = (String)tmp.get(0);
				int sIdx = ((Integer)tmp.get(1)).intValue();
				int eIdx = ((Integer)tmp.get(2)).intValue();
				
				if(tokenIdx>=sIdx && tokenIdx<eIdx)
				    {result+=tag+delimiter;numIts--;}
			    }	

			/*
			for(int i=0;i<tagList.size();i++)
			    {
				ArrayList tmp = (ArrayList)tagList.get(i);
				String tag = (String)tmp.get(0);
				int sIdx = ((Integer)tmp.get(1)).intValue();
				int eIdx = ((Integer)tmp.get(2)).intValue();

				if(tokenIdx<sIdx)break; //don't even waste our time, it's in order

				if(tokenIdx>=sIdx && tokenIdx<eIdx)
				    result+=tag+delimiter;
			    }
			*/
			
			if(oldResult.equals(result))result="";
			dataTokens.add(new Token(token));
			targetTokens.add(new Token(result));
			if(result.length()>0)oldResult=""+result;

		    }
		for(int i=0;i<loneTags.size();i++)
		    {
			dataTokens.add(new Token("^lonetag"));
			targetTokens.add(new Token("!!"+(String)loneTags.get(i)+"!!"));	
		    }
		carrier.setData(dataTokens);
		carrier.setTarget(targetTokens);
		carrier.setSource(null);
		return carrier;
	}
    //
    //SERIALIZATION
    //
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException 
        {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(sgmlPattern);
		out.writeObject(lexer);
		out.writeObject(backgroundTag);
		out.writeObject(delimiter);
		out.writeObject(new Integer(maxTags));
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException 
        {
		int version = in.readInt ();
		sgmlPattern = (Pattern) in.readObject();
		lexer = (CharSequenceLexer) in.readObject();
		backgroundTag = (String) in.readObject();
		delimiter = (String) in.readObject();
		maxTags = ((Integer) in.readObject()).intValue();
	}

    private int calcOffset(String region)
    {
	Pattern sgmlp = Pattern.compile ("<(/?[^>]*)>");	
	Matcher m = sgmlp.matcher (region);
	boolean done=false;
	int result=0;
	while(!done)
	    {
		done=!(m.find());
		if(!done)
		    result+=m.group().length();
	    }
	return result;
    }
}
