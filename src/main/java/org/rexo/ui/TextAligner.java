package org.rexo.ui;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.util.EnglishDictionary;

import java.io.*;
import java.util.*;


public class TextAligner {

  private static class AlignmentException extends Exception {
	private static final long serialVersionUID = 1L;

	AlignmentException(String description) {
      super(description);
    }
  }

  private static class Span {
    String label;
    CharSequence text;
    int start;
    int end;
    boolean isUseful;

    Span(String label) {
      this.label = label;
    }
    Span(Span first) {
      this.label = first.label;
      this.text = first.text;
      this.start = first.start;
      this.end = first.end;
    }
  }

  private static class Alignment {
    //private final int maxNormOffset = 50;
    // Not really the max offset, the offset window size
    private final static int normalMaxOffset = 400;
    private final int maxOffset;

    // Gotcha: consider the following example: (taken from 
    // original text: forDistributed and Parallel Architectures ffT.
    // new text: RealWorld Applications forDistributed and Parallel Architectures \LambdaT.

    private final int copyPenalty = 1;
    private final int insertPenalty = 1;
    private final int deletePenalty = 1;

    private CharSequence fromText;
    private CharSequence toText;

    // A mapping of offsets from 'fromText' to 'toText'
    //private int maxOffset;
    private int[] baseOffsets;
    private int[][] offsetCostTable;
    private int[][] prevIndexTable;
    private int[] bestCosts;

    private int[][] numIncidentPaths = null;
    private int[] numVisitedColumns = null;

    private int[] offsetMap;

    private int finalCost = -1;

    // Whether the row-clearing algorithm should be performed.  This reduces the amount of
    // memory needed but slows down the program.
    private boolean clearRows = true;

    Alignment(CharSequence fromText, CharSequence toText) throws AlignmentException {
      this(fromText, toText, normalMaxOffset);
    }
    Alignment(CharSequence fromText, CharSequence toText, int maxOffset) throws AlignmentException {
      this.maxOffset = maxOffset;
      this.fromText = fromText;
      this.toText = toText;

      System.gc();
      // Check if there will be enough memory to hold the entire table
      long maxMemory = Runtime.getRuntime().maxMemory();
      // xxx assume sizeof(int) == 4 bytes
      // to be conservative, require twice the memory required by the biggest table
      if (maxMemory/2 >= (((long)maxOffset*2+1)*toText.length()*4)) {
	clearRows = false;
      }
      System.out.println("Max offset is: "+maxOffset+" toText.length is"+toText.length()+" total size is "+(maxOffset*2+1) *toText.length()+"clearRows="+clearRows);

      offsetCostTable = new int[toText.length()][];
      prevIndexTable = new int[toText.length()][];
      baseOffsets = new int[toText.length()];
      bestCosts = new int[toText.length()];
      if (clearRows) {
	numIncidentPaths = new int[toText.length()][];
	numVisitedColumns = new int[toText.length()];
      }
      computeOffsetMap();
    }

    private int indexToOffset(int i, int row) {
      return (i - maxOffset) + baseOffsets[row];
    }
    private int offsetToIndex(int o, int row) {
      return (o - baseOffsets[row]) + maxOffset;
    }

    private boolean isIndexInRange(int i) {
      if (i >= 0 && i < maxOffset*2+1)
	return true;
      return false;
    }

    private int copyCost(char from, char to) {
      return (from == to) ? 0 : copyPenalty;
    }

    private int deleteCost(char ch) {
      //return (ch == ' ') ? 0 : deletePenalty;
      return deletePenalty;
    }

    private int insertCost(char ch) {
      //return (ch == ' ') ? 0 : insertPenalty;
      return insertPenalty;
    }

