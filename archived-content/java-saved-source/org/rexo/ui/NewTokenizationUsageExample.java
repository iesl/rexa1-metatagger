package org.rexo.ui;

import edu.umass.cs.mallet.base.extract.StringSpan;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.rexo.referencetagging.NewHtmlTokenization;


import java.io.*;
import java.util.Iterator;
import java.util.List;

public class NewTokenizationUsageExample {

  // Token properties that are set by NewHtmlTokenization:
  private static String[] booleanPropertyNames = new String[] {
    // font size properties
    "largestfont",
    "largefont",
    "smallfont",

    // font change properties
    "newfontsize",
    "newfontname",
    "newfontnumber",

    // position properties
    "newline",
		"newpage",

    // quotation properties
    "leftQuote",
    "normalQuote",
    "rightQuote",

    // structural properties
    "authorBeginning"
  };
	// page number
	private static int pageNum;
  // bounding box coordinates 
  // (normally only set if token is first in a new 'tbox', i.e. newfontsize=true or newline=true)
  private static double llx;
  private static double lly;
	

  private static void printTokenization(NewHtmlTokenization tokenization, BufferedWriter out) throws IOException {
    List activeSpanList = tokenization.getActiveSpanList();
    System.out.println("Printing tokenization: ");
    for (int i = 0; i < tokenization.size(); i++) {
      StringSpan span = (StringSpan) tokenization.getToken(i);
      System.out.println(span.getText());
      System.out.println((String)activeSpanList.get(i));

      // Token text
      out.write(span.getText()+"\n");
      // Active spans
      out.write((String)activeSpanList.get(i)+"\n");

      StringBuffer propertyString = new StringBuffer(("("));

      boolean appended = false;
      for (int j = 0; j < booleanPropertyNames.length; j++) {
	String propertyName = booleanPropertyNames[j];
	boolean value = (span.getNumericProperty(propertyName) > 0);

	//propertyString.append(propertyName+"="+value ? "true" : "false");
	if (value) {
	  if (appended) {
	    propertyString.append(", ");
	  }
	  propertyString.append(propertyName);
	  appended = true;
	}
      }
			if (span.getNumericProperty("pageNum") != 0) {
				pageNum = (int) span.getNumericProperty("pageNum");
				propertyString.append(", pageNum="+pageNum);
			}
      if (span.getNumericProperty("llx") != 0 || span.getNumericProperty("lly") != 0) {
	llx = span.getNumericProperty("llx");
	lly = span.getNumericProperty("lly");
	propertyString.append(", llx="+llx+" lly="+lly);
      }
      
      propertyString.append(")");
      // Properties
      System.out.println(propertyString);
      out.write(propertyString+"\n");
      out.write("\n");
      //System.out.println();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.out.println("usage: NewTokenizationUsageExample documentDirectory spanDirectory outputDirectory");
    }

    System.out.println("Initializing de-hyphenation dictionary");
    // For each file in span directory, load corresponding document and initialize span reader
    SAXBuilder saxBuilder = new SAXBuilder();

    File spanDir = new File(args[1]);
    File[] dirListing;
    if (spanDir.isDirectory()) {
      dirListing = spanDir.listFiles(new FileFilter() {
	  public boolean accept(File pathName) {
	    String fileName = pathName.getName();
	    return fileName.endsWith(".spans");
	  }
	});
    }
    else {
      dirListing = new File[] {spanDir};
    }
    System.out.println("got "+dirListing.length+" files");

    for (int i = 0; i < dirListing.length; i++) {
      File spanFile = dirListing[i];
      File docFile = new File(args[0], spanFile.getName().replaceAll("\\.spans", ""));
      File outFile = new File(args[2], spanFile.getName().replaceAll("\\.spans", "")+".list");
      System.out.println(docFile+" "+spanFile+" "+outFile+"...");

      Document doc = saxBuilder.build(new FileInputStream(docFile)); 
      BufferedReader spanReader = new BufferedReader(new FileReader(spanFile));
      BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

      // Initialize tokenization
      NewHtmlTokenization tokenization = new NewHtmlTokenization(doc, null, spanReader);

      // Print header subtokenization
      List headerSubToks = tokenization.getSubtokenizationsByName("headers-hlabeled");
      assert headerSubToks.size() == 1;
      out.write("Header tokenization:\n");
      printTokenization((NewHtmlTokenization)headerSubToks.get(0), out);

      // Print reference subtokenizations
      List referenceSubToks = tokenization.getSubtokenizationsByName("reference-hlabeled");
      Iterator referenceI = referenceSubToks.iterator();
      while (referenceI.hasNext()) {
	NewHtmlTokenization refTokenization = (NewHtmlTokenization) referenceI.next();
	out.write("\nReference tokenization:\n");
	printTokenization(refTokenization, out);
      }
      out.close();
    }
  }
}
