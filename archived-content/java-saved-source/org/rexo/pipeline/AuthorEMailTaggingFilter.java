package org.rexo.pipeline;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.pipeline.components.RxFilter;
import org.rexo.pipeline.components.RxPipeline;
import org.rexo.store.MetaDataXMLDocument;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Vector;


import java.util.regex.*;

// needed for findXMLElement
import java.util.Iterator;
import org.jdom.filter.Filter;

/** Author: saunders Created Apr 19, 2006 Copyright (C) 
    Univ. of Massachusetts Amherst, Computer Science Dept. */

public class AuthorEMailTaggingFilter implements RxFilter {

    private static Logger log = Logger.getLogger( AuthorEMailTaggingFilter.class );

    public class Author {
	public String first;
	public String middle;
	public String last;
	public int idx;
	public Element elm;
	
	public Author(Element elm, String first, String middle, String last, int idx){
	    this.elm   = elm;
	    this.first = first;
	    this.middle = middle;
	    this.last = last;
	    this.idx = idx;
	}

	public String str(){
	    return first + " " + middle + " " + last;
	}
    }
    
    public class Email {
	public String name;
	public String domain;
	public int idx;

	public Email(String name, String domain, int idx){
	    this.name = name;
	    this.domain = domain;
	    this.idx = idx;
	}

	public String str(){
	    return name + domain;
	}
    }

    public class AuthorEmailMatch{
	Author author;
	Email  email;
	String str_match_type;
	String order_match_type;

	public AuthorEmailMatch(Author author, Email email, String str_match_type, String order_match_type){
	    this.author = author;
	    this.email  = email;
	    this.str_match_type = str_match_type;
	    this.order_match_type = order_match_type;
	}
    }

    String tld_regex;

    public AuthorEMailTaggingFilter(){
	tld_regex = makeRegexOr(tlds);
    }

    public int init(RxPipeline pipeline) {
	return ReturnCode.OK;
    }

    Vector authors;
    Vector emails;
    Vector matches;

    public int accept(RxDocument rdoc) {
	log.info( "AuthorEMailTaggingFilter: " + rdoc.getId() );
	
	SAXBuilder saxBuilder = new SAXBuilder();
	Document xmlDocument;
	byte[] inputBytes = (byte[])rdoc.getScope( "document" ).get( "docBytes" );

	authors = new Vector();
	emails  = new Vector();
	matches = new Vector();

	try {
	    ByteArrayInputStream inputStream = new ByteArrayInputStream( inputBytes );
	    xmlDocument = saxBuilder.build( inputStream );
	    MetaDataXMLDocument metaDoc = new MetaDataXMLDocument( xmlDocument );
	    String bodyText = metaDoc.getBodyText();
	    
	    //	    XMLOutputter output = new XMLOutputter( Format.getPrettyFormat() );
	    
	    authors = findAuthors(xmlDocument);

	    emails = findEmails(xmlDocument);

	    matches = matchAuthorsEmails(authors, emails);

	    System.out.println("\n\n\n\n\n\n");
	}
	catch (Exception e) {
	    log.error( "exception: " + e.getMessage() );
	    return ReturnCode.ABORT_PAPER;
	}

	return ReturnCode.OK;
    }


    public Vector matchAuthorsEmails(Vector authors, Vector emails){
	Vector matches = new Vector();

	for (int an = 0; an < authors.size(); an++){
	    Author author = (Author) authors.get(an);
	    //	    System.out.println("AUTHOR : " +author.str());
	}

	for (int en = 0; en < emails.size(); en++){
	    Email email = (Email) emails.get(en);
	    //	    System.out.println("EMAIL : " +email.str());
	}


	for (int e = 0; e < emails.size(); e++){
	    Email email = (Email) emails.get(e);
	    
	    for (int a = 0 ; a < authors.size(); a++){
		Author author = (Author) authors.get(a);
		
		String str_match = matchNameEmail(author, email);
		
		String order_match = "";
		if (e == a){
		    order_match = "SAME_POSITION";
		}

		if (!str_match.equals("")){
		    System.out.println(str_match+" "+order_match+"\t"+author.str()+ "\t" + email.str());
		    // XMLOutputter output = new XMLOutputter( Format.getPrettyFormat() );
		    
		}
	    }
	}

	return matches;
    }



    public Vector findAuthors(Document xmlDocument){

	Vector authors = new Vector();

	Vector headers_elms = findXMLElements(xmlDocument, "headers");
	Element header = (Element) headers_elms.get(0);
	
	// find authors
	Vector author_elms = findXMLElements(header, "author");
	Iterator iter = author_elms.iterator();
	
	while(iter.hasNext()){
	    Element author_elm = (Element) iter.next();

	    Author author = new Author(author_elm,
				       cleanWord(childText(author_elm, "author-first")),
				       cleanWord(childText(author_elm, "author-middle")),
				       cleanWord(childText(author_elm, "author-last")),
				       authors.size());
	    
	    authors.add(author);
	}

	return authors;
    }