    private void clearIncidentPath(int position, int index) {

      // If this row has already been finished, do nothing
      // careful: Don't use -1 for initialization since this is a valid offset
      if (offsetMap[position] != Integer.MAX_VALUE - 100) {
	return;
      }
      //if (position == 11293)
      //System.out.println("clearing incident path: "+(position)+" "+index);
      assert numIncidentPaths[position][index] > 0;
      numIncidentPaths[position][index]--;
      //if (position == 11293) System.out.println(numIncidentPaths[position][index]);
      //if (position == 11293) System.out.println(numVisitedColumns[position]);
      if (numIncidentPaths[position][index] == 0) {
	// Recurse to clear the previous positions on this path
	// that have unfreed table entries
	if (position > 0) {
	  clearIncidentPath(position-1, prevIndexTable[position][index]);
	}

	// There must always be at least one path, so before there
	// must have been at least two
	assert numVisitedColumns[position] > 1;
	numVisitedColumns[position]--;
      }
      //System.out.println(position+": new visited columns: "+numVisitedColumns[position]);

      // Gotcha : it's possible that this is true for the first time even if 
      // numVisitedColumns was not just decremented, so this needs to be outside the
      // above conditional.
      if (numVisitedColumns[position] == 1) {
	// Since only one possible path to this row remains,
	// free the dynamic programming tables' row for it
	int numFound = 0;
	int foundIndex = -1;
	for (int i = 0; i < numIncidentPaths[position].length; i++) {
	  if (numIncidentPaths[position][i] > 0) {
	    foundIndex = i;
	    // break;
	    numFound++;
	  }
	}
	assert numFound == 1 : "Numfound should be 1; is "+numFound;
	offsetMap[position] = indexToOffset(foundIndex, position);
	//System.out.println(position+": Freeing dynamic tables row for "+position+", found offset="+offsetMap[position]);
	prevIndexTable[position] = null;
	numIncidentPaths[position] = null;
      }
    }

