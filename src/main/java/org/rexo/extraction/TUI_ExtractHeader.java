
/** 
	Fuchun Peng, Oct. 2003
 */

package org.rexo.extraction;

import java.io.File;
import java.io.FileNotFoundException;


public class TUI_ExtractHeader 
{

	public static void main (String[] args) throws FileNotFoundException, java.io.IOException
	{
		ExtractHeader extracter = new ExtractHeader();
		String retS = extracter.extractHeaderDir( new File(args[0]), 0 );
//		extracter.randomSelectPapers(45);
	}
}
