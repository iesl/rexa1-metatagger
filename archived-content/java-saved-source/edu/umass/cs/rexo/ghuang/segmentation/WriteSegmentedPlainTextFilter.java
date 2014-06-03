package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.rexo.pipeline.AbstractFilter;
import org.rexo.pipeline.components.RxDocument;
import org.rexo.referencetagging.NewHtmlTokenization;

import edu.umass.cs.mallet.base.types.Token;


/**
 * @author ghuang
 *
 */
public class WriteSegmentedPlainTextFilter extends AbstractFilter
{
	private static Logger log = Logger.getLogger( WriteSegmentedPlainTextFilter.class );
	
	/** 
	 * @see org.rexo.pipeline.components.RxFilter#accept(org.rexo.pipeline.components.RxDocument)
	 */
	public int accept(RxDocument rdoc)
	{
		// Write out the header and reference segments in a plain text file:
		//     	numReferences=?
		//
		//		h: header line 1
		//     	h: ...
		//     	h: header line m
		//
		//     	b: body line 1 
		//	   	b: ...
		//		b: body line n
		//
		//		p: biblio prologue line 1
		// 		p: ...
		//		
		//		r1: ref 1 line 1
		//		r1: ...
		//		r1: ref 1 line k_1
		//
		//		r2: ref 2 line 1
		//		 		...
		//
		//		e: epilogue line 1
		//		e: ...
		
		log.info( "WriteSegmentedPlainTextFilter" );
		Map segmentations = (Map)rdoc.getScope( "document" ).get( "segmentation" );


		if (segmentations == null) {
			getLogger( rdoc ).warn( "No segmentations available for document " + rdoc );
			log.error( "(text writer): no segmentation" );
			return ReturnCode.ABORT_PAPER;
		}

		String inputFilePath = (String)rdoc.getScope( "document" ).get( "file.name" );
		File inputFile = new File( inputFilePath );
		String inputName = inputFile.getName();
		PrintWriter printWriter = null;

		try {
			
			NewHtmlTokenization headerTokenization = (NewHtmlTokenization)segmentations.get( "headerTokenization" );
			NewHtmlTokenization bodyTokenization = (NewHtmlTokenization)segmentations.get( "bodyTokenization" );
			NewHtmlTokenization prologueTokenization = (NewHtmlTokenization)segmentations.get( "prologueTokenization" );
			List referencesList = (List)segmentations.get( "referenceList" ); // list of NewHtmlTokenizations			
			NewHtmlTokenization epilogueTokenization = (NewHtmlTokenization)segmentations.get( "epilogueTokenization" );

			File outdir = (File)rdoc.getScope( "session" ).get( "output.directory" );
			File indir = (File)rdoc.getScope( "session" ).get( "input.directory" );
			String relativeOutputPath = inputFile.getParentFile().getPath().substring( indir.getPath().length() );
			relativeOutputPath = relativeOutputPath.replaceFirst( "^/", "" );
			File outputPath = new File( outdir, relativeOutputPath );
			outputPath.mkdirs();
			File fullpath = new File( outputPath, inputName + ".seg.txt" );
			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fullpath))); 
			
			printWriter.println("numReferences=" + referencesList.size());
			writeHelper(printWriter, "h: ", headerTokenization);
			printWriter.println();
			writeHelper(printWriter, "b: ", bodyTokenization);
			printWriter.println();
			writeHelper(printWriter, "p: ", prologueTokenization);
			printWriter.println();
			
			for (int i = 0; i < referencesList.size(); i++) {
				NewHtmlTokenization ref = (NewHtmlTokenization) referencesList.get(i);
				writeHelper(printWriter, "r" + (i+1) + ": ", ref);
				printWriter.println();
			}
			
			writeHelper(printWriter, "e: ", epilogueTokenization);
			printWriter.close();
			
		}
		catch (IOException e) {
			log.info( "(print writer) " + e.getClass().getName() + ": " + e.getMessage() );
			
			if (printWriter != null) 
				printWriter.close();
			
			return ReturnCode.ABORT_PAPER;
		}

		return ReturnCode.OK;
	}
	
	
	private static void writeHelper(PrintWriter writer, String prefix, NewHtmlTokenization tokens) throws IOException
	{
		if (tokens == null) return;

		int lineNum = 0;
		
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.getToken(i);
			int tokLineNum = (int) token.getNumericProperty("lineNum");

			if (tokLineNum == 0) { // I don't know why this happens
				continue;
			}
			else if (tokLineNum != lineNum) {
				lineNum = tokLineNum;
				writer.print("\n" + prefix + token.getText() + " ");
			}
			else {
				writer.print(token.getText() + " ");	
			}
			
		}
	}
	
}