    private void computeOffsetMap() throws AlignmentException {
      // debug
      boolean haveWarned = false;
      boolean haveWarnedEnd = false;

      int sizeDif = Math.abs(toText.length() - fromText.length());
      // Don't throw exceptions for small spans since they may be used
      // for verification.
      if (sizeDif > 1000 && sizeDif > toText.length()/2) {
	System.out.println("sizes do not match: fromSize="+fromText.length()+" toSize="+toText.length());
	throw new AlignmentException("sizes do not match: fromSize="+fromText.length()+" toSize="+toText.length());
      }

      offsetMap = new int[toText.length()];
      for (int i = 0; i < toText.length(); i++) {
	offsetMap[i] = Integer.MAX_VALUE - 100;
      }

      // Compute the offset cost table
      // Location == absolute byte location in 'from' file
      // Offset == relative offset between a byte in 'to' file and best match in 'from' file
      // BaseOffset = "default" offset for a table row
      // Index == index into a table row, specifies an additional offset on top of the base
      // of amount (i - maxOffset). Valid indices range from 0 to 2*maxOffset+1.
      // Position == byte in 'toFile'
      baseOffsets[0] = 0;
      for (int position = 0; position < toText.length(); position++) {
	int minCost = Integer.MAX_VALUE;
	int bestOffset = -1;

	// Allocate memory for this row
	offsetCostTable[position] = new int[maxOffset*2+1];
	prevIndexTable[position] = new int[maxOffset*2+1];
	if (clearRows) {
	  numIncidentPaths[position] = new int[maxOffset*2+1];
	}
	// save memory
	if (position > 2) {
	  offsetCostTable[position-2] = null;
	}
	for (int i = 0; i < offsetCostTable[position].length; i++) {
	  int copyCost;
	  int insertCost;
	  int deleteCost;

	  int offset = indexToOffset(i, position);
	  int fromLocation = offset+position;
	  boolean isValidOffset = (fromLocation > -1 && fromLocation < fromText.length());

	  // Invalid offsets are marked as expensive
	  if (!isValidOffset) {
	    offsetCostTable[position][i] = Integer.MAX_VALUE-100;
	    continue;
	  }

	  // Initialize the first row
	  if (position == 0) {
	    char fromChar = fromText.charAt(fromLocation);
	    char toChar = toText.charAt(0);
	    copyCost = copyCost(fromChar, toChar) + offset*deletePenalty;
	    offsetCostTable[position][i] = copyCost;

	    if (copyCost < minCost) {
	      minCost = copyCost;
	      bestOffset = offset;
	    }
	    continue;
	  }

	  // Initialize the following rows

	  // Invalid offsets should have been marked as expensive in the previous iteration so
	  // no need to check for them
	  if (isIndexInRange(offsetToIndex(offset, position-1))) {
	    char fromChar = fromText.charAt(fromLocation);
	    char toChar = toText.charAt(position);
	    int prevIndex = offsetToIndex(offset, position-1);
	    copyCost = copyCost(fromChar, toChar) + offsetCostTable[position-1][prevIndex];
	  }
	  else {
	    copyCost = Integer.MAX_VALUE-100;
	  }
	  // if i+1 is valid index/offset
	  //if (i+1 < offsetCostTable[position].length) 
	  if (isIndexInRange(offsetToIndex(offset+1, position-1))) {
	    char toChar = toText.charAt(position);
	    int prevIndex = offsetToIndex(offset+1, position-1);
	    insertCost = insertCost(toChar) + offsetCostTable[position-1][prevIndex];
	  }
	  else
	    insertCost = Integer.MAX_VALUE-100;

	  // if i-1 is valid index/offset
	  if (i-1 > -1) {
	    //if (isIndexInRange(offsetToIndex(offset-1), position))
	    char fromChar = fromText.charAt(fromLocation);
	    deleteCost = deleteCost(fromChar) + offsetCostTable[position][i-1];
	  }
	  else
	    deleteCost = Integer.MAX_VALUE-100;

	  // With variable start locations, we might have a situation where no are moves are legal
	  if (copyCost >= Integer.MAX_VALUE-100 && insertCost >= Integer.MAX_VALUE-100 && deleteCost >= Integer.MAX_VALUE-100) {
	    offsetCostTable[position][i] = Integer.MAX_VALUE - 100;
	    continue;
	  }

	  if (copyCost <= insertCost && copyCost <= deleteCost) {
	    offsetCostTable[position][i] = copyCost;
	    prevIndexTable[position][i] = offsetToIndex(offset, position-1);
	  }
	  else if (insertCost < deleteCost) {
	    offsetCostTable[position][i] = insertCost;
	    prevIndexTable[position][i] = offsetToIndex(offset+1, position-1);
	  }
	  //else if (deleteCost < Integer.MAX_VALUE - 100) {
	  else {
	    assert deleteCost < Integer.MAX_VALUE - 100 : position + " " + deleteCost + " " + insertCost + " " + copyCost;
	    assert i > 0;
	    offsetCostTable[position][i] = deleteCost;
	    prevIndexTable[position][i] = prevIndexTable[position][i-1];
	  }

	  if (offsetCostTable[position][i] < minCost) {
	    minCost = offsetCostTable[position][i];
	    bestOffset = offset;
	  }

	  // debug
	  assert offsetCostTable[position][i] < Integer.MAX_VALUE - 100 : "copy: "+copyCost+" insert: "+insertCost+" delete: "+deleteCost;
	  //if (position == 1) {
	    //System.out.println("inserting: "+i+" "+numIncidentPaths[position-1][prevIndexTable[position][i]]);
	  //}
	  if (clearRows) {
	    numIncidentPaths[position-1][prevIndexTable[position][i]]++;

	    if (numIncidentPaths[position-1][prevIndexTable[position][i]] == 1)
	      numVisitedColumns[position-1]++;
	  }
	}

	// Initialize the next line's base offset
	if (position < toText.length()-1) {
	  //System.out.println("new min cost: "+minCost);
	  //System.out.println("new best offset: "+bestOffset);
	  baseOffsets[position+1] = bestOffset;
	}

	// Save the best cost
	bestCosts[position] = minCost;
	// Quit if the cost is too high
	int windowCost = -1;
	if (position >= 4000)
	  windowCost = bestCosts[position] - bestCosts[position-4000];
	if (windowCost >= 4000*.75) {
	  //throw new AlignmentException("intermediate window cost "+windowCost+" too high; no alignment found");
	   if (!haveWarned) {
	     haveWarned = true;
	     System.out.println("A: intermediate window cost "+windowCost+" too high");
	   }
	}

        if (clearRows && position >= 2) {
	  // Update path counts for previous rows and free table rows
	  for (int i = 0; i < offsetCostTable[position-1].length; i++) {
	    if (numIncidentPaths[position-1][i] == 0 && offsetCostTable[position-1][i] < Integer.MAX_VALUE - 100) {
	      clearIncidentPath(position-2, prevIndexTable[position-1][i]);
	    }
	  }
	}
	//  int j;
//  	for (j = 0; j < position; j++) {
//  	  if (prevIndexTable[j] != null)
//  	    break;
//  	}
// 	System.out.println("allocated width: "+(position-j+1)+" "+j);

      }

      // Initialize the offset mappings
      //offsetMap = new int[toText.length()];

      // Compute the best final position
      int mapIndex = -1;
      for (int position = toText.length()-1; position > -1; position--) {
	// Stop if the path from the beginning to here has been 
	// determined already
	if (offsetMap[position] != Integer.MAX_VALUE - 100)
	  break;

	// Initialize mapIndex
	if (position == toText.length()-1) {
	  int minIndex = 0;
	  for (int i = 0; i < offsetCostTable[position].length; i++) {
	    if (offsetCostTable[position][i] < offsetCostTable[position][minIndex])
	      minIndex = i;
	  }
	  System.out.println("final offset: "+indexToOffset(minIndex, position));
	  finalCost = offsetCostTable[position][minIndex];
	  System.out.println("final cost: "+finalCost);
	  // if (finalCost >= (position+1)/4) {
	  //if (finalCost >= (position+1)/2) {
	  int windowCost = -1;
	  if (position >= 3000)
	    windowCost = bestCosts[position] - bestCosts[position-3000];
	  System.out.println("final window cost: "+windowCost);
	  if (windowCost > .5*3000) {
	  //throw new AlignmentException("Text at end appears misaligned: window cost="+windowCost);
	     if (!haveWarnedEnd) {
	       haveWarnedEnd = true;
	       System.out.println("B:Text at end appears misaligned: window cost="+windowCost);
	     }
	  }
	  if (finalCost >= (position/3)) {
	    //throw new AlignmentException("final cost too high; no alignment found");
	    System.out.println("C:final cost too high");
	  }
	  mapIndex = minIndex;
	}
	offsetMap[position] = indexToOffset(mapIndex, position);
	mapIndex = prevIndexTable[position][mapIndex];
      }
      // Final step -- set the offsetmap to map to the beginning of aligned text, not
      // the end 
      for (int position = toText.length()-1; position > 0; position--) {
	if (offsetMap[position] > offsetMap[position-1]) {
	  //System.out.println("shifting offset: old="+offsetMap[position]+" new="+offsetMap[position-1]+" original text: "+fromText.subSequence(position+offsetMap[position-1], position+offsetMap[position]+1));
	  offsetMap[position] = offsetMap[position-1];
	}
      }
    }

