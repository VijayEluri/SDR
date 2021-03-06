/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package net.cscott.sdr.recog;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.linguist.dflat.DynamicFlatLinguist;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.URL;


/**
 * A simple demo showing a simple speech application 
 * built using Sphinx-4. This application uses the Sphinx-4 endpointer,
 * which automatically segments incoming audio into utterances and silences.
 */
public class SphinxDemo {

    /**
     * Main method for running the demo.
     */
    public static void main(String[] args) {
        try {
            URL url;
            if (args.length > 0) {
                url = new File(args[0]).toURI().toURL();
            } else {
                url = SphinxDemo.class.getResource("sdr.config.xml");
            }

            System.out.println("Loading...");

            ConfigurationManager cm = new ConfigurationManager(url);

	    Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
	    Microphone microphone = (Microphone) cm.lookup("microphone");

            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();
            
            /* get the JSGF grammar component */
            JSGFGrammar jsgfGrammar =
                (JSGFGrammar) cm.lookup("jsgfGrammar");
            jsgfGrammar.loadJSGF("Mainstream");
            // XXX work around bug in DynamicFlatLinguist
            ((DynamicFlatLinguist) cm.lookup("dflatLinguist")).allocate();

            jsgfGrammar.dumpRandomSentences(10);

            /* the microphone will keep recording until the program exits */
            // start the recording by selecting a mixer (the first one)
            microphone.switchMixer(microphone.availableMixers().get(0));

            System.out.println("Give a Mainstream call.");

            while (true) {
                System.out.println
                ("Start speaking. Press Ctrl-C to quit, or say \"Bow to your partner\".\n");

                /*
                 * This method will return when the end of speech
                 * is reached. Note that the endpointer will determine
                 * the end of speech.
                 */
                Result result = recognizer.recognize();

                if (result != null) {
                    // we can use result.getResults() to get N possible
                    // results.  (See source code for
                    // Result.getBestFinalResultNoFiller() for details).
                    String resultText = result.getBestFinalResultNoFiller();
                    System.out.println("You said: " + resultText + "\n");
                    if (resultText.equalsIgnoreCase("exit") ||
                        resultText.toLowerCase().startsWith("bow to "))
                        break;
                } else {
                    System.out.println("I can't hear what you said.\n");
                }
            }
            recognizer.deallocate();
        } catch (IOException e) {
            System.err.println("Problem when loading SphinxDemo: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring SphinxDemo: " + e);
            e.printStackTrace();
        }
    }
}
