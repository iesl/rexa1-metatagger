/**
 * author: ghuang
 */

package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.rexo.pipeline.ErrorLogFilter;
import org.rexo.pipeline.InfoLogFilter;
import org.rexo.pipeline.PipelineMetricsFilter;
import org.rexo.pipeline.components.RxDocumentDirectorySource;
import org.rexo.pipeline.components.RxDocumentQueue;
import org.rexo.pipeline.components.RxDocumentSource;
import org.rexo.pipeline.components.RxPipeline;
import org.rexo.ui.CommandLineOptions;
import org.rexo.ui.LoggerConfigurator;

import edu.umass.cs.mallet.base.fst.CRF4;

public class SegmentationTestTUI
{
	private static Logger log = Logger.getLogger(SegmentationTestTUI.class);

	/* Construct an RxDocumentQueue from the command line options */
	private static RxDocumentSource buildRxDocumentSource(Map argumentMap)
	{
		// create a new source queue
		String inputDirName = (String) argumentMap.get("input.directory");
		String listFileName = (String) argumentMap.get("input.listfile");
		System.out.println("dirName: " + inputDirName);

		RxDocumentSource source;
		if (listFileName == null) {
			RxDocumentQueue documentQueue = new RxDocumentQueue(new File(
					inputDirName));
			documentQueue.setExtensions(new String[] { ".pstotext.xml" });
			source = documentQueue;
		}
		else {
			source = new RxDocumentDirectorySource(new File(listFileName),
					new File(inputDirName));
		}

		if (argumentMap.get("num.documents") != null) {
			long maxDocNum = ((Long) argumentMap.get("num.documents"))
					.longValue();
			source.setMaxDocuments(maxDocNum);
			log.info("Processing first " + maxDocNum + " documents only");
		}
		else {
			log.info("Processing all documents (default)");
		}

		return source;
	}

	/* Construct the Metatagger pipeline from the given RxDocumentQueue and command
	 * line options */
	private static RxPipeline buildPipeline(Map argumentMap) throws IOException, ClassNotFoundException
	{
		RxPipeline pipeline = new RxPipeline();

		CRFBibliographySegmentor crfBibSegmentor = null;

		// init CRF files and target directory
		String refSegSrc = (String) argumentMap.get("crf.ref.segmentor");
		if (refSegSrc != null) {
			log.info("using CRF reference segmentor");
			FileInputStream fis = new FileInputStream(refSegSrc);
			ObjectInputStream ois = new ObjectInputStream(fis);
			CRF4 crf = (CRF4) ois.readObject();
			ois.close();
			crfBibSegmentor = new CRFBibliographySegmentor(crf);
		}
		else {
			log.info("using manual reference segmentor");
		}
		
		if (argumentMap.containsKey("output.directory")) {
			String outputDirName = (String) argumentMap.get("output.directory");
			pipeline.getScope("session").put("output.directory",
					new File(outputDirName));
		}

		if (argumentMap.containsKey("input.directory")) {
			String dirName = (String) argumentMap.get("input.directory");
			pipeline.getScope("session").put("input.directory",
					new File(dirName));
		}

		// handle 'enable-log'
		boolean logp = argumentMap.get("enable.log") != null;
		pipeline.getScope("session").put("log.boolean", new Boolean(logp));
		pipeline.getScope("session").put("log.directory", new File("./log"));
		// initialized in 'LogfileFactory'
		pipeline.getScope("session").put("sessionID.integer", new Integer(-1));

		log.info(logp ? "Logging enabled" : "Logging disabled (default)");

		boolean doReprocess = argumentMap.get("reprocess.documents") != null;
		pipeline.getScope("session").put("reprocess.boolean",
				new Boolean(doReprocess));
		if (doReprocess) {
			System.out.println("Reprocessing documents");
		}

		// handle 'continuous execution'
		boolean continuousExecution = argumentMap.get("continuous.execution") != null;
		pipeline.getScope("session").put("continuous.execution.boolean", Boolean.valueOf(continuousExecution));

		// construct pipeline
		// standard pipeline
		pipeline.addStandardFilters();

		if (refSegSrc == null) 
			pipeline.add(new org.rexo.referencetagging.SegmentationFilter());
		else 
			pipeline.add(new edu.umass.cs.rexo.ghuang.segmentation.SegmentationFilter(crfBibSegmentor));

		pipeline.add(new WriteSegmentedPlainTextFilter());

		if (logp) {
			pipeline
			// log document errors to '.list' and '.html'
					.addErrorFilters()
					.add(new ErrorLogFilter())
					.addEpilogueFilters()
					.add(new InfoLogFilter())
					.add(new PipelineMetricsFilter());
		}

		return pipeline;
	}

	/**
	 * Run the meta-tagger pipeline
	 * @throws  
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception
	{
		try {
			LoggerConfigurator.configure(SegmentationTestTUI.class);
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}

		Map initProperties;
		CommandLineOptions cli = null;

		try {
			cli = new CommandLineOptions()
			{
				protected void createOptions()
				{
					getOptions()
							.addOption(
									OptionBuilder
											.withLongOpt("continuous")
											.withDescription(
													"re-run on input directory until stop file is created")
											.create());

					getOptions()
							.addOption(
									OptionBuilder.withLongOpt("enable-log")
											.withDescription("enable logging")
											.create());

					getOptions().addOption(
							OptionBuilder.withLongOpt("input-directory")
									.hasArg().create());

					getOptions().addOption(
							OptionBuilder.withLongOpt("input-list").hasArg()
									.create());

					getOptions().addOption(
							OptionBuilder.withLongOpt("output-directory")
									.hasArg().create());

					getOptions().addOption(
							OptionBuilder.withLongOpt("ref-segmentation-crf")
									.hasArg().create());

					getOptions().addOption(
							OptionBuilder.withLongOpt("db-version").hasArg()
									.create());
				}

				protected Object parseOptions(CommandLine commandLine,
						Object object)
				{
					Map initProperties = new HashedMap();

					if (commandLine.hasOption("continuous")) {
						initProperties.put("continuous.execution", "true");
					}
					if (commandLine.hasOption("ref-segmentation-crf")) {
						initProperties.put("crf.ref.segmentor", commandLine
								.getOptionValue("ref-segmentation-crf"));
					}
					initProperties.put("input.directory", commandLine
							.getOptionValue("input-directory"));
					initProperties.put("input.listfile", commandLine
							.getOptionValue("input-list"));
					initProperties.put("output.directory", commandLine
							.getOptionValue("output-directory"));

					if (commandLine.hasOption("enable-log")) {
						initProperties.put("enable.log", "true");
					}
					if (commandLine.hasOption("num-documents")) {
						initProperties.put("num.documents", new Long(
								commandLine.getOptionValue("num-documents")));
					}
					return initProperties;
				}
			};

			initProperties = (Map) cli.parse(args, SegmentationTestTUI.class);
		}
		catch (Exception e) {
			log.error(e.getClass().getName() + ": " + e.getMessage());
			if (cli != null) {
				log.info("\n" + cli.getHelpString(SegmentationTestTUI.class));
			}
			return;
		}

		// Create the pipeline
		RxPipeline pipeline = buildPipeline(initProperties);

		// Create a source queue
		RxDocumentSource source = buildRxDocumentSource(initProperties);

		pipeline.setInputSource(source);

		System.out.print("Starting pipeline: ");
		System.out.println(pipeline.toString());
		pipeline.execute();
	}
}