    public int getCost() {
      return finalCost;
    }

    Span alignSpan(Span sourceSpan) throws AlignmentException {
      Span ret = new Span(sourceSpan);
      //System.out.println("aligning span: label="+ret.label+" start="+ret.start+" end="+ret.end);
      assert sourceSpan.isUseful;
      assert ret.start > -1 && ret.start < offsetMap.length : "ret.start="+ret.start+" offsetMap.length="+offsetMap.length;
      assert ret.end > -1 && ret.end < offsetMap.length: "ret.end="+ret.end+" offsetMap.length="+offsetMap.length;

      int oldStart = ret.start;
      int oldEnd = ret.end;
      CharSequence origText = ret.text;

      int alignedStart = ret.start+offsetMap[ret.start];
      int alignedEnd = ret.end+offsetMap[ret.end];
      ret.start = ret.start+offsetMap[ret.start];
      ret.end = ret.end+offsetMap[ret.end];
      assert ret.start > -1 && ret.start < fromText.length() : "ret.start="+ret.start+" fromText.length()="+fromText.length();
      assert ret.end > -1 && ret.end < fromText.length() : "ret.end="+ret.end+" fromText.length()="+fromText.length();
      ret.text = fromText.subSequence(ret.start, ret.end+1);

      //int spanCost = bestCosts[oldEnd] - bestCosts[oldStart];
      int spanLength = oldEnd - oldStart + 1;
      // If this is a short span, find the string edit distance for it for alignment purposes.
      // Note: the global 'betsCosts' table is ill-suited to this since if an alignment appears
      // after an insertion/deletion then bestCosts[oldEnd]-bestCosts[oldStart] may be high
      // even if the alignment is accurate.

      if (spanLength > 5 && spanLength < 1000) {
	Alignment verifier;
	// Hack -- I think Alignment.getCost() (at the moment)
	// neglects to take into account deleted text at
	// the end of sourceText, so (for now) to get
	// the proper value we always align from the smaller
	// to larger body of text. (which should hopefully take
	// care of this for the majority of affected cases)
	if (origText.length() <= ret.text.length())
	  verifier = new Alignment(origText, ret.text);
	else
	  verifier = new Alignment(ret.text, origText);

	int spanCost = verifier.getCost();
	System.out.println("original text: "+origText);
	System.out.println("aligned text : "+ret.text);
	System.out.println("span cost is: "+spanCost);

	if (spanCost >= (spanLength)*.5)
	  throw new AlignmentException("span cost too high; cost="+spanCost);
      }

      return ret;
    }

  }

