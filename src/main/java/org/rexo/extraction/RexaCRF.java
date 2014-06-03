package org.rexo.extraction;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import edu.umass.cs.mallet.base.fst.CRF4;
import edu.umass.cs.mallet.base.pipe.Noop;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.FeatureSequence;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.InstanceList;

/** 
    @author Gary Huang <a href="mailto:ghuang@cs.umass.edu">ghuang@cs.umass.edu</a>
*/
public class RexaCRF extends CRF4 implements Serializable
{	
	private static final long serialVersionUID = 1L;


	public RexaCRF (Pipe inputPipe, Pipe outputPipe)
    {
        super(inputPipe, outputPipe);
    }

	public RexaCRF (CRF4 other)
	{
		super(other);
	}

    /** Add states to create a first-order Markov model on labels, treating the label 
        sequence in each instance as a separate path. If a label sequence (x1, x2) 
        is not present in the given instances, then the state transition from x1 to x2 is 
        not allowed.

        For example, given just two instances with label sequences (x1, x2, x3) and 
        (x1, x3, x2), the sequence (x1, x2, x3, x2) is not allowed.

        Parameters are tied on transitions between the same labels.
    */
    public void addStatesForLabelTrajectoriesAsIn (InstanceList trainingSet, String startStateName)
    {
        HashMap stateName2destInfo = new HashMap(); // name --> dest name --> [consumed label, weight name]
        HashMap stateName2costs = new HashMap(); // name --> [init, final]

        stateName2destInfo.put(startStateName, new HashMap());
        stateName2costs.put(startStateName, new double[]{ ZERO_COST, INFINITE_COST });

        for (int ii = 0; ii < trainingSet.size(); ii++) {
	  Instance instance = trainingSet.getInstance(ii);

	  System.out.println("%%% inst name=" + instance.getName());

	  FeatureSequence labels = (FeatureSequence) instance.getTarget();
	  // Define state name to be the concatenation of the instance 
	  // index, label name, and index of the label in the sequence
	  String sourceLabel = (String) labels.get(0);
	  String sourceStateName = "<" + ii + "," + sourceLabel + ",0>";
	  boolean selfTransition = false;

	  // Update the initial transition
	  HashMap destInfo = (HashMap) stateName2destInfo.remove(startStateName);
	  String weightName = startStateName + "->" + sourceLabel + ":" + sourceLabel;

	  destInfo.put(sourceStateName, new String[]{ sourceLabel, weightName });
	  stateName2destInfo.put(startStateName, destInfo);

	  for (int i = 0; i < labels.size(); i++)
	      System.out.println("%%% " + labels.get(i));
	  System.out.println("%%% " + startStateName + " to " + sourceStateName + " consuming " + sourceLabel + " using wt " + weightName);

	  // Update the rest of the transitions
	  for (int li = 1; li < labels.size(); li++) {
	      String destLabel = (String) labels.get(li);
	      String destStateName = "<" + ii + "," + destLabel + "," + li + ">";

	      // Don't distinguish transitions b/w the same 
	      // labels but add a self-transition if needed
	      if (! sourceLabel.equals(destLabel)) {

		if (stateName2destInfo.get(sourceStateName) == null)
		    stateName2destInfo.put(sourceStateName, new HashMap());

		if (stateName2costs.get(sourceStateName) == null)
		    stateName2costs.put(sourceStateName, new double[]{ INFINITE_COST, INFINITE_COST });

		destInfo = (HashMap) stateName2destInfo.remove(sourceStateName);
		weightName = sourceLabel + "->" + destLabel + ":" + destLabel;
		destInfo.put(destStateName, new String[]{ destLabel, weightName });

		System.out.println("%%% " + sourceStateName + " to " + destStateName + " consuming " + destLabel + " using wt " + weightName + " finalCost=inf");

		if (selfTransition) {
		    weightName = sourceLabel + "->" + sourceLabel + ":" + sourceLabel;
		    destInfo.put(sourceStateName, new String[]{ sourceLabel, weightName });

		    System.out.println("%%% " + sourceStateName + " to " + sourceStateName + " consuming " + sourceLabel + " using wt " + weightName + " finalCost=inf");
		}
	
		stateName2destInfo.put(sourceStateName, destInfo);

		sourceLabel = destLabel;
		sourceStateName = destStateName;
		selfTransition = false;
	      }
	      else
		selfTransition = true;

	      // Last state in the sequence always has a self-transition
	      if (li == labels.size() - 1) {
		if (stateName2destInfo.get(sourceStateName) == null)
		    stateName2destInfo.put(sourceStateName, new HashMap());

		stateName2costs.put(sourceStateName, new double[]{ INFINITE_COST, ZERO_COST });
		destInfo = (HashMap) stateName2destInfo.remove(sourceStateName);
		weightName = sourceLabel + "->" + sourceLabel + ":" + sourceLabel;
		destInfo.put(sourceStateName, new String[]{ sourceLabel, weightName });

		System.out.println("%%% " + sourceStateName + " to " + sourceStateName + " consuming " + sourceLabel + " using wt " + weightName + " finalCost=0");
		    
		stateName2destInfo.put(sourceStateName, destInfo);
	      }

	  }
        }

        // Add all the accumulated states
        for (Iterator iter = stateName2destInfo.keySet().iterator(); iter.hasNext(); ) {
	  String sourceStateName = (String) iter.next();
	  double[] costs = (double[]) stateName2costs.get(sourceStateName);
	  double initialCost = costs[0];
	  double finalCost = costs[1];
	  HashMap destInfo = (HashMap) stateName2destInfo.get(sourceStateName);
	  String[] destStateNames = new String[destInfo.size()];
	  String[] destLabels = new String[destInfo.size()];
	  String[] weightNames = new String[destInfo.size()];
	  int count = 0;

	  for (Iterator iter2 = destInfo.keySet().iterator(); iter2.hasNext(); ) {
	      String destStateName = (String) iter2.next();
	      String[] stuff = (String[]) destInfo.get(destStateName);

	      destStateNames[count] = destStateName;
	      destLabels[count] = stuff[0];
	      weightNames[count] = stuff[1];
	      count++;
	  }

	  addState(sourceStateName, initialCost, finalCost, destStateNames, destLabels, weightNames);
        }
    }