    public Vector findEmails(Document xmlDocument){
	Vector emails = new Vector();

	// cleanup emails and prepare for match
	Vector email_elms = findXMLElements(xmlDocument, "email");
	Iterator iter = email_elms.iterator();
	
	while(iter.hasNext()){
	    Element xml = (Element) iter.next();
	    
	    String noisy_email = xml.getText();
	    System.out.println("EMAIL STRING: " + noisy_email);

	    while(!noisy_email.equals("")){
		noisy_email = segmentEmail(emails,noisy_email);
	    }
	}

	return emails;
    }		
    

    public String segmentEmail(Vector emails, String working){
	Pattern p;
	Matcher m;

	// first strip extraneous words at beginning of email string

	p = Pattern.compile(".*e?-?mails?\\W+", Pattern.CASE_INSENSITIVE);
	m = p.matcher(working);
	working =  m.replaceFirst("");

	// find domain and remove
	// BUG: somtimes only retrieving ".co" instead of ".com"
	p = Pattern.compile("(.*?)(@([\\w-]+\\.)+"+tld_regex+")(\\W+|$)", 
				    Pattern.CASE_INSENSITIVE);
 	m = p.matcher(working);

	if (!m.find()){
	    return "";
	}

 	String remainder = m.replaceFirst("");

	String names  = m.group(1);
 	String domain = m.group(2);

	processEmailChunk(emails, names, domain);
	
	return remainder;
    }

    
    public String makeNameRegex(String str){
	String first_char = getFirstChar(str,"");
	if (first_char.equals("")){
	    return "("+str+")";
	}
	else {
	    return "(("+str+")|("+first_char+"))";
	}
    }

    public boolean exactMatch(String first, String middle, String last, 
			      String email_name){


	String first_regex  = makeNameRegex(first);
	String middle_regex = makeNameRegex(middle);
	String last_regex   = makeNameRegex(last);

	String exact_regex;

	if (middle.equals("")){

	    String single_regex = "("+first_regex+"|" + last_regex + ")";
	    String first_order = "("+first_regex+"\\W?"+last_regex+")";
	    String last_order = "(" + last_regex + first_regex + ")";
	    
	    exact_regex = "(("+single_regex+")|("+first_order+")|("+last_order+"))";
	}
	else {
	    String single_regex = "("+first_regex+"|" + middle_regex + "|" + last_regex + ")";
	    String first_order = "("+first_regex+"(\\W?" + middle_regex +")?(\\W?" + last_regex+")?)";
	    String last_order = "(" + last_regex + first_regex + middle_regex +"?)";
	    
	    exact_regex = "(("+single_regex+")|("+first_order+")|("+last_order+"))";
	}
	
	Pattern p = Pattern.compile(exact_regex);
	Matcher m = p.matcher(email_name);


	if (m.matches()){
	    return true;
 	}
	else {
	    return false;
	} 
    }

    public boolean middleGuessMatch(String first, String middle, String last, String email_name){
	if (middle.equals("")){
	    String first_regex  = makeNameRegex(first);
	    String last_regex   = makeNameRegex(last);

	    String first_order = "("+first_regex+"\\W?.\\W?"+last_regex+")";
	    String last_order = "(" + last_regex + first_regex + ".)";

	    String exact_regex = "(("+first_order+")|("+last_order+"))";

	    Pattern p = Pattern.compile(exact_regex);
	    Matcher m = p.matcher(email_name);

	    if (m.matches()){
		return true;
	    }
	}

	return false;
    }

    public boolean nameSubstringMatch(String first, String middle, String last, String email_name){
	String regex = new String("");
	
	if (first.length() > 1){
	    regex = "("+first+")";
	}
	
	if (middle.length() > 1){
	    if (!regex.equals("")){
		regex += "|";
	    }

	    regex += "("+middle+")";
	}

	if (last.length() > 1){
	    if (!regex.equals("")){
		regex += "|";
	    }

	    regex += "("+last+")";
	}
	
	Pattern p = Pattern.compile(regex);
	Matcher m = p.matcher(email_name);

	return m.find();
    }