  // TODO: \ -> '' 
  //static void produceSpans(Element rootElement, StringBuffer sourceText, List spansSoFar) {
  // Added importance hack for later filtering out of unimportant spans
  static void produceSpans(Element rootElement, StringBuffer sourceText, List spansSoFar, boolean isUseful) {
    Span newSpan = new Span(rootElement.getName());
    List contentList = rootElement.getContent();

    // Skip elements with no content
    if (contentList.size() == 0)
      return;

    // Start span
    spansSoFar.add(newSpan);
    newSpan.start = sourceText.length();

    newSpan.isUseful = isUseful;

    Iterator contentI = contentList.iterator();
    while (contentI.hasNext()) {
      Content content = (Content) contentI.next();

      if (content instanceof Text) {
	String text = ((Text)content).getText();
	if (text.matches("\\s+")) {
		  //System.out.println("normalizing white space: "+text);
		  //sourceText.append(' ');
		  //System.out.println("skipping white space: "+text.length());
		}
		else {
		  //System.out.println("appending non-white space: "+text.trim());
		  sourceText.append(text.trim());
		  // May sometimes insert extra spaces at the end of lines, but alignment should handle this.
		  // try Ignore all whitespace
		  //sourceText.append(' ');
		}
      }
      // recurse on child elements
      else if (content instanceof Element) {
	String childName = ((Element)content).getName();
	// all descendents of 'reference' and 'body' should be skipped for alignment
	// (as well as 'reference' and 'body' themselves)
	if (!isUseful || childName.equals("reference") || childName.equals("body") || childName.equals("biblioEpilogue"))
	  produceSpans((Element) content, sourceText, spansSoFar, false);
	// everything else should be aligned
	else
	  produceSpans((Element) content, sourceText, spansSoFar, true);
      }
    }
    // If nothing was found, insert a space.
    if (newSpan.start == sourceText.length()) {
      //System.out.println("adding extra space");
      sourceText.append(' ');
    }

    // Finish span
    newSpan.end = sourceText.length()-1;
    newSpan.text = sourceText.subSequence(newSpan.start, newSpan.end+1);
    //System.out.println("finished span: label="+newSpan.label+" start="+newSpan.start+" end="+newSpan.end);
  }

