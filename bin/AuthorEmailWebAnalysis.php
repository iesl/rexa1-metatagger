<?php
/* written by bseeger on Sept 16th, 2014 
 * This script will take an output file from Analyzer - Analyze Author Email Tagging Filter.
 * It will parse the file, display the results and allow you to trim out 
 * results that don't make sense. Then you can save a file of pdf file names that you might to keep 
 * in your collection. 
 */
session_start();

//phpinfo();

/* HELPER FUNCTIONS HERE */

function parseFile($filename) {
	$myfile = fopen($filename, "r") or die("Unable to open input file");

	$inRecord = false;
	$data = array();
	$curPDF = "";
	$atSummary = false;

	while(!feof($myfile)) {
		// parse it!
		$line = fgets($myfile);
		
		if (startsWith("##",$line) && !$inRecord) {
			// the first ## line will have a machine summary on it. Then read until the 
			// next ## for the whole record. 
			// parse it!

			// filename, total samples, full success, partial email, partial inst, false matches
			$machineOutput = explode(";", $line);
			$curPDF = trim($machineOutput[0], "## ");
			$data[$curPDF] = array("TotalSamples" => $machineOutput[2],  /* throw away 1, name of filter */
								   "FullSuccess" => $machineOutput[3],
							 	   "PartialEmail" => $machineOutput[4],
							 	   "PartialInst" => $machineOutput[5],
								   "FalseMatches" => $machineOutput[6],
								   "Analysis" => "");
			$inRecord = true;
		} else if (startsWith("##", $line)) {
			// finish the record
			$inRecord = false;
			$curPDF = "";
	    } else if (startsWith("-----", $line)) {
			//Summary section!
			if ($atSummary == false) {
				$atSummary = true;
				$curPDF = "Summary";
				$data[$curPDF] = array("Analysis" => "");	 
			}  // else we are done! we will hit this at the closing ---- but don't need to do anything. 
		} else {
			// store the line output
			if ($curPDF !== "") {
				$data[$curPDF]["Analysis"] .= $line;
			} else {
				error_log("No where to put line data");
			}
		}
	}

	fclose($myfile);

	return $data;
}

function startsWith($needle, $haystack) {
	return (($needle === "") || strpos($haystack, $needle) === 0);
}

/* PAGE LOGIC STARTS HERE */

$directory = @$_REQUEST["directory"]; // where the files are
$filename = @$_REQUEST["filename"];
$mode = @$_REQUEST["mode"];
$msg = "";

if ($mode == "SaveTrimmedList") { 
	$outFile = fopen("/tmp/TrimmedFileList.txt", 'w');

 	foreach (array_keys($_SESSION['FileData']) as $pdfName) {
		fwrite($outFile, "$pdfName\n");
	}

	fclose($outFile);

	$msg = "Trimmed file list saved to file: /tmp/TrimmedFileList.txt";

} else if (isset($_POST['GoodDocs'])) {
	foreach ($_POST['GoodDocs'] as $pdfName) {
		$fileData[$pdfName] = $_SESSION['FileData'][$pdfName];
	}
	$_SESSION['FileData'] = $fileData;	
	$msg = "File list trimmed successfully";

} else {

	if ($filename) {
		$fileData = parseFile($filename);
		$_SESSION['Filename'] = $filename;
	} else { 
		$fileData = array();
	} 

	$_SESSION['FileData'] = $fileData;

	if (isset($directory)) {
		rtrim($directory, "/");
		$_SESSION['Directory'] = $directory;
	}
}
$filename = $_SESSION['Filename'];
$directory = $_SESSION['Directory'];
$fileData = $_SESSION['FileData'];

?>


<!DOCTYPE html>
<html>
  <head> 
     <link rel="stylesheet" type="text/css" href="analysis.css">
  </head>
  <body>

<?php
/*
  echo "<pre>";
  print_r($_SESSION);
  print_r($_POST);
  print_r($_GET);
  echo "</pre>";
 */
 	$totalSamples = 0;
	$totFullSuccess = 0;
	$totPartialEmail = 0;
	$totPartialInst = 0;
	$totFalseMatches = 0;

	echo '<form name="input" action="AuthorEmailWebAnalysis.php" method="post">';
	echo "<input type=\"hidden\" name=\"filename\" value=\"$filename\">";
	echo '<table>';

	if ($msg) { 
		echo "<tr><td colspan=\"2\" class=\"message\">$msg</td></tr>";
	}

 	foreach ($fileData as $pdfName => $pdfRecord) { 
		//echo "<pre>" + print_r($pdfRecord)+ "</pre>";
		$totalSamples += @$pdfRecord["TotalSamples"];
		$totFullSuccess += @$pdfRecord["FullSuccess"];
		$totPartialEmail += @$pdfRecord["PartialEmail"];
		$totPartialInst += @$pdfRecord["PartialInst"];
		$totFalseMatches += @$pdfRecord["FalseMatches"];

		if ($pdfName != "Summary") {
			$xmlDirPath = $directory . "/" . $pdfName;
			$pdfDirPath = $directory . "/" . trim($pdfName,".meta.xml");
			echo "<tr><td class=\"pdfName\" colspan=\"2\"><a target=\"_blank\" href=\"$xmlDirPath\">$pdfName</a>  <a target=\"_blank\" href=\"$pdfDirPath\">PDF</a></td></tr>";
			echo "<tr>
			 		<td class=\"pdfText\"> <pre>";
					print_r($pdfRecord['Analysis']);
			echo "  </pre> </td>
					<td class=\"pdfAdmin\"> <input type=\"checkbox\" name=\"GoodDocs[]\" value=\"$pdfName\">Keep it?</input></td>
				  </tr>";

		} else {
			echo "<tr> <td class=\"pdfName\" colspan=\"2\">Summary From File</td></tr>
			      <tr> <td class=\"pdfText\" colspan=\"2\">
				  <pre>";
				print_r($pdfRecord['Analysis']);
			echo '</pre> </td> </tr>';
		}
	}	

	$matchPercentage = $totFullSuccess / $totalSamples * 100;
	$numberFiles = count($fileData);

	if (isset($fileData["Summary"])) { 
		$numberFiles -= 1;
	}

	echo "<tr> <td class=\"pdfName\" colspan=\"2\">Website Summary</td></tr>
          <tr> 
		    <td class=\"pdfText\" colspan=\"2\">
		      <pre>
Total number of files analyzed:  $numberFiles
Complete Author/Email/Institute Matches: $totFullSuccess $matchPercentage%
Total number of authors:  $totalSamples
Average authors per file: " . $totalSamples / $numberFiles . "
Partial Match - Email:  $totPartialEmail  " . $totPartialEmail / $totalSamples * 100 . "%
Partial Match - Inst:  $totPartialInst  " . $totPartialInst / $totalSamples * 100 . "%";

	echo "
	          </pre> 
		  </td> 
		</tr>

		<tr><td colspan=\"2\" class=\"submit\">
   	            <input type=\"submit\" value=\"Trim List\">
				<input type=\"button\" onclick=\"window.location.replace('AuthorEmailWebAnalysis.php?mode=Start&filename=$filename')\" value=\"Reset\" />
				<input type=\"button\" onclick=\"window.location.replace('AuthorEmailWebAnalysis.php?mode=SaveTrimmedList&filename=$filename')\" value=\"Save PDF Names To File\" />
	   	     </td>
		  </tr>
		</table>
	  </form>
	";
?>

  </body>
</html>
