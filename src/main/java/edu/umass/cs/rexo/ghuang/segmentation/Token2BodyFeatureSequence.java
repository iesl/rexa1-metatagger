package edu.umass.cs.rexo.ghuang.segmentation;

import edu.umass.cs.mallet.base.extract.Span;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import org.rexo.extraction.NewHtmlTokenization;
import org.rexo.span.CompositeSpan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by klimzaporojets on 8/4/14.
 * Adds features useful to identify parts of the body of a particular paper.
 */
public class Token2BodyFeatureSequence  extends Pipe implements Serializable {


    @Override
    public Instance pipe(Instance carrier) {
        NewHtmlTokenization data = (NewHtmlTokenization)carrier.getData();

        List<CompositeSpan> lineSpans = data.getLineSpans();
        NewHtmlTokenization2LineInfo nhtml2LineInfo = new NewHtmlTokenization2LineInfo();
        Instance onlyLines =  nhtml2LineInfo.pipe(carrier);

        int qty = 0;
        for(int i = 0; i< lineSpans.size(); i++)
        {
            int min = 0;
            CompositeSpan ls = lineSpans.get(i);
            for(Span sp:(List<Span>)ls.getSpans())
            {
                System.out.println(qty);
                System.out.println(sp.getText() + " vs. " + data.getToken(qty).getText());
                if(!sp.getText().equals(data.getToken(qty).getText()))
                {
                    System.out.println("distinct");
                }

                qty++;

            }
        }

        return carrier;
    }


    private void computeFeatures(LineInfo[] lineInfos, NewHtmlTokenization data)
    {
//        computeLexiconFeatures(lineInfos, data);
//        computeLayoutFeatures(lineInfos, data);
    }

    private static void computeLayoutFeatures(LineInfo[] lineInfos, NewHtmlTokenization data) {
        for(int i =0; i<lineInfos.length; i++ )
        {
            List<String> features = new ArrayList<String>();

        }

    }

    private static void computeLexiconFeatures(LineInfo[] lineInfos, NewHtmlTokenization data) {
        // high correlation with non-bibliographic content
        String[] NonSectionWords = {"^(Table).*", "^(Figure).*", "^(Fig\\.).*"};
        String allCaps = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ1-9]+";
        String finalDot = "((.*)\\.)$";
        String lonelyNumbers = "[1-9][\\.]{0,1}"; //"[1-9][\\.]{0,1}";
        String lonelyLetters = "[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ][\\.]{0,1}"; //"[A-ZÁÉÍÓÚÀÈÌÒÙÇÑÏÜ][\\.]{0,1}";
        String firstLevelSection = "^((\\s).*([\\d]+)([\\.]{0,1})([\\s]+).*)";
        String secondLevelSection = "^((\\s).*([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+).*)";
        String thirdLevelSection = "^((\\s).*([\\d]+)(\\.)([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+).*)";
//        String fourthLevelSection = "^((\\s).*([\\d]+)(\\.)([\\d]+)(\\.)([\\d]+)([\\.]{0,1})([\\s]+).*)";

                /*{ "^[^A-Za-z]*Received[^A-Za-z]",
                "^[A-Za-z]*Figure(s)?[^A-Za-z]",
                "^[A-Za-z]*Table(s)?[^A-Za-z]", "^[A-Za-z]*Graph(s)?[^A-Za-z]",
                "We ", " we ", "She ", " she ", "He ", " he ", "Our ", " our ",
                "Her ", " her ", "His ", " his ", "These ", " these ", "Acknowledgements" };*/

        for(int i =0; i<lineInfos.length; i++ )
        {
            List<String> lexiconFeatures = new ArrayList<String>();;


        }


    }

}