  static List produceAlignedSpans(Document oldDoc, NewHtmlTokenization newDocTok) throws AlignmentException {

    List oldDocSpans = new LinkedList();
    List alignedSpans = new LinkedList();
    StringBuffer oldDocText = new StringBuffer();

    // get spans for oldDoc
    produceSpans(oldDoc.getRootElement(), oldDocText, oldDocSpans, true);

    // create oldDoc->newDocTok alignment
    CharSequence newDocText = (CharSequence) newDocTok.getDocument();

    //Align the new text onto the old text
    Alignment aligner;
    int maxOffset = 100;
    // gotcha: I have found at least one paper (http:##www-adele.imag.fr#Les.Publications#intConferences#EDOC2002Cer.pdf.pp.tagged.xml)
    // for which a dynamic table width of 800 was not sufficient for alignment, but 1600 worked!
    // another example: http:##www-cgi.cs.cmu.edu#afs#cs#project#coda#Web#docdir#clement00-thesis.ps.gz, which required a width of
    // 1600 and required > 10 minutes to convert!
    //StringBuffer revNewDocText = new StringBuffer(newDocText.toString()).reverse();
    //StringBuffer revOldDocText = new StringBuffer(oldDocText.toString()).reverse();
    while (true) {
      try {
	aligner = new Alignment(newDocText, oldDocText, maxOffset);
	//newDocText.reverse();
	//oldDocText.reverse();
	//Alignment verifier = new Alignment(newDocText, oldDocText, maxOffset);
	//newDocText.reverse();
	//oldDocText.reverse();
	//if (Math.abs(verifier.getCost() - aligner.getCost()) > 50) {
	  //System.out.println("D:Cost and reverse cost don't match: cost="+aligner.getCost()+"reverse cost="+verifier.getCost());
	  //throw new AlignmentException("Cost and reverse cost don't match: cost="+aligner.getCost()+"reverse cost="+verifier.getCost());
	//}
	//aligner.cost()

	Iterator oldSpanI = oldDocSpans.iterator();
	while (oldSpanI.hasNext()) {
	  Span span = (Span) oldSpanI.next();
	  // Hack -- skip "unimportant" spans such as the body and non-labeled references
	  if (!span.isUseful) {
	    System.out.println("skipping 'unimportant' span "+span.label);
	    continue;
	  }
	  Span alignedSpan = aligner.alignSpan(span);
	  alignedSpans.add(alignedSpan);
	}
	break;
      }
      catch (AlignmentException e) {
	alignedSpans.clear();
	// only go up to width of 1600
	if (maxOffset >= 1600)
	  throw e;

	// If aligner fails. try again using a larger table
	System.out.println(e);
	System.out.println("Alignment failed! Trying again with a larger table...");
	maxOffset *= 2;
      }
    }
    // Iterator oldSpanI = oldDocSpans.iterator();
//     while (oldSpanI.hasNext()) {
//       Span span = (Span) oldSpanI.next();
//       Span alignedSpan = aligner.alignSpan(span);
//       alignedSpans.add(alignedSpan);
//     }

    return alignedSpans;
  }

  private static void writeSpansToFile(File outputFile, String fileId, List spanList) throws IOException {
    BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
    out.write("fileId="+fileId+"\n");
    Iterator spanI = spanList.iterator();
    while (spanI.hasNext()) {
      Span span = (Span) spanI.next();
      out.write(span.label+" "+span.start+" "+span.end+" "+span.text+"\n");
      //out.write(span.label+" "+span.start+" "+span.end+"\n");
    }
    out.close();
  }