    // Collapses all author-* states
    public void addStatesForLabelTrajectoriesAsInCollapseAuthors (InstanceList trainingSet, String startStateName)
    {
        HashMap stateName2destInfo = new HashMap(); // name --> dest name --> [consumed label, weight name]
        HashMap stateName2costs = new HashMap(); // name --> [init, final]

        stateName2destInfo.put(startStateName, new HashMap());
        stateName2costs.put(startStateName, new double[]{ ZERO_COST, INFINITE_COST });

        for (int ii = 0; ii < trainingSet.size(); ii++) {
	  Instance instance = trainingSet.getInstance(ii);

	  System.out.println("%%% inst name=" + instance.getName());

	  FeatureSequence labels = (FeatureSequence) instance.getTarget();
	  // Define state name to be the concatenation of the instance 
	  // index, label name, and index of the label in the sequence
	  String sourceLabel = (String) labels.get(0);
	  String sourceStateName = sourceLabel.indexOf("author") == 0 ? sourceLabel : "<" + ii + "," + sourceLabel + ",0>";
	  boolean selfTransition = false;

	  // Update the initial transition
	  HashMap destInfo = (HashMap) stateName2destInfo.remove(startStateName);
	  String weightName = startStateName + "->" + sourceLabel + ":" + sourceLabel;

	  destInfo.put(sourceStateName, new String[]{ sourceLabel, weightName });
	  stateName2destInfo.put(startStateName, destInfo);

	  for (int i = 0; i < labels.size(); i++)
	      System.out.println("%%% " + labels.get(i));
	  System.out.println("%%% " + startStateName + " to " + sourceStateName + " consuming " + sourceLabel + " using wt " + weightName);

	  // Update the rest of the transitions
	  for (int li = 1; li < labels.size(); li++) {
	      String destLabel = (String) labels.get(li);
	      String destStateName = destLabel.indexOf("author") == 0 ? destLabel : "<" + ii + "," + destLabel + "," + li + ">";

	      // Don't distinguish transitions b/w the same 
	      // labels but add a self-transition if needed
	      if (! sourceLabel.equals(destLabel)) {

		if (stateName2destInfo.get(sourceStateName) == null)
		    stateName2destInfo.put(sourceStateName, new HashMap());

		if (stateName2costs.get(sourceStateName) == null)
		    stateName2costs.put(sourceStateName, new double[]{ INFINITE_COST, INFINITE_COST });

		destInfo = (HashMap) stateName2destInfo.remove(sourceStateName);
		weightName = sourceLabel + "->" + destLabel + ":" + destLabel;
		destInfo.put(destStateName, new String[]{ destLabel, weightName });

		System.out.println("%%% " + sourceStateName + " to " + destStateName + " consuming " + destLabel + " using wt " + weightName + " finalCost=inf");

		if (selfTransition) {
		    weightName = sourceLabel + "->" + sourceLabel + ":" + sourceLabel;
		    destInfo.put(sourceStateName, new String[]{ sourceLabel, weightName });

		    System.out.println("%%% " + sourceStateName + " to " + sourceStateName + " consuming " + sourceLabel + " using wt " + weightName + " finalCost=inf");
		}
	
		stateName2destInfo.put(sourceStateName, destInfo);

		sourceLabel = destLabel;
		sourceStateName = destStateName;
		selfTransition = false;
	      }
	      else
		selfTransition = true;

	      // Last state in the sequence always has a self-transition
	      if (li == labels.size() - 1) {
		if (stateName2destInfo.get(sourceStateName) == null)
		    stateName2destInfo.put(sourceStateName, new HashMap());

		stateName2costs.put(sourceStateName, new double[]{ INFINITE_COST, ZERO_COST });
		destInfo = (HashMap) stateName2destInfo.remove(sourceStateName);
		weightName = sourceLabel + "->" + sourceLabel + ":" + sourceLabel;
		destInfo.put(sourceStateName, new String[]{ sourceLabel, weightName });

		System.out.println("%%% " + sourceStateName + " to " + sourceStateName + " consuming " + sourceLabel + " using wt " + weightName + " finalCost=0");
		    
		stateName2destInfo.put(sourceStateName, destInfo);
	      }

	  }
        }

        // Add all the accumulated states
        for (Iterator iter = stateName2destInfo.keySet().iterator(); iter.hasNext(); ) {
	  String sourceStateName = (String) iter.next();
	  double[] costs = (double[]) stateName2costs.get(sourceStateName);
	  double initialCost = costs[0];
	  double finalCost = costs[1];
	  HashMap destInfo = (HashMap) stateName2destInfo.get(sourceStateName);
	  String[] destStateNames = new String[destInfo.size()];
	  String[] destLabels = new String[destInfo.size()];
	  String[] weightNames = new String[destInfo.size()];
	  int count = 0;

	  for (Iterator iter2 = destInfo.keySet().iterator(); iter2.hasNext(); ) {
	      String destStateName = (String) iter2.next();
	      String[] stuff = (String[]) destInfo.get(destStateName);

	      destStateNames[count] = destStateName;
	      destLabels[count] = stuff[0];
	      weightNames[count] = stuff[1];
	      count++;
	  }

	  addState(sourceStateName, initialCost, finalCost, destStateNames, destLabels, weightNames);
        }
    }