    // NOTE : could consider instead weighting end-of-string email insertions less?
    public int levenMatch(String first, String middle, String last, String email_name){

	int min = getLevenshteinDistance(first+middle+last, email_name);

	min = Math.min(min, getLevenshteinDistance(last+first+middle, email_name));
	min = Math.min(min, getLevenshteinDistance(first, email_name));
	min = Math.min(min, getLevenshteinDistance(last, email_name));

	// also try full name + 1-letter name combinations
	if (first.length() > 0){
	    min = Math.min (min,getLevenshteinDistance(first.substring(0,1)+last,email_name));
	    min = Math.min (min,getLevenshteinDistance(last+first.substring(0,1),email_name));
	}

	if (last.length() > 0){
	    min = Math.min (min,getLevenshteinDistance(first+last.substring(0,1),email_name));
	    min = Math.min (min,getLevenshteinDistance(last.substring(0,1)+first,email_name));
	}

	return min;
    }


    // TOP LEVEL STRING MATCHING ROUTINE

    public String matchNameEmail(Author author,  Email email){
	return matchNameEmail(author, email.name);
    }

    public String matchNameEmail(Author author, String name){
	String first  = author.first.toLowerCase();
	String middle = author.middle.toLowerCase();
	String last   = author.last.toLowerCase(); 
	String email  = name.toLowerCase();

	if (exactMatch(first,  middle, last, email)){
	    //	    System.out.println("EXACT_MATCH:"+name+" "+author.str());
	    return "EXACT";
	}
	else if (middleGuessMatch(first, middle, last, email)){
	    return "MIDDLE_GUESS";
	}	
	else if (nameSubstringMatch(first, middle, last, email)){
	    return "NAME_SUBSTRING_MATCH";
	}
	else {
	    int distance = levenMatch(first, middle, last, email);
	    if (distance < 4) {
		return "LEVEN"+distance;
	    }
	}

	return "";
    }

    public String[] splitNames(String names){
	Pattern p = Pattern.compile("[,|;]");
	Matcher m = p.matcher(names);
	names = m.replaceAll(" ");
	
	names.replaceAll("^ +","");
	names.replaceAll(" +$","");

	return names.split(" +");
    }

    public int numExactMatches(String names){

	String[] people = splitNames(names);

	int num_matches = 0;
	
	for (int i = 0; i < people.length; i++){
	    if (!people[i].equals("")){

		for (int a = 0; a < authors.size(); a++){
		    String match = matchNameEmail((Author) authors.get(a), people[i]);
		    if (match.equals("EXACT")){
			num_matches++;
		    }
		}
	    }
	}

	return num_matches;
    }