  private static void writeLock(File lockFile) throws IOException {
    assert !lockFile.exists();
    boolean status = lockFile.createNewFile();
    assert status;
  }

  private static void removeLock(File lockFile) {
    assert lockFile.exists();
    boolean status = lockFile.delete();
    assert status;
  }

  public static void main(String[] argv) throws Exception {
    File labeledXMLDir, newXMLDir, outputXMLDir;
    HashSet labeledXMLFiles = new HashSet();
    List newXMLFiles = new LinkedList();

    SAXBuilder saxBuilder = new SAXBuilder();

    if (argv.length < 3) {
      System.out.println("Usage: TextAligner labledXMLDirectory newXMLDirectory outputXMLDirectory");
      System.exit(0);
    }

    labeledXMLDir = new File(argv[0]);
    newXMLDir = new File(argv[1]);
    outputXMLDir = new File(argv[2]);

    if (labeledXMLDir.isDirectory()) {
		File[] dirListing = labeledXMLDir.listFiles(new FileFilter() {
				public boolean accept(File pathName) {
					String fileName = pathName.getName();
					return fileName.endsWith(".xml") || fileName.endsWith(".XML");
				}
			});
		for (int i = 0; i < dirListing.length; i++) {
			File file = dirListing[i];
			labeledXMLFiles.add(file.getName());
		}
    }
    else {
		// temp -- treat as singleton file
		//labeledXMLFiles.put("test", labeledXMLDir);
		File file = labeledXMLDir;
		labeledXMLFiles.add(file);
    }

    if (newXMLDir.isDirectory()) {
		File[] dirListing = newXMLDir.listFiles(new FileFilter() {
				public boolean accept(File pathName) {
					String fileName = pathName.getName();
					return fileName.endsWith(".xml");
				}
			});
		newXMLFiles.addAll(Arrays.asList(dirListing));
    }
    else {
		newXMLFiles.add(newXMLDir);
    }

    // For each new file, try to find an equivalent old file
    Iterator newFileI = newXMLFiles.iterator();
    while (newFileI.hasNext()) {
		File newFile = (File) newFileI.next();
		String oldName = newFile.getName().replaceAll(".pstotext.xml", ".pp.tagged.xml");
		System.out.println("processing newFile: "+newFile+" oldName: "+oldName);

		if (labeledXMLFiles.contains(oldName)) {
			// process pair of files
			File oldFile = new File(labeledXMLDir, oldName);
			File outputFile = new File(outputXMLDir, newFile.getName().replaceAll(".pstotext.xml", "")+".spans");
			File lockFile = new File(outputXMLDir, newFile.getName().replaceAll(".pstotext.xml", "")+".spans.lock");
			// skip files already processed
			if (outputFile.exists() || lockFile.exists()) {
				System.out.println("Skipping file "+newFile);
				continue;
			}
			System.out.println("writing lock file...");
			writeLock(lockFile);

			System.out.println("found pair of files: "+oldFile+" "+newFile);
			Document oldDoc, newDoc;
			try {
				oldDoc = saxBuilder.build(new FileInputStream(oldFile));
				newDoc = saxBuilder.build(new FileInputStream(newFile));
			}
			catch (JDOMParseException e) {
				System.out.println(e);
				continue;
			}

			// ignore de-hyphenation
			NewHtmlTokenization newDocTok = NewHtmlTokenization.createNewHtmlTokenization(newDoc, EnglishDictionary.createDefault() );
			List spanList;
			try {
				spanList = produceAlignedSpans(oldDoc, newDocTok);
			}
			catch (AlignmentException e) {
				System.out.println(e);
				continue;
			}
			writeSpansToFile(outputFile, oldFile.toString(), spanList);
			removeLock(lockFile);

		}
		else {
			System.out.println("No labeled files corresponding to '"+newFile+"' in new XML directory");
		}
    }
  }
}