    /**
       Create specilaized state cliques for the state trajectories 
       for reference tagging of Rexa data

       Weights for transitions b/w the same two labels are shared in the same clique
       but not across different cliques.

       Every state's final cost is 0 except for the specified start state
    */
    public void addStateCliquesForRexaReferences (String startStateName)
    {
        HashMap stateName2destInfo = new HashMap(); // name --> dest name --> [consumed label, weight name]
        HashMap stateName2costs = new HashMap(); // name --> [init, final]
        HashMap destInfo = new HashMap();
        String weightName, srcState, destState;

        // Take care of the starting states
        String[] initStates = new String[]{ "ref-marker", "authors", "author", "author-last", "author-first" };

        for (int si = 0; si < initStates.length; si++) {
	  weightName = startStateName + "->" + initStates[si] + ":" + initStates[si];
	  destInfo.put(initStates[si], new String[]{ initStates[si], weightName });
        }
        
        stateName2destInfo.put(startStateName, destInfo);
        stateName2costs.put(startStateName, new double[]{ ZERO_COST, INFINITE_COST });

        // Create a clique for the initial part of references, which contains the ref-marker and author tags
        String[] clique0States = new String[]{ "ref-marker", "authors", "author", "author-first", "author-middle", "author-last", "author-affix" };
        
        for (int si = 0; si < clique0States.length; si++) {
	  srcState = clique0States[si];
	  destInfo = new HashMap();

	  for (int si2 = 0; si2 < clique0States.length; si2++) {
	      destState = clique0States[si2];
	      weightName = srcState + "->" + destState + ":" + destState;
	      destInfo.put(destState, new String[]{ destState, weightName });
	  }
	  stateName2destInfo.put(srcState, destInfo);
	  stateName2costs.put(srcState, new double[]{ INFINITE_COST, ZERO_COST });
        }

        // Create a specialized state clique for each general reference type
        String[] seriesConsumedLabels = new String[]{ "address", "booktitle", "conference", "date", "editor", "note", "pages", "publisher", "series", "title", "volume" };
        String[] seriesStartLabels = new String[]{ "booktitle", "title", "date" };

        String[] thesisConsumedLabels = new String[]{ "address", "date", "institution", "thesis", "title", "volume" };
        String[] thesisStartLabels = new String[]{ "title", "date" };

        String[] techConsumedLabels = new String[]{ "address", "date", "institution", "journal", "note", "tech", "title", "web" };
        String[] techStartLabels = new String[]{ "title", "tech", "date", "institution" };

        String[] titleOnlyConsumedLabels = new String[]{ "address", "date", "institution", "note", "publisher", "title", "volume", "web" };
        String[] titleOnlyStartLabels = new String[]{ "title", "note", "date", "institution", "web" };

        String[] booktitleConsumedLabels = new String[]{ "address", "booktitle", "date", "editor", "institution", "note", "pages", "publisher", "title", "volume" };
        String[] booktitleStartLabels = new String[]{ "booktitle", "date", "title", "editor" };

        String[] conferenceConsumedLabels = new String[]{ "address", "conference", "date", "editor", "institution", "journal", "note", "number", "pages", "publisher", "title", "volume", "web" };
        String[] conferenceStartLabels = new String[]{ "date", "title" };

        String[] journalConsumedLabels = new String[]{ "address", "date", "institution", "journal", "note", "number", "pages", "title", "volume", "web" };
        String[] journalStartLabels = new String[]{ "journal", "date", "title" };

        String[] cliqueNames = new String[]{ "[series]-", "[thesis]-", "[tech]-", "[titleOnly]-", "[booktitle]-", "[conference]-", "[journal]-" };
        String[][] consumedLabels = new String[][]{ seriesConsumedLabels, thesisConsumedLabels, techConsumedLabels, titleOnlyConsumedLabels, booktitleConsumedLabels, conferenceConsumedLabels, journalConsumedLabels };
        String[][] startLabels = new String[][]{ seriesStartLabels, thesisStartLabels, techStartLabels, titleOnlyStartLabels, booktitleStartLabels, conferenceStartLabels, journalStartLabels };
        
        // For each general reference type
        for (int ci = 0; ci < consumedLabels.length; ci++) {
	  String[] cliqueBeginLabels = startLabels[ci];
	 
	  // Link its starting states to the ref-marker/authors clique
	  for (int a = 0; a < clique0States.length; a++) {
	      srcState = clique0States[a];
	      destInfo = (HashMap) stateName2destInfo.remove(srcState);
	      assert(destInfo != null);

	      for (int b = 0; b < cliqueBeginLabels.length; b++) {
		destState = cliqueNames[ci] + cliqueBeginLabels[b];
		weightName = srcState + "->" + destState + ":" + cliqueBeginLabels[b];
		destInfo.put(destState, new String[]{ cliqueBeginLabels[b], weightName });
	      }
	      stateName2destInfo.put(srcState, destInfo);
	  }

	  // Create a clique of all states involved in the type
	  String[] cliqueLabels = consumedLabels[ci];
	  for (int a = 0; a < cliqueLabels.length; a++) {
	      srcState = cliqueNames[ci] + cliqueLabels[a];
	      
	      if (stateName2destInfo.get(srcState) == null) {
		stateName2destInfo.put(srcState, new HashMap());
		stateName2costs.put(srcState, new double[]{ INFINITE_COST, ZERO_COST });
	      }
	      
	      destInfo = (HashMap) stateName2destInfo.remove(srcState);
	      
	      for (int b = 0; b < cliqueLabels.length; b++) {
		destState = cliqueNames[ci] + cliqueLabels[b];
		weightName = srcState + "->" + destState + ":" + cliqueLabels[b];
		destInfo.put(destState, new String[]{ cliqueLabels[b], weightName });
	      }
	      stateName2destInfo.put(srcState, destInfo);
	  }
        }

        // Add all the accumulated states
        for (Iterator iter = stateName2destInfo.keySet().iterator(); iter.hasNext(); ) {
	  String sourceStateName = (String) iter.next();	 
	  double[] costs = (double[]) stateName2costs.get(sourceStateName);
	  double initialCost = costs[0];
	  double finalCost = costs[1];
	  destInfo = (HashMap) stateName2destInfo.get(sourceStateName);
	  String[] destStateNames = new String[destInfo.size()];
	  String[] destLabels = new String[destInfo.size()];
	  String[] weightNames = new String[destInfo.size()];
	  int count = 0;

	  for (Iterator iter2 = destInfo.keySet().iterator(); iter2.hasNext(); ) {
	      String destStateName = (String) iter2.next();
	      String[] stuff = (String[]) destInfo.get(destStateName);

	      destStateNames[count] = destStateName;
	      destLabels[count] = stuff[0];
	      weightNames[count] = stuff[1];
	      count++;
	  }

	  addState(sourceStateName, initialCost, finalCost, destStateNames, destLabels, weightNames);
        }
    }

