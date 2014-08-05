package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.span.CompositeSpan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by klimzaporojets on 8/4/14.
 * Adds features useful to identify parts of the body of a particular paper.
 */
public class Token2BodyFeatureSequence  extends Pipe implements Serializable {

    private static String lonelyNumbers = "[1-9][\\.]{0,1}[\\s]+]"; //"[1-9][\\.]{0,1}";
    private static String lonelyLetters = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ][\\.]{0,1}[\\s]+]"; //"[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ][\\.]{0,1}";

    static Pattern ptrnLonelyNumbers = Pattern.compile(lonelyNumbers);
    static Pattern ptrnLonelyLetters = Pattern.compile(lonelyLetters);

    @Override
    public Instance pipe(Instance carrier) {
        NewHtmlTokenization data = (NewHtmlTokenization)carrier.getData();

        List<CompositeSpan> lineSpans = data.getLineSpans();
        NewHtmlTokenization2LineInfo nhtml2LineInfo = new NewHtmlTokenization2LineInfo();
        Instance onlyLines =  nhtml2LineInfo.pipe(carrier);
        computeFeatures((LineInfo [])onlyLines.getData(),data);
        int qty = 0;
//        for(int i = 0; i< lineSpans.size(); i++)
//        {
//            int min = 0;
//            CompositeSpan ls = lineSpans.get(i);
//
//            for(Span sp:(List<Span>)ls.getSpans())
//            {
//                System.out.println(qty);
//                System.out.println(sp.getText() + " vs. " + data.getToken(qty).getText());
//                if(!sp.getText().equals(data.getToken(qty).getText()))
//                {
//                    System.out.println("distinct");
//                }
//                qty++;
//            }
//        }

        return carrier;
    }


    private void computeFeatures(LineInfo[] lineInfos, NewHtmlTokenization data)
    {
        computeLexiconFeatures(data);
//        computeLayoutFeatures(lineInfos, data);
    }

    private static void computeLayoutFeatures(LineInfo[] lineInfos, NewHtmlTokenization data) {
        for(int i =0; i<lineInfos.length; i++ )
        {
            List<String> features = new ArrayList<String>();

        }

    }

    private static void computeLexiconFeatures(/*LineInfo[] lineInfos,*/ NewHtmlTokenization data) {
        // high correlation with non-bibliographic content
        String[] nonSectionWords = {"^(Table).*", "^(Figure).*", "^(Fig\\.).*"};
        String allCaps = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ1-9]+";
        String initCap = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ].*";
        String finalDot = "((.*)\\.)$";
        String firstLevelSection = "^((\\s).*([\\d]+)([\\.]{0,1})([\\s]+).*)";
        String secondLevelSection = "^((\\s).*([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+).*)";
        String thirdLevelSection = "^((\\s).*([\\d]+)(\\.)([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+).*)";


//        String fourthLevelSection = "^((\\s).*([\\d]+)(\\.)([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+).*)";

                /*{ "^[^A-Za-z]*Received[^A-Za-z]",
                "^[A-Za-z]*Figure(s)?[^A-Za-z]",
                "^[A-Za-z]*Table(s)?[^A-Za-z]", "^[A-Za-z]*Graph(s)?[^A-Za-z]",
                "We ", " we ", "She ", " she ", "He ", " he ", "Our ", " our ",
                "Her ", " her ", "His ", " his ", "These ", " these ", "Acknowledgements" };*/

        for(int i =0; i<data.getLineSpans().size(); i++ )
        {
            List<String> lexiconFeatures = new ArrayList<String>();
            CompositeSpan ls = (CompositeSpan)data.getLineSpans().get(i);
            String currentLineText = ls.getText().trim();
            String squishedLineText = currentLineText.replaceAll("\\s", "");

            for (int j = 0; j < nonSectionWords.length; j++) {
                if (currentLineText.matches(nonSectionWords[j])) {
                    //lineInfos[i].presentFeatures.add("containsPostword1");
                    ls.setFeatureValue("startsNonSectionWord", 1.0);
                    break;
                }
            }

            if(squishedLineText.matches(allCaps))
            {
                ls.setFeatureValue("allCaps", 1.0);
            }
            if(currentLineText.matches(initCap))
            {
                ls.setFeatureValue("startsCap", 1.0);
            }
            if(currentLineText.matches(finalDot))
            {
                ls.setFeatureValue("endsInDot", 1.0);
            }

            Matcher matcher = ptrnLonelyLetters.matcher(currentLineText);
            if(isUpFlagCount(currentLineText,ptrnLonelyLetters,0.5))
            {
                ls.setFeatureValue("manyLonelyLetters", 1.0);
            }

            if(isUpFlagCount(currentLineText,ptrnLonelyNumbers,0.5))
            {
                ls.setFeatureValue("manyLonelyNumbers", 1.0);
            }

            if(currentLineText.matches(firstLevelSection))
            {
                ls.setFeatureValue("firstLevelSectionPtrn", 1.0);
            }

            if(currentLineText.matches(secondLevelSection))
            {
                ls.setFeatureValue("secondLevelSectionPtrn", 1.0);
            }

            if(currentLineText.matches(thirdLevelSection))
            {
                ls.setFeatureValue("thirdLevelSectionPtrn", 1.0);
            }
        }
    }
    private static boolean isUpFlagCount(String text, Pattern pattern, Double ratioActivation)
    {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while(matcher.find())
        {
            count++;
        }
        if(Double.valueOf(count)/Double.valueOf(text.split(" ").length) > ratioActivation)
        {
            return true;
        }
        return false;
    }

}
