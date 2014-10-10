<?php
/* written by bseeger on Oct 2nd 2014
 * This script will take an output file from Analyzer - Analyze Citation In Context Filter.
 * It will parse the file, display the results. 
 */

session_start();

//phpinfo();

/* HELPER FUNCTIONS HERE */
function parseSummaryLine($line) {
	// parse it!

	// filename, total samples, full success, partial email, partial inst, false matches
	$machineOutput = explode(";", $line);

  // s"$pdfFilename;$name;$citationType;$numFoundReferences;$numExpectedCitations;$numFoundCitations;$numExpectedCitations;$numCitationsLinked"// TODO ADD ON HERE

	$curPDF = trim($machineOutput[0], "## ");

  $data = array("FilterName" => $machineOutput[1],  /* throw away 1, name of filter */
                "CitationType" => $machineOutput[2],
                "NumFoundReferences" => $machineOutput[3],
                "NumExpectedReferences" => $machineOutput[4],
                "NumFoundCitations" => $machineOutput[5],
                "NumExpectedCitations" => $machineOutput[6],
                "NumCitationsMatched" => $machineOutput[7],
                "NumReferencesCited" => $machineOutput[8],
                "References" => array(),
                "Citations" => array(),
								"Analysis" => "");

  return array($curPDF, $data);
}

function parseReference($line) { 
  $ref = explode(":", $line);
  $data = array("Id" => trim(@$ref[0]), 
                "Refmarker" => trim(@$ref[1]), 
                "Dates" => trim(@$ref[2]),
                "Authors" => trim(@$ref[3]),
                "Cited" => false);

  return $data; 
} 