    public void addStatesForReferencesFromRegex ()
    {				
        // journal: (^r?a?t?ji?[vudgnlp]*w?$) | (^r?a?dt?j[vudgnlp]*$)
        // conference: (^r?atn?e?cy?[eigpuvldn]*w?$) | (^r?adt?cy?e?[lgpnvu]*$) | (^r?adtec[igpuvldn]*$)
        // booktitle: (^r?abt?[plndyi]*$) | (^r?adt?b[yelpvgn]*$) | (^r?adteb[plgn]*$) 
        //            | (^r?ate?b[lpvgd]*$) | (^r?atb[epdglyv]*$)
        // other stuff: (^r?aty?[vgnlid]*w?$) | (^r?adt[npwvli]*$) | (^r?ad?[nw]*$) | (^r?t?w?d?$)
        // tech report: (^r?athil?d?n?w?$) | (^r?athdi?n?w?$) | (^r?adth[ilnw]*$) | (^r?ah[twd]*$)
        // thesis: (^r?atsi?l?dv?$) | (^r?adtsil?$)
        String[] regExprs = new String[] {"0r?[123456]*t?ji?[vudgnlp]*w?",
				  "0r?[123456]*Dt?j[vudgnlp]*", // careful: D == d

				  "0r?[123456]*tN?E?cy?[eigpuvldn]*w?", // careful: N == n, E == e
				  "0r?[123456]*dt?cy?e?[lgpnvu]*",
				  "0r?[123456]*dtec[igpuvldn]*",
										  
				  "0r?[123456]*bt?[plndyi]*",
				  "0r?[123456]*dt?b[yelpvgn]*",
				  "0r?[123456]*dteb[plgn]*",
				  "0r?[123456]*te?b[lpvgd]*",
				  "0r?[123456]*tb[epdglyv]*",
										  
				  "0r?[123456]*ty?[vgnlid]*w?",
				  "0r?[123456]*dt[npwvli]*",
				  "0r?[123456]*d?[nw]*",
				  "0r?t?w?d?",
										  
				  "0r?[123456]*thil?d?n?w?",
				  "0r?[123456]*thdi?n?w?",
				  "0r?[123456]*dth[ilnw]*",
				  "0r?[123456]*h[twd]*",
										  
				  "0r?[123456]*tsi?l?dv?",
				  "0r?[123456]*dtsil?" };

        String[] prefixes = new String[] { "journal_1-",
				   "journal_2-",
				   "conference_1-",
				   "conference_2-",
				   "conference_3-",
				   "booktitle_1-",
				   "booktitle_2-",
				   "booktitle_3-",
				   "booktitle_4-",
				   "booktitle_5-",
				   "others_1-",
				   "others_2-",
				   "others_3-",
				   "others_4-",
				   "tech_1-",
				   "tech_2-",
				   "tech_3-",
				   "tech_4-",
				   "thesis_1-",
				   "thesis_2-" };

        // Combine destination info from all regex
        HashMap destMap = new HashMap();
        HashSet startSet = new HashSet();
        HashSet finalSet = new HashSet();
        for (int i = 0; i < regExprs.length; i++) {
	  HashMap char2name = getDefaultChar2StateMap(prefixes[i]);

	  if (prefixes[i].equals("journal_2-")) {
	      char2name.put("D", "journal_2a-date");
	  }
	  else if (prefixes[i].equals("conference_1-")) {
	      char2name.put("N", "conference_1a-note");
	      char2name.put("E", "conference_1a-editor");
	  }

	  Object[] stuff = getDestinationStatesFromRegularExpression(regExprs[i]);
	  HashMap partialDestMap = (HashMap) stuff[0];
	  HashSet partialStartSet = (HashSet) stuff[1];
	  HashSet partialFinalSet = (HashSet) stuff[2];
			
	  System.out.println("### start set = " + partialStartSet);
	  System.out.println("### final set = " + partialFinalSet);

	  for (Iterator iter = partialDestMap.keySet().iterator(); iter.hasNext(); ) {
	      String srcLetter = (String) iter.next();
	      HashSet partialDestSet = (HashSet) partialDestMap.get(srcLetter);
	      String srcState = (String) char2name.get(srcLetter);
	      HashSet destSet = (HashSet) destMap.remove(srcState);
	      if (destSet == null)
		destSet = new HashSet();

	      System.out.println("### src = " + srcState);
	      for (Iterator iter2 = partialDestSet.iterator(); iter2.hasNext(); ) {
		String destLetter = (String) iter2.next();
		String destState = (String) char2name.get(destLetter);
		destSet.add(destState);
		System.out.println("###\t\t" + destState);
	      }
	      destMap.put(srcState, destSet);

	      if (partialStartSet.contains(srcLetter))
		startSet.add(srcState);
	      if (partialFinalSet.contains(srcLetter))
		finalSet.add(srcState);
	  }
        }

        // Add all the accumulated state transitions
        for (Iterator iter = destMap.keySet().iterator(); iter.hasNext(); ) {
	  String srcState = (String) iter.next();
	  double initialCost = startSet.contains(srcState) ? ZERO_COST : INFINITE_COST;
	  double finalCost = finalSet.contains(srcState) ? ZERO_COST : INFINITE_COST;
	  HashSet ds = (HashSet) destMap.get(srcState);
	  String[] destStates = new String[ds.size()];
	  int i = 0;
	  for (Iterator iter2 = ds.iterator(); iter2.hasNext(); i++)
	      destStates[i] = (String) iter2.next();
	  String[] destLabels = destStateNames2destLabels(destStates);
	  String[] weightNames = transitions2weightNames(srcState, destStates);

	  System.out.println("@@@ " + srcState + " " + initialCost + " " + finalCost);
	  for (int x = 0; x < destStates.length; x++)
	      System.out.println("\t" + destStates[x] + " " + destLabels[x] + " " + weightNames[x]);
			

	  addState(srcState, initialCost, finalCost, destStates, destLabels, weightNames);
        }

    }

