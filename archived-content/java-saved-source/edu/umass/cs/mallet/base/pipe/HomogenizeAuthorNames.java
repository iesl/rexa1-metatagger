/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/** 
   @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
   @author Michael Wick
 */

package edu.umass.cs.mallet.base.pipe;

import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;

import java.io.*;

/**
 * Pipe that turns the full author name into "GENERICAUTHOROFHOMEPAGE"
 * Takes a CharSequence and returns a CharSequence
 */

public class HomogenizeAuthorNames extends Pipe implements Serializable
{
	CharSequenceLexer lexer;
    //private HashMap _authors;
    //private HashMap _emails;
        
        String author;
        String authorEmail;

    public void setAuthor(String author)
    {
	this.author=author;
    }
    public void setEmail(String authorEmail)
    {
	this.authorEmail=authorEmail;
    }

	public HomogenizeAuthorNames (CharSequenceLexer lexer)
	{
		this.lexer = lexer;
	}

	public HomogenizeAuthorNames (String regex)
	{
		this.lexer = new CharSequenceLexer (regex);
	}

	public HomogenizeAuthorNames (String author, String authorEmail)
	{
		this (new CharSequenceLexer());
	        this.author=author;
		this.authorEmail = authorEmail;
	}

	public Instance pipe (Instance carrier)
	{
	    //CharSequence string = (CharSequence) carrier.getData();
	    String authorName=author;
	    String email = authorEmail;
	    String result = ((CharSequence)carrier.getData()).toString().toLowerCase();

	    //scripting languages seem to confuse the SGML parser
	    result=result.replaceAll("[\n\r]"," "); //. doesn't match newline, eliminate them
	    result=result.replaceAll("</script>","</script>\n"); //kinda a hack to stop expression after first </script>... but it works
	    result=result.replaceAll("<script.*</script>","");

	    /*
	    String file_name = carrier.getName().toString();
	    file_name=file_name.substring(5,file_name.length());
	    String rfile = new String(file_name);
                                                                                                    
	    int tidx = rfile.indexOf(file_name);
	    rfile=rfile.substring(0,tidx);

	    if(_authors.containsKey(file_name))
		authorName=(String)_authors.get(file_name);
	    if(_emails.containsKey(authorName))
		email=(String)_emails.get(authorName);
            */


	    //unfortunately too many non-homepages contain the author's email
	    /*
	    if(email.length()>0)
		{
		    String username="";
		    String domain="";
		    int atIdx=email.indexOf("@");
		    if(atIdx!=-1)
			username=email.substring(0,atIdx);
		    domain=email.substring(atIdx+1,email.length());
		    StringTokenizer st = new StringTokenizer(domain,".");
		    //String regex=username+"|name"+"(at|@) ?";
		    String regex="("+username.toLowerCase()+"|name) ?"+"(at|@) ?";
		    //regex="@cs ?(dot|\\.)umass\\.edu";
		    
		    while(st.hasMoreTokens())
		    {
			regex+=st.nextToken();
			if(st.countTokens()>0)
			    regex+=" ?(dot|\\.) ?";
		    }
		    
		    
		    Pattern emailPattern = Pattern.compile (regex);
		    Matcher m = emailPattern.matcher (result);
		    
		    while(m.find())
			{
			    String emailm = m.group();
			    //System.out.println("Name = " + author + ".  Email = " + authorEmail);
			    System.out.println("email = " +emailm);
			    //System.out.println("regex="+regex);
			}
		   
		    //result=result.replaceAll(regex,"genericauthoremail");//GENERICAUTHOREMAIL
		}
		/**/


	    authorName=authorName.replaceAll("_"," ").toLowerCase();
	    if(authorName.length()>0)
		//result=result.replaceAll(authorName," genericauthorofhomepage ");//GENERICAUTHOROFHOMEPAGE
		result=result.replaceAll(authorName," genericauthorofhomepage lastgenericauthorofhomepage firstgenericauthorofhomepage ");//GENERICAUTHOROFHOMEPAGE
	    String lName=authorName.replaceAll(".* ","");
	    //System.out.println("auth="+authorName);
	    if(lName.length()>0)
		result=result.replaceAll(lName," lastgenericauthorofhomepage ");
	    String fName=authorName.replaceAll(" .*","");
	    if(fName.length()>0)
		result=result.replaceAll(fName," firstgenericauthorofhomepage ");
	    //result=result.toLowerCase();
	    result=result.replaceAll("home page","homepage");



	    carrier.setData((CharSequence)result);
	    return carrier;
	}

	public static void main (String[] args)
	{
		try {
			for (int i = 0; i < args.length; i++) {
				Instance carrier = new Instance (new File(args[i]), null, null, null);
				Pipe p = new SerialPipes (new Pipe[] {
					new Input2CharSequence (),
					new CharSequence2TokenSequence(new CharSequenceLexer())});
				carrier = p.pipe (carrier);
				TokenSequence ts = (TokenSequence) carrier.getData();
				System.out.println ("===");
				System.out.println (args[i]);
				System.out.println (ts.toString());
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
		out.writeObject(lexer);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		lexer = (CharSequenceLexer) in.readObject();
	}


	
}
