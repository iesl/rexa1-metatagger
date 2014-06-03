
/** 
	Fuchun Peng, July 2003
 */

package org.rexo.extraction;

import java.io.FileNotFoundException;

import edu.umass.cs.mallet.base.util.CommandOption;

public class TestUI 
{
	static CommandOption.File crfInputFileOption = new CommandOption.File
	(TUI.class, "crf-input-file", "FILENAME", true, null,
	 "The name of the file to read the trained CRF for testing.", null);

	static CommandOption.String testFileOption = new CommandOption.String
	(TUI.class, "test-file", "FILENAME", true, null,
	 "The name of the file containing the testing data.", null);


	static final CommandOption.List commandOptions =
	new CommandOption.List (
		"information extraction on paper header or reference.",
		new CommandOption[] {
			crfInputFileOption,
			testFileOption,
		});


	public static void main (String[] args) throws FileNotFoundException
	{
		commandOptions.process (args);

		IEInterface ieInterface = new IEInterface(crfInputFileOption.value);

		ieInterface.loadCRF();

//		ieInterface.viterbiCRF(testFileOption.value, true, "<NEW_HEADER>");
		ieInterface.viterbiCRF(testFileOption.value, true, "<NEWREFERENCE>");
//		ieInterface.viterbiCRF(testFileOption.value, true, "^$");


//		String str = "G. E. Hinton, T. J. Sejnowski, and D. H. Ackley. Boltzmann machines: Constraint satisfaction networks that learn. Technical Report CMU-CS-84-119, Carnegie-Mellon University, Pittsburg, PA, May 1984.";
//		String retStr = ieInterface.viterbiCRFString(str, true);

//		System.out.println(retStr);
	}
	
}