    // Helper method for addStatesForReferencesFromRegex
    private static HashMap getDefaultChar2StateMap (String prefix)
    {
        HashMap result = new HashMap();
        result.put("0", "init-<START>");
        result.put("1", "init-authors");
        result.put("2", "init-author");
        result.put("3", "init-author-first");
        result.put("4", "init-author-middle");
        result.put("5", "init-author-last");
        result.put("6", "init-author-affix");
        result.put("b", prefix + "booktitle");
        result.put("c", prefix + "conference");
        result.put("d", prefix + "date");
        result.put("e", prefix + "editor");
        result.put("g", prefix + "pages");
        result.put("h", prefix + "tech");
        result.put("i", prefix + "institution");
        result.put("j", prefix + "journal");
        result.put("l", prefix + "address");
        result.put("n", prefix + "note");
        result.put("p", prefix + "publisher");
        result.put("r", "init-ref-marker");
        result.put("s", prefix + "thesis");
        result.put("t", prefix + "title");
        result.put("u", prefix + "number");
        result.put("v", prefix + "volume");
        result.put("w", prefix + "web");
        result.put("y", prefix + "series");
        return result;
    }

    // Treats the regex as describing a state path
    // Only supports regex containing ? following a alpha-numeric char and [...]*
    // (Regex may not contain nesting of any kind or parens or ^ or | or $ or +)
    // Always adds self-transitions
    // Returns: 1) HashMap from each state to the set of possible next states.
    //          2) HashSet of initial states
    //          3) HashSet of final states
    private static Object[] getDestinationStatesFromRegularExpression (String regex)
    {
        if (regex.trim().length() == 0 || ! regex.matches("((\\w\\??)*(\\[\\w+\\]\\*)*)*"))
	  throw new IllegalArgumentException("malformed regex: " + regex);

        // Mark whether the char at each position is between brackets
        // and whether the char at each position is followed immediately by a ?
        BitSet insideBrackets = new BitSet();
        BitSet followedByQ = new BitSet();
        boolean inside = false;
        for (int i = 0; i < regex.length(); i++) {
	  char ch = regex.charAt(i);
	  if (ch == '[')
	      inside = true;
	  else if (ch == ']')
	      inside = false;
	  else if (inside)
	      insideBrackets.set(i);

	  if (ch == '?')
	      followedByQ.set(i-1);
        }
		
        // For each char in the regex find the range in which to look for dest states
        int[] startIndices = new int[regex.length()];
        int[] endIndices = new int[regex.length()];
        for (int i = 0; i < regex.length(); i++) {
	  String s = regex.substring(i, i+1);
	  if (! s.matches("\\w"))
	      continue;
			
	  int limit = (i == regex.length()-1) ? i : i+1;
	  char ch = regex.charAt(limit);
	  while (limit < regex.length() - 1 && (ch == '?' || ch == '[' || ch == ']' || ch == '*' || 
					insideBrackets.get(limit) || followedByQ.get(limit))) {
	      limit++;
	      ch = regex.charAt(limit);
	  }
	  endIndices[i] = limit;

	  limit = i;
	  while (insideBrackets.get(limit))
	      limit--;
	  startIndices[i] = limit;
        }

        // Finally, set the destination states for each char in the regex
        HashMap state2destSet = new HashMap(); // state -> set of dest states
        HashSet startSet = new HashSet();
        HashSet finalSet = new HashSet();
        for (int i = 0; i < regex.length(); i++) {
	  String src = regex.substring(i, i+1);
	  if (! src.matches("\\w"))
	      continue;

	  //System.out.println("@@@ " + regex.charAt(i) + " " + insideBrackets.get(i) + " " + followedByQ.get(i) + " " + regex.charAt(i) + " " + startIndices[i] + " " + endIndices[i] + " " + regex.charAt(startIndices[i]) + " " + regex.charAt(endIndices[i]));

	  HashSet destSet = (HashSet) state2destSet.remove(src);
	  if (destSet == null)
	      destSet = new HashSet();

	  for (int j = startIndices[i]; j <= endIndices[i]; j++) {
	      String dest = regex.substring(j, j+1);
	      if (dest.matches("\\w"))
		destSet.add(dest);
	  }
	  state2destSet.put(src, destSet);

	  if (startIndices[i] == 0)
	      startSet.add(src);
	  if (endIndices[i] == regex.length() - 1)
	      finalSet.add(src);
        }

        return new Object[]{ state2destSet, startSet, finalSet };
    }

    static private String[] prependStrings (String prefix, String[] stuff)
    {
        String[] ret = new String[stuff.length];
        for (int i = 0; i < stuff.length; i++)
	  ret[i] = prefix + stuff[i];
        return ret;
    }

    static private String[] destStateNames2destLabels (String[] destStateNames)
    {
        String[] ret = new String[destStateNames.length];
        for (int i = 0; i < destStateNames.length; i++) {
	  ret[i] = destStateNames[i].substring(1+destStateNames[i].indexOf("-"));
        }
        return ret;
    }

    static private String[] transitions2weightNames (String srcStateName, String[] destStateNames)
    {
        String srcLabel = srcStateName.substring(1+srcStateName.indexOf("-"));
        String[] ret = new String[destStateNames.length];
        for (int i = 0; i < destStateNames.length; i++) {
	  String destLabel = destStateNames[i].substring(1+destStateNames[i].indexOf("-"));
	  ret[i] = srcLabel + "->" + destLabel + ":" + destLabel;
        }
        return ret;
    }


    // Tester
    public static void main (String[] argv)
    {
        RexaCRF crf = new RexaCRF(new Noop(), new Noop());
        crf.addStatesForReferencesFromRegex();  // exception expected
    }
		
}
