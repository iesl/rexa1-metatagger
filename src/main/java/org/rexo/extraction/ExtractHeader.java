package org.rexo.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.util.RegexFileFilter;

public class ExtractHeader{
	String xmlTag = "<[^>]*>";
	String xmlTag_font = "<font[^>]*>";
	String xmlTag_nofont = "<[^font][^>]*/>";
	String xmlTag_nofont_noposition = "<[^font&&^position][^>]*>";
	String xmlStartTag = "<\\?xml [^<]*\\?>";
	String docTag = "<doc>|</doc>";
	String contentTag = "<content>|</content>";

	String headerBoundary  = "1. introduction";	
	String headerBoundary2 = "1 introduction";
	String headerBoundary3 = "introduction";
	String headerBoundary4 = "I. introduction";

	LinkedHashMap authorList = new LinkedHashMap();

	String ackStr = "acknowledgements";
	String contentStr = "contents";

	String abstractStr = "abstract";
	int len_limit = 1000;

	FileFilter filter = new RegexFileFilter(Pattern.compile(".*xml$"));

	ArrayList paperList = new ArrayList();

	public ExtractHeader()
	{
	}

	public String extractHeaderDir(File xmlFile, int flag) throws java.io.IOException, java.io.FileNotFoundException
	{
		String buffer = "";
		if(xmlFile.isDirectory()){
			File[] directoryContents = xmlFile.listFiles();
			for (int i = 0; i < directoryContents.length; i++) {

				if (directoryContents[i].isDirectory())
					buffer += extractHeaderDir(directoryContents[i], flag);
				else{
					if (filter != null && !filter.accept(directoryContents[i])) continue;
					buffer += extractHeaderFile(directoryContents[i], flag);
				}
			}
		}
                else if(xmlFile.isFile()){
                                buffer += extractHeaderFile(xmlFile, flag);
                }

		return buffer;
	}
	
	public ArrayList randomSelectPapers(int num)
	{
		java.util.Random r = new java.util.Random(System.currentTimeMillis());
	
		num = Math.min(num, paperList.size());
		System.out.println("generating " +  num + " random numbers");	
		LinkedHashSet numSet = new LinkedHashSet();
		while(numSet.size() < num){
			int current_num = r.nextInt(paperList.size());

			assert(current_num < paperList.size());

			numSet.add(new Integer(current_num));	
		}

		System.out.println("selecting papers");	

		ArrayList retList = new ArrayList();
		Iterator iter = numSet.iterator();
		while(iter.hasNext()){
			int current_num = ((Integer)iter.next()).intValue();
			retList.add(paperList.get(current_num));

			System.out.println(paperList.get(current_num));
		}	

		return retList;
	}

	public String string2Author(String str)
	{
		int index = str.indexOf("~");
		int index2 = -1;
		if(index == -1){
			index2 = str.lastIndexOf("\\");
		}
		else{
			index2 = str.indexOf("\\",index);
		}

		return str.substring(0, index2);
	}

	public boolean updateAuthorList(String author)
	{
		Integer numInteger = (Integer)authorList.get(author);

		int num = 0;
		if( numInteger != null){
			num = numInteger.intValue();
		}
		num ++;
		authorList.put(author, new Integer(num) );

//		System.out.println(num);
		if(num <=3 ) return true;
		else return false;
	}

	public String extractHeaderFile(File xmlFile, int flag) throws java.io.IOException, java.io.FileNotFoundException
	{

		String author = string2Author(xmlFile.toString());
		
		if(updateAuthorList(author) == false) return "";

		BufferedReader bf = new BufferedReader (new FileReader (xmlFile));

		String buffer = "<NEW_HEADER>\n";	
		buffer += "source: " + xmlFile.toString() + "\n";
		int maxLen = 0;
		boolean goodPaper = false;
		while(true){
			String line = bf.readLine();
			if(line == null){
				break;
			}

			String replaceXMLTag;
			if(flag == 0){//keep all xml tags
				replaceXMLTag = "";
			}
			else if(flag == 1){//only keep font tag, and remove all other tags
				replaceXMLTag = xmlTag_nofont;
			}
			else if(flag == 2){
				replaceXMLTag = xmlTag_nofont_noposition;
			}
			else{
				replaceXMLTag = xmlTag;
			}
	
			String xmlRemovedStr = line.replaceAll(replaceXMLTag, "");
			xmlRemovedStr = xmlRemovedStr.replaceAll(xmlStartTag, "");
			xmlRemovedStr = xmlRemovedStr.replaceAll(docTag,"");
			xmlRemovedStr = xmlRemovedStr.replaceAll(contentTag,"");

			if(xmlRemovedStr.equals("")){
				continue;
			}
			if(xmlRemovedStr.length() == 1 || xmlRemovedStr.equals("**") || xmlRemovedStr.equals("***")){
				continue;
			}

		
			String lowerstr = xmlRemovedStr.toLowerCase();
			lowerstr = lowerstr.replaceAll(xmlTag,"");
			if(lowerstr.equals(abstractStr)){
				goodPaper = true;
			}

			if(lowerstr.equals(headerBoundary) || 
				lowerstr.equals(headerBoundary2)||
				lowerstr.equals(headerBoundary3) ||
				lowerstr.equals(headerBoundary4) ||
				lowerstr.startsWith(ackStr) ||
				lowerstr.startsWith(contentStr)
			){
				break;
			}

			buffer += xmlRemovedStr + "\n";
//			buffer += line + "\n";
			if(goodPaper){	
				maxLen += xmlRemovedStr.length();
			}
//
//			System.out.println( xmlRemovedStr );

		}

		bf.close();

		if(!goodPaper || maxLen > len_limit) buffer = "";
		else{
//                System.out.println("source file: " + xmlFile);
	                System.out.println("author: " + author);

			System.out.println(buffer);
			paperList.add(buffer);
		}

		return buffer;
	}
}
	
