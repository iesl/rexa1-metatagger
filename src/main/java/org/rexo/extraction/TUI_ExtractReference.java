
/** 
	Fuchun Peng, Oct. 2003
 */

package org.rexo.extraction;

import java.io.File;
import java.io.FileNotFoundException;


public class TUI_ExtractReference
{

	public static void main (String[] args) throws FileNotFoundException, java.io.IOException
	{
		ExtractReference extracter = new ExtractReference();
		String retS = extracter.extractRefDir( new File(args[0]), 1 );
//		extracter.randomSelectPapers(45);
	}
}
