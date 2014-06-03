


package edu.umass.cs.mallet.base.pipe;

import edu.umass.cs.mallet.base.types.Alphabet;
import edu.umass.cs.mallet.base.types.FeatureSequence;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.TokenSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class RevisedHomepage2FeatureSequence extends Pipe
{
    private HashMap hpMap;
    private String authorName;
    private String authorEmail;

    public void setAuthor(String authorName)
    {
	this.authorName=authorName;
    }
    public void setEmail(String authorEmail)
    {
	this.authorEmail = authorEmail;
    }


    public RevisedHomepage2FeatureSequence(String authorName, String authorEmail, HashMap hpMap)
    {
	super(Alphabet.class, null);
	this.authorName=authorName;
	this.authorEmail=authorEmail;
	this.hpMap=hpMap;
    }

    public Instance pipe (Instance carrier)
    {	
	TokenSequence tsData = (TokenSequence)carrier.getData();
	TokenSequence tsTarget= (TokenSequence)carrier.getTarget();
	ArrayList tmpStore = new ArrayList();
	String file_name = carrier.getName().toString();
	if(file_name.length()>5)file_name=file_name.substring(5,file_name.length());	
	
	String author_name=authorName;
	String email = authorEmail;
	String username = authorEmail.replaceAll("@.*","");

	String oldToken="blah";
	if(tsData.size()>0)
	    oldToken=tsTarget.getToken(0).getText().toLowerCase();
	for(int i=0;i<tsData.size();i++)
	    {
		String dataToken = tsData.getToken(i).getText();
		String targetToken = tsTarget.getToken(i).getText().toLowerCase();
		if(targetToken.length()==0)
		    targetToken=oldToken;
		else
		   oldToken=targetToken;
		//
		//publication?
		if(dataToken.indexOf("publication")!=-1
		   || dataToken.indexOf("paper")!=-1
		       || dataToken.indexOf("pubs")!=-1)		
		    {
			if(targetToken.indexOf("href")!=-1
			    || isHeader(targetToken))
			    {
				tmpStore.add("has_publication_link");
				tmpStore.add("has_publication");
			    }
			if(isHeader(targetToken))
			    tmpStore.add("has_publication");	   

		    }
		//
		//research?
		if(dataToken.indexOf("research")!=-1
		   ||dataToken.indexOf("group")!=-1
		   ||dataToken.indexOf("interest")!=-1
		   ||dataToken.indexOf("project")!=-1)
		    {
			if(targetToken.indexOf("href")!=-1
			   || isHeader(targetToken))
			    {
				tmpStore.add("has_research_link");
				tmpStore.add("has_research");
			    }
			if(isHeader(targetToken))
			    tmpStore.add("has_research");
		    }
		//
		//resume?
		if(dataToken.indexOf("resume")!=-1
		   ||dataToken.indexOf("vita")!=-1
		   ||dataToken.equals("cv"))
		    {
			if(targetToken.indexOf("href")!=-1
			   || isHeader(targetToken))
			    {
				tmpStore.add("has_resume_link");
				tmpStore.add("has_resume");
			    }
			if(isHeader(targetToken))
			    tmpStore.add("has_resume");
		    }
		//
		//Author/Email features
		if(dataToken.indexOf("genericauthorofhomepage")!=-1
		   || dataToken.indexOf("genericauthoremail")!=-1
		   || dataToken.indexOf("firstgenericauthorofhomepage")!=-1
		   || dataToken.indexOf("lastgenericauthorofhomepage")!=-1)
		    {
			if(targetToken.indexOf("title!!")!=-1 || isHeader(targetToken))
			    {
				if(targetToken.indexOf("title!!")!=-1)
				    tmpStore.add("title " + dataToken);
				else
				    tmpStore.add("h " + dataToken);				   
			    }

			if(dataToken.indexOf("genericauthoremail")!=-1)
			    tmpStore.add("contains email");
		    }
		if(targetToken.indexOf("!!<img")!=-1)
		    {
			String alt="";
			int closeIdx;
			int openIdx=targetToken.indexOf("alt");
			if(openIdx!=-1)
			    {
				openIdx=targetToken.indexOf("\"",openIdx);
				closeIdx=targetToken.indexOf("\"",openIdx+1);
				if(openIdx!=-1 && closeIdx!=-1)
				    alt=targetToken.substring(openIdx,closeIdx);
			    }
			String src="";
			openIdx=targetToken.indexOf("src");
			if(openIdx!=-1)
			    {
				openIdx=targetToken.indexOf("\"",openIdx);
				closeIdx=targetToken.indexOf("\"",openIdx+1);
				if(openIdx!=-1 && closeIdx!=-1)
				    src=targetToken.substring(openIdx,closeIdx);
			    }


			if(alt.indexOf("genericauthorofhomepage")!=-1
			   ||alt.indexOf(username)!=-1 )
			    {
				tmpStore.add("h genericauthorofhomepage");
				tmpStore.add("img_alt_has_author");
				tmpStore.add("img_has_author");
			    }

			if(src.indexOf("genericauthorofhomepage")!=-1
			   ||src.indexOf(username)!=-1)
			    {
				tmpStore.add("img_has_author");
			    }
			
			if(alt.indexOf("email")!=-1)
			    {
				//System.out.println("alt has image of email: " + alt);
			    }
			if(src.indexOf("email")!=-1)
			    {
				//System.out.println("src has image of email: " + src);
			    }
		    }
		//
		//Contact Features
		if(dataToken.equals("phone")
		   || dataToken.equals("telephone")
		   || dataToken.equals("address")
		   || dataToken.equals("contact")
		   || dataToken.equals("office")
		   || dataToken.equals("tel")
		   || dataToken.equals("fax"))
		    tmpStore.add("contactinfo");
		//
		//Title Features
		if(targetToken.indexOf("!!title!!")!=-1)
		    {
			if(dataToken.equals("resume")
			   ||dataToken.indexOf("vita")==0
			   ||dataToken.indexOf("bio")==0
			   ||dataToken.indexOf("biblio")==0
			   ||dataToken.indexOf("paper")==0
			   ||dataToken.indexOf("cv")==0
			   ||dataToken.indexOf("talk")==0
			   ||dataToken.indexOf("publication")==0
			   ||dataToken.indexOf("abstract")==0)
			    tmpStore.add("title_has_extra");
			if(dataToken.equals("homepage"))
			    tmpStore.add("title_has_homepage");
		    }
		//
		//Email
		if(dataToken.indexOf("genericauthoremail")!=-1);
		{
		    //System.out.println("email "+targetToken);
		}

	    }

	//
	//add components of filename as features
	StringTokenizer st = new StringTokenizer(file_name.replaceAll("/","_"),"_");
	boolean theend=false;
	int count=0;
	
	while(st.hasMoreTokens())
	    {
		String tmp = st.nextToken().toLowerCase();
		if(tmp.charAt(0)=='~')
		    {
			if(st.countTokens()==0 && (tmp.indexOf(username)!=-1 || authorName.indexOf(tmp.substring(1,tmp.length()))!=-1)) 
			    {
				tmpStore.add("possibleroot");
			    }
		    }
		
		//***** or if the next token equals .htm or .html
		if((st.countTokens()==0) 
		   && (tmp.indexOf(username)!=-1 || authorName.indexOf(tmp.substring(1,tmp.length()))!=-1)) 
		    {
			tmpStore.add("possibleroot");
		    }
	    }	
	//
	//format our features and return them
	FeatureSequence result =
	    new FeatureSequence((Alphabet)getDataAlphabet(), tmpStore.size());
	for(int i=0;i<tmpStore.size();i++)
	    result.add((String)tmpStore.get(i));
	carrier.setData(result);
	carrier.setSource(null);
	carrier.setTarget(null);
	if(hpMap!=null)
	    {
		if(isRealHomepage(file_name))
		    carrier.setTarget("yes");
		else
		    carrier.setTarget("no");
	    }
	return carrier;
    }


    private boolean isRealHomepage(String path)
    {
	return false;

	//if(hpMap==null)return false;
	//String homepage = path.replaceAll(".*/","");
	//if(homepage.length()>3)
	//    homepage=homepage.substring(3,homepage.length());
	//homepage=homepage.replaceAll("%23","#");
	//if(homepage.endsWith("#"))
	//    homepage=homepage.substring(0,homepage.length()-1);
	//if(hpMap.get(authorName).equals(homepage))
	//    {
	//	System.out.println("Author HP = " + homepage);
	//	return true;
	//    }
	//return false;
    }

    private boolean isHeader(String targetTokens)
    {
	StringTokenizer st = new StringTokenizer(targetTokens,"!!");
	while(st.hasMoreTokens())
	    {
		String targetToken = st.nextToken();
		if(targetToken.indexOf("h1")==0
		   ||targetToken.indexOf("h2")==0
		   ||targetToken.indexOf("h3")==0
		   ||targetToken.indexOf("meta")==0
		   //||targetToken.indexOf("o")==0
		   ||targetToken.equals("big")
		   ||targetToken.equals("strong"))
		    return true;
		
		//<font size=+1> or <font size="5"> etc..
		if(targetToken.length()>4 && targetToken.substring(0,4).equals("font"))
		    {
			String tmp = targetToken.replaceAll("[ \'\"]","");
			int idx = tmp.indexOf("size=")+5;
			if(idx-5!=-1 && idx+2<=tmp.length())
			    {
				String size = tmp.substring(idx,idx+2);
				try
				    {
					if(size.charAt(0)=='+' && (int)(size.charAt(1)-'0') > 0)
					    return true;
					else if(size.charAt(0)!='-')
					    size=size.substring(0,1);
					if(Integer.parseInt(size)>=3)
					    return true;
				    }
				catch 
				    (NumberFormatException e){return false;}
			    }
		    }
	    }	
	return false;
    }
}
