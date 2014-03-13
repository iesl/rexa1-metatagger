package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.logging.Logger;

import edu.umass.cs.mallet.base.util.CommandOption;
import edu.umass.cs.mallet.base.util.MalletLogger;

/**
 * 
 * @author ghuang
 *
 */
public class CheckDuplicateInstancesTUI
{
    private static CommandOption.File labeledDataOption = new CommandOption.File
    (CheckDuplicateInstancesTUI.class, "labeled-data", null, true, null,
    		"file containing labeled data in the \"list of lines\" format..", "");
    
    private static CommandOption.Integer numComparisonLinesOption = new CommandOption.Integer
    (CheckDuplicateInstancesTUI.class, "num-comparison-lines", null, true, 10,
    		"number of initial lines in each instance used to check duplicates..", "");
	
    private static final Logger logger = MalletLogger.getLogger (CheckDuplicateInstancesTUI.class.getName ());
	
	public static void main(String[] args) throws Exception
	{
		CommandOption.List options = new CommandOption.List ("", new CommandOption[0]);
		options.add (CheckDuplicateInstancesTUI.class);
		options.process (args);
		options.logOptions (logger);

		BufferedReader reader = new BufferedReader(new FileReader(labeledDataOption.value));
		String line = null;
		Hashtable firstLines = new Hashtable();
		String firstInstLines = "";
		int instLinesRead = 0;
		final int numComparisonLines = numComparisonLinesOption.value;
		int idx = 0;

		while ((line = reader.readLine()) != null) {
			line = new StringBuffer(line).reverse().toString();
			String[] parts = line.split("@", 2);
			idx++;

			if (parts.length == 1) {
				instLinesRead = 0;
				firstInstLines = "";
				continue;
			}
			else if (instLinesRead >= numComparisonLines) {
				continue;
			}

			firstInstLines += new StringBuffer(parts[1]).reverse().toString();
			instLinesRead++;
			
			if (instLinesRead == numComparisonLines) {
				if (firstLines.containsKey(firstInstLines)) {
					int dupStart = idx - numComparisonLines + 1;
					System.out.println("Possibly duplicate instance @ " + dupStart + ", " + firstLines.get(firstInstLines) + ":\nfirst lines=" + firstInstLines + "\n");
				}
				else {
					Integer val = new Integer(idx - numComparisonLines + 1);
					firstLines.put(firstInstLines, val);
				}
			}

		}
	}

}