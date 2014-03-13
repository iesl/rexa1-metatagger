package org.rexo.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.util.RegexFileFilter;

public class ExtractReference{
	String xmlTag = "<[^>]*>";
	String xmlTag_font = "<font[^>]*>";
	String xmlTag_nofont = "<[^font][^>]*/>";
	String xmlStartTag = "<\\?xml [^<]*\\?>";
	String docTag = "<doc>|</doc>";
	String contentTag = "<content>|</content>";

	String refBoundary  = "references";	

	FileFilter filter = new RegexFileFilter(Pattern.compile(".*xml$"));

	ArrayList paperList = new ArrayList();

	public ExtractReference()
	{
	}

	public String extractRefDir(File xmlFile, int flag) throws java.io.IOException, java.io.FileNotFoundException
	{
		String buffer = "";
		if(xmlFile.isDirectory()){
			File[] directoryContents = xmlFile.listFiles();
			for (int i = 0; i < directoryContents.length; i++) {

				if (directoryContents[i].isDirectory())
					buffer += extractRefDir(directoryContents[i], flag);
				else{
					if (filter != null && !filter.accept(directoryContents[i])) continue;
					buffer += extractRefFile(directoryContents[i], flag);
				}
			}
		}
		else if(xmlFile.isFile()){
				buffer += extractRefFile(xmlFile, flag);
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

	public String extractRefFile(File xmlFile, int flag) throws java.io.IOException, java.io.FileNotFoundException
	{

//		File xmlFile = new File(xml);
		System.out.println("source file: " + xmlFile);
		BufferedReader bf = new BufferedReader (new FileReader (xmlFile));

		String buffer = "";	
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

			String tempText = xmlRemovedStr.replaceAll(xmlTag, "");
			String lowerstr = tempText.toLowerCase();
			if(lowerstr.equals(refBoundary)){
				goodPaper = true;
			}

			if(goodPaper){
				buffer += xmlRemovedStr + "\n";
	//			buffer += line + "\n";

//				System.out.println( xmlRemovedStr );
			}

		}

		bf.close();

		if(!goodPaper) buffer = "";
		else{
			System.out.println(buffer);
			paperList.add(buffer);
		}

		return buffer;
	}
}
	