    public void processEmailChunk(Vector emails, String names, String domain){

	// string outer parens/brackets
	names = names.replaceAll("\\{","");
	names = names.replaceAll("\\}","");
	names = names.replaceAll("\\(","");
	names = names.replaceAll("\\)","");
	names = names.replaceAll("\\[","");
  	names = names.replaceAll("\\]","");
  	names = names.replaceAll("#","");
	names = names.replaceAll("<","");

	// simple correction of conversion errors
	if (names.matches("1.*2") ||
	    names.matches("f.*[ ,].*g")) {

	    int matches_no_sub = numExactMatches(names);
	    
	    String sub = names.substring(1,names.length() - 1);

	    int matches_sub = numExactMatches(sub);

	    if (matches_sub > matches_no_sub){
		names = sub;
	    }
	}

	String[] people = splitNames(names);

	for (int i = 0; i < people.length; i++){
	    if (!people[i].equals("")){
		Email email = new Email(people[i],domain, emails.size());
		emails.add(email);
	    }
	}
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    // string convenience routines

    public String getFirstChar(String str, String def){
	if (str.length() > 0){
	    return str.substring(0,1);
	}
	else {
	    return def;
	}
    }

    public String cleanWord(String noisy){
	return noisy.replaceAll("\\W","");
    }


    // XML convenience routines

    public class XMLFilter implements Filter {
	private String _element_name;

	public XMLFilter(String element_name){
	    _element_name = element_name;
	}

	public boolean matches(Object o) {
	    if (o instanceof Element) {
		Element element = (Element) o;
		if (element.getName().equals( _element_name )) {
		    return true;
		}
	    }
	    return false;
	}
    };

    
    public Vector findXMLElements(Element element, String element_name) {
	Vector elements = new Vector();
	
	Iterator descendants = element.getDescendants( new XMLFilter(element_name));

	while(descendants.hasNext()){
	    Element xml_elm = (Element) descendants.next();
	    elements.add(xml_elm);
	}

	return elements;
    }

    public Vector findXMLElements(  Document doc, String element_name){
	return findXMLElements(doc.getRootElement(), element_name);
    }

    public String childText(Element elm, String child_name){
	if ( elm.getChild(child_name) != null){
	    return elm.getChild(child_name).getText();
	}
	else {
	    return new String("");
	}
    }

    // REGEX routines

    public String makeRegexOr( String[] strings){
	String regex = new String("");
	for (int i = 0; i < (strings.length - 1); i++){
	    regex +=  "(" + strings[i] + ")" + "|";
	}
	regex += "(" + strings[strings.length -1] + ")";

	regex = "(" + regex + ")";

	return regex;
    }


    public static String[] tlds = {
	"AC", "AD", "AE", "AERO", "AF", "AG", "AI", "AL", "AM", "AN","AO", 
	"AQ", "AR", "ARPA", "AS", "AT", "AU", "AW", "AZ", "BA","BB", "BD", "BE", "BF",
	"BG", "BH", "BI", "BIZ", "BJ", "BM", "BN", "BO", "BR","BS","BT", "BV", "BW",
	"BY","BZ","CA","CAT","CC","CD","CF","CG","CH","CI","CK","CL","CM","CN","CO",
	"COM","COOP","CR","CU","CV","CX","CY","CZ","DE","DJ","DK","DM","DO","DZ","EC",
	"EDU","EE","EG","ER","ES","ET","EU","FI","FJ","FK","FM","FO","FR","GA","GB",
	"GD","GE","GF","GG","GH","GI","GL","GM","GN","GOV","GP","GQ","GR","GS","GT",
	"GU","GW","GY","HK","HM","HN","HR","HT","HU","ID","IE","IL","IM","IN","INFO",
	"INT","IO","IQ","IR","IS","IT","JE","JM","JO","JOBS","JP","KE","KG","KH","KI",
	"KM","KN","KR","KW","KY","KZ","LA","LB","LC","LI","LK","LR","LS","LT","LU",
	"LV","LY","MA","MC","MD","MG","MH","MIL","MK","ML","MM","MN","MO","MOBI","MP",
	"MQ","MR","MS","MT","MU","MUSEUM","MV","MW","MX","MY","MZ","NA","NAME","NC",
	"NE","NET","NF","NG","NI","NL","NO","NP","NR","NU","NZ","OM","ORG","PA","PE",
	"PF","PG","PH","PK","PL","PM","PN","PR","PRO","PS","PT","PW","PY","QA","RE",
	"RO","RU","RW","SA","SB","SC","SD","SE","SG","SH","SI","SJ","SK","SL","SM",
	"SN","SO","SR","ST","SU","SV","SY","SZ","TC","TD","TF","TG","TH","TJ","TK",
	"TL","TM","TN","TO","TP","TR","TRAVEL","TT","TV","TW","TZ","UA","UG","UK",
	"UM","US","UY","UZ","VA","VC","VE","VG","VI","VN","VU","WF","WS","YE","YT",
	"YU","ZA","ZM","ZW" };

    public static int getLevenshteinDistance (String s, String t) {
		
	/*
	  The difference between this impl. and the previous is that, rather 
	  than creating and retaining a matrix of size s.length()+1 by t.length()+1, 
	  we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
	  is the 'current working' distance array that maintains the newest distance cost
	  counts as we iterate through the characters of String s.  Each time we increment
	  the index of String t we are comparing, d is copied to p, the second int[].  Doing so
	  allows us to retain the previous cost counts as required by the algorithm (taking 
	  the minimum of the cost count to the left, up one, and diagonally up and to the left
	  of the current cost count being calculated).  (Note that the arrays aren't really 
	  copied anymore, just switched...this is clearly much better than cloning an array 
	  or doing a System.arraycopy() each time  through the outer loop.)
	  
	  Effectively, the difference between the two implementations is this one does not 
	  cause an out of memory condition when calculating the LD over two very large strings.  		
	*/		
		
	int n = s.length(); // length of s
	int m = t.length(); // length of t
	
	if (n == 0) {
	    return m;
	} else if (m == 0) {
	    return n;
	}
	
	int p[] = new int[n+1]; //'previous' cost array, horizontally
	int d[] = new int[n+1]; // cost array, horizontally
	int _d[]; //placeholder to assist in swapping p and d
	
	// indexes into strings s and t
	int i; // iterates through s
	int j; // iterates through t
	
	char t_j; // jth character of t

	int cost; // cost
	
	for (i = 0; i<=n; i++) {
	    p[i] = i;
	}
	
	for (j = 1; j<=m; j++) {
	    t_j = t.charAt(j-1);
	    d[0] = j;
	    
	    for (i=1; i<=n; i++) {
		// NOTE : set substitution to 2
		cost = s.charAt(i-1)==t_j ? 0 : 2;
		// minimum of cell to the left+1, to the top+1, diagonally left and up +cost
		d[i] = Math.min( Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);  
	    }
	    
	    // copy current distance counts to 'previous row' distance counts
	    _d = p;
	    p = d;
	    d = _d;
	} 
	
	// our last action in the above loop was to switch d and p, so p now 
	// actually has the most recent cost counts
	return p[n];
    }

}