function parseFile($filename) {
	$myfile = fopen($filename, "r") or die("Unable to open input file: $filename");

	$inRecord = false;
	$data = array();
	$curPDF = "";
	$atSummary = false;
  $parseRef = false;

	while(!feof($myfile)) {
		// parse it!
		$line = fgets($myfile);
    //echo "$line <br>";	
		if (startsWith("##",$line) && !$inRecord) {
      // the first ## line will have a machine summary on it. Then read until the 
	    // next ## for the whole record. 
      $result = parseSummaryLine($line);
      $curPDF = $result[0];
      $data[$curPDF] = $result[1];
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

		} else if (startsWith("References End", $line)) { 
      $parseRef = false; 

    } else if ((startsWith("References:", $line) || $parseRef) && $inRecord) {
      if ($parseRef) {
        $refData = parseReference($line);
        //print_r($refData);
        $data[$curPDF]['References'][$refData['Id']] = $refData;
        //print_r($data[$curPDF]['References']);
        //$data[$curPDF]['References'][$refData][] = array('Id'=> $refData['Id'], 'Refmarker' => $refData['Refmarker'], 'Authors' => $refData['Authors']);
      }
      $parseRef = true; 
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
  $pos = strpos($haystack,$needle);
  //echo "Looking for '$needle' in '$haystack'  pos: $pos<br>";

	return (($needle === "") || $pos === 0);
}

if (isset($_REQUEST["CLEANUP"])) {
  unset($_SESSION["FileData"]);
  unset($_SESSION["CurrentFileIndex"]);
  unset($_SESSION["Filename"]);
  unset($_SESSION["Directory"]);
  die("Cleaned up!");
}

/* PAGE LOGIC STARTS HERE */
$fileData = @$_SESSION["FileData"];
$curFileIndex = @$_SESSION["CurrentFileIndex"];
$directory = @$_REQUEST["directory"]; // where the files are
$filename = @$_REQUEST["filename"];
$mode = @$_REQUEST["mode"];
$ignore = @$_REQUEST["ignore"];
$msg = "";

if (@$directory) {
  rtrim($directory);
  $_SESSION['Directory'] = $directory;
}

$ignoreFile = $directory . "AnalysisIgnore.txt";

if (!isset($fileData)) {
	if ($filename) {
		$fileData = parseFile(@$directory.'/'.$filename);
		$_SESSION['Filename'] = $filename;
	} else { 
		$fileData = array();
	} 
  
  $curFileIndex = 0;
  //$curFile = key($fileData);

	$_SESSION['FileData'] = $fileData;
  $_SESSION['CurrentFileIndex'] = $curFileIndex;

  // Now weed out the files to ignore
  $iFile = @fopen($ignoreFile, "r");

  if ($iFile) {
    while (!feof($iFile)) {
      $line = fgets($iFile);
      unset($fileData[$line]);
    }

    fclose($iFile);
  }
	
} else {
  if (isset($mode)) {
    if( $mode == "next") {
      $curFileIndex ++;
    } else if ($mode == "back" ) {
      $curFileIndex --;
    } else if ($mode == "ignore" ) {
      // put filename in ignore file. 
      $pdfName = array_keys($fileData)[$curFileIndex];
      file_put_contents($ignoreFile, $pdfName."\n", FILE_APPEND);
      unset($fileData[$pdfName]);
      $_SESSION['FileData'] = $fileData;
    }

    // adjust to make sure in bounds 
    if ($curFileIndex >= count($fileData) ) {
      $curFileIndex = count($fileData) - 1;
    } else if ($curFileIndex < 0) {
      $curFileIndex = 0;
    }

    $_SESSION['CurrentFileIndex'] = $curFileIndex;
  } 
}

$fileData = $_SESSION['FileData'];
$curFile = array_keys($fileData)[$curFileIndex];

$filename = $_SESSION['Filename'];
$directory = $_SESSION['Directory'];

/*
echo "<pre>";
echo print_r($fileData);
echo "</pre>";
*/


function flattenXMLString ($xml) {
  $str = "";
  foreach ($xml as $childElement) {
    $str .= $childElement;
    $str .= flattenXMLString($childElement);
  }

  return $str;
}

function writeReferences($references) { 
  echo "<div class=\"references\">REFERENCES<br>";
  $index = 1;
  foreach ($references as $ref) {
    $id = $ref['Id'];
    $marker = trim($ref['Refmarker']);
    $dates = trim($ref['Dates']);
    $cited = ""; 
    if ($ref['Cited'] == false) {
      $cited = "~NOT CITED~";
    }
    echo "<div id=\"$id\">$cited <br> $index) $id $marker <br>&nbsp;&nbsp; ${ref['Authors']}<br>&nbsp;&nbsp;$dates</div>"; 
    echo "-----------</br>";
    $index++;
  }
  echo "</div>";
}

/*
function writeReferences($xml) {
  $p = xml_parser_create();
  xml_parse_into_struct($p, $xml, $elementArray, $index);
  
  echo "<div class=\"references\">REFERENCES<br>";
  //print_r($elementArray);
  foreach ($elementArray as $element) {
    //echo "<pre>"; 
    //print_r($element) ;
    //echo "</pre>";
    //echo "Element: " + $element['tag'] + "  '" + @$element['value']  + "'";
    switch ($element['tag']) {
      case "REFERENCE":
        switch ($element['type']) { 
          case 'open':
            echo "----------------------------------<br>";
            echo "&nbsp;&nbsp;RefMarker: " + $element['attributes']['REFID'] + "<br>";
            break;
          case 'close':
            echo "<br>";
            break;
          case 'complete': // shouldn't hit this!
            echo "---COMPLETE --- ";
            break;
          default:// ignore
        }
        break;

      case "AUTHOR-FIRST":
      case "AUTHOR-MIDDLE":
        echo trim($element['value']) + "&nsbp; FIRST MIDDLE ";
        break;
      case "AUTHOR-LAST":
        echo trim($element['value']) + "<br>";
        break;
      case "TITLE":
      case "JOURNAL":
        echo "  " + $element['value'] + "<br>";
        break;
      default: 
        echo "Default: " + $element['tag'] + "<br>";
        break;
    }
  }
  echo "<pre>";
  print_r($elementArray);
  echo "</pre></div>";
}*/


function writeXML2($xml) {
  
  $p = xml_parser_create();
  //echo "XML:" .$xml->asXML();
  xml_parse_into_struct($p, $xml, $elementArray, $index);
 //********
 /*
  print_r($index);
  echo "<pre>";
  echo "Element array:";
  print_r($elementArray);
  echo "</pre>";
 */
  $regexStart = "[([{]";
  $regexStop = "[)\]}]";

  echo "<div class=\"docText\">";
  foreach ($elementArray as $element) {

    $eStr = @$element['value'];
    // if there is a value... 
    if (isset($eStr)) {
      $htmlClass = strtolower($element['tag']);

      $split = preg_split( "/$regexStart/", $eStr, -1, PREG_SPLIT_OFFSET_CAPTURE);
      foreach ($split as $piece) {
      
        if ($piece[0] == NULL) continue; 

        // back up one to get the matched char, if not at 0!
        $str = " ";
        $offset = $piece[1] > 0 ? $piece[1] - 1 : 0; 
      
        // does line start with the regex pattern?
        if(strstr($regexStart,$eStr[$offset]) !== false) {
          // it's only the regex, skip it. 
          //echo "Piece: '${piece[0]}'";
          if (strlen(trim($piece[0])) == 1){
            continue;
          }
          $str .= $eStr[$offset];
          if (preg_match("/$regexStop/", $piece[0], $match, PREG_OFFSET_CAPTURE) == 1)  {
            $matchOffset = $match[0][1];  
            $str = $str . substr($piece[0], 0, $matchOffset+1)  . "       " . substr($piece[0], $matchOffset + 1);
          } else {
            $str .= $piece[0];
          }
        } else {
          $str .= $piece[0];
        }
        $str = substr(str_replace("\n", "", $str), 0, 80);
        if ($element['tag'] == "CITATION") {
          $refid = $element['attributes']['REFID'];
          // javascript is non functional here -- needs to be debugged!
          echo "<div class=\"citation\" onmouseover=\"toggleBackground(\"$refid\", \"red\");\" onmouseout=\"toggleBackground(\"$refid\", \"yellow\");\"><pre>$str</pre></div>";
        } else {
          echo "<div class=\"$htmlClass\"><pre>$str</pre></div>";
        }
      }
    }
  }
  echo "</div>";

  xml_parser_free($p);
}

function getBackNextButtons($filename) {
  return "
   	    <!-- <input type=\"submit\" value=\"<--- Back One File\"> -->
				<input type=\"button\" onclick=\"window.location.replace('CICWebAnalysis.php?&filename=$filename&mode=back')\" value=\"<-- Back One File\"/> 
        <input type=\"button\" onclick=\"window.location.replace('CICWebAnalysis.php?&filename=$filename&mode=next')\" value=\"Next File --->\"/>
  ";
}

?>


<!DOCTYPE html>
<html>
  <head> 
     <link rel="stylesheet" type="text/css" href="analysis.css">
  </head>
  <script>
  // there is some bad javascript around here... won't function as expected.  Blah.
  function toggleBackground(id, color) {
    if (id != -1) {
      document.getElementById(id).style.backgroundColor = color;
    }
  }
  </script>
  <body>

<?php
/*
  echo "<pre>";
  print_r($_SESSION);
  print_r($_POST);
  print_r($_GET);
  echo "</pre>";
 */
	$totCitationsMatched = 0;
	$totCitationsNotMatched = 0;
  $totFoundReferences = 0;
  $totExpectedReferences = 0;
  $totFoundCitations = 0;
  $totExpectedCitations = 0;
  $totReferencesCited = 0;

	echo '<form name="input" action="CICWebAnalysis.php" method="post">';
	echo "<input type=\"hidden\" name=\"filename\" value=\"$filename\">";
	echo '<table>';

	if ($msg) { 
		echo "<tr><td colspan=\"2\" class=\"message\">$msg</td></tr>";
	}

  $fileInfo = array();
  $fileSummary = "";

  /* only work on one file */
  $pdfRecord = $fileData[$curFile];

  /* improve this to only do it one time */
 	foreach ($fileData as $pdfName => $pdfRecord) { 
		//echo "<pre>" + print_r($pdfRecord)+ "</pre>";
		$totCitationsMatched += @$pdfRecord["NumCitationsMatched"];
    $totFoundCitations += @$pdfRecord["NumFoundCitations"];
    $totExpectedCitations += @$pdfRecord["NumExpectedCitations"];
    $totFoundReferences += @$pdfRecord["NumFoundReferences"];
    $totExpectedReferences += @$pdfRecord["NumExpectedReferences"];
    $totReferencesCited += @$pdfRecord["NumReferencesCited"];
 /* 
    foreach ($fileData[$curFile]['References'] as $ref) {
      if ($ref['Cited'] == true) {
        $totReferencesCited++;
      }
    }
    */
/*
		if ($pdfName != "Summary") {

			$xmlDirPath = $directory . "/" . $pdfName;
			$pdfDirPath = $directory . "/" . trim($pdfName,".meta.xml");
			$fileInfo[$pdfName] = "<tr><td class=\"pdfName\" colspan=\"2\"><a target=\"_blank\" href=\"$xmlDirPath\">$pdfName</a>  <a target=\"_blank\" href=\"$pdfDirPath\">PDF</a></td></tr>";
			$fileInfo[$pdfName] .= "<tr>
			                   <td class=\"pdfText\"> <pre>";
			$fileInfo[$pdfName] .=	print_r($pdfRecord['Analysis'], true);
			$fileInfo[$pdfName] .= "  </pre> </td>
					<td class=\"pdfAdmin\"> <input type=\"checkbox\" name=\"GoodDocs[]\" value=\"$pdfName\">Keep it?</input></td>
				  </tr>";

		} else {
			$fileSummary = "<tr> <td class=\"pdfName\" colspan=\"2\">Summary From File</td></tr>
			      <tr> <td class=\"pdfText\" colspan=\"2\">
				  <pre>";
			$fileSummary .= print_r($pdfRecord['Analysis'], true);
			$fileSummary .= '</pre> </td> </tr>';
		}
    */
	}	
  
  $xmlFilename = $directory . '/'. $curFile;
  $pdfFilename = $directory . '/'. trim($curFile, ".meta.xml");
  $pstotextFilename = $directory . '/'. trim ($curFile,".meta.xml") . ".pstotext.xml";

	$matchPercentage = $totCitationsMatched / $totFoundCitations * 100;
	$refCitedPercentage = $totReferencesCited / $totFoundReferences * 100;
  $refNotCitedPercentage = ($totFoundReferences  - $totReferencesCited)  / $totFoundReferences * 100;
	$numberFiles = count($fileData);

	if (isset($fileData["Summary"])) { 
		$numberFiles -= 1;
	}


	echo "<tr> <td class=\"pdfName\" colspan=\"2\">Website Summary</td></tr>
          <tr> 
		    <td class=\"pdfText\" colspan=\"2\">
          <div class=\"summary\">
		      <pre><h2> Overall Analysis </h2>
  Total number of files analyzed:   $numberFiles

                     Found:     

       Citations                               $totFoundCitations           
       References                              $totFoundReferences         
       -------------------------------------------------------------
       Citation to Reference Match:            $totCitationsMatched          " .  number_format((float)$matchPercentage, 2, '.', '') ."%
       References Cited:                       $totReferencesCited           " .  number_format((float)$refCitedPercentage, 2, '.', '') . "%

       Average citations per file:             " . number_format($totFoundCitations / $numberFiles, 2, '.', '') . "
       References with no citation found:      ". ($totFoundReferences - $totReferencesCited) . "          " . number_format((float)$refNotCitedPercentage, 2, '.', '') ."%
	          </pre> 
            <div>
		  </td> 
		</tr>
";

 // print the summary from the file
  echo "
    
    <tr>
      <td class=\"pdfName\" colspan=\"2\">Summary From File</td>
    <tr>
    <tr>
      <td class=\"pdfText\" colspan=\"2\">
      <pre>$curFile";

      echo print_r($fileData[$curFile]['Analysis'], true);

  echo "
      </pre>
      </td>
    </tr>
    <tr>
      <td colspan=\"2\" class=\"submit\">" .  getBackNextButtons($filename) . " </td>
		</tr>
    <tr>
      <td class=\"pdfName\">File
        <a target=\"_blank\" href=\"$pstotextFilename\">pstotext</a>&nbsp;&nbsp;&nbsp; <a target=\"_blank\" href=\"$xmlFilename\">$curFile</a>&nbsp;&nbsp;&nbsp;<a target=\"_blank\" href=\"$pdfFilename\">PDF</a></td></tr> </pre>
      </td>
      <td class=\"pdfName\">  
				<input type=\"button\" onclick=\"window.location.replace('CICWebAnalysis.php?&filename=$filename&mode=ignore')\" value=\"Ignore File\"/> 
      </td>
    </tr>

    </table>
    ";

  // now play with body text from file: 

  $xml = simplexml_load_file($directory.'/'.$curFile);
  $body = $xml->xpath('//body');
  $citations = $xml->xpath('//citation');
  $refXML = $xml->xpath('//biblio');


  echo "<div class=\"citations\">";
  $index = 1;
  foreach ($citations as $cit) {
    echo "$index)  \t" . $cit  . " \t\t\t";
    foreach ($cit->attributes() as $name => $value) {
      echo "$name: $value";
      if ($name == "refID" && (String)$value != "-1") {
        $fileData[$curFile]['References'][(String)$value]['Cited'] = true;
      }
    }
    $index ++;
    echo "<br>";
  }
  echo "</div>";

  echo "<div class=\"container\">";

  writeXML2($body[0]->asXML());
  writeReferences($fileData[$curFile]['References']);
	  
  echo "</div>";	
/*
  echo $fileSummary;

  foreach ($fileInfo as $data) {
    echo $data;
  }
  */
  echo "
    <table>
      <tr>
        <td colspan=\"2\" class=\"submit\">" . 
          getBackNextButtons($filename) . " 
        </td>
		  </tr>
		</table>
	  </form>
	";
?>

  </body>
</html>
