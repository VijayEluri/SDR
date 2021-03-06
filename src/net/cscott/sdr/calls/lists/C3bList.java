package net.cscott.sdr.calls.lists;

import java.util.Collections;
import java.util.List;

import net.cscott.sdr.calls.Call;
import net.cscott.sdr.calls.DanceState;
import net.cscott.sdr.calls.Evaluator;
import net.cscott.sdr.calls.FormationList;
import net.cscott.sdr.calls.Program;
import net.cscott.sdr.calls.MatcherList;
import net.cscott.sdr.calls.ast.Expr;
import net.cscott.sdr.calls.grm.Grm;
import net.cscott.sdr.calls.grm.Rule;
import net.cscott.sdr.calls.lists.A1List.SolidEvaluator;
import net.cscott.sdr.calls.lists.A1List.SolidMatch;
import net.cscott.sdr.calls.lists.A1List.SolidType;
import net.cscott.sdr.calls.lists.BasicList.LRMEvaluator;
import net.cscott.sdr.calls.lists.BasicList.LRMType;
import net.cscott.sdr.util.Fraction;

/**
 * The <code>C3bList</code> class contains complex call
 * and concept definitions which are on the 'plus' program.
 * Note that "simple" calls and concepts are defined in
 * the resource file at
 * <a href="doc-files/c3b.calls"><code>net/cscott/sdr/calls/lists/c3b.calls</code></a>;
 * this class contains only those definitions for which an
 * executable component is required.
 * @author C. Scott Ananian
 */
public abstract class C3bList {
    // hide constructor.
    private C3bList() { }

    private static abstract class C3BCall extends Call {
        private final String name;
        C3BCall(String name) { this.name = name; }
        @Override
        public final String getName() { return name; }
        @Override
        public final Program getProgram() { return Program.C3B; }
        @Override
        public List<Expr> getDefaultArguments() {
            return Collections.emptyList();
        }
    }

    // note that precedence level makes "mirror swing thru and roll"
    // equivalent to "mirror (swing thru and roll)"
    // while "left swing thru and roll" is "(left swing thru) and roll".
    // XXX: is this right?
    public static final Call MIRROR = new C3BCall("mirror") {
        @Override
        public int getMinNumberOfArguments() { return 1; }
        @Override
        public Rule getRule() {
            Grm g = Grm.parse("mirror <0=anything>");
            return new Rule("anything", g, Fraction.ZERO, Rule.Option.CONCEPT);
        }
        @Override
        public Evaluator getEvaluator(DanceState ds, List<Expr> args) {
            assert args.size()==1;
            return new LRMEvaluator(LRMType.MIRROR, args.get(0));
        }
    };

    public static final Call TANDEM_TWOSOME = new C3BCall("tandem twosome") {
        @Override
        public int getMinNumberOfArguments() { return 1; }
        @Override
        public Rule getRule() {
            Grm g = Grm.parse("tandem twosome <0=anything>");
            return new Rule("anything", g, Fraction.ZERO, Rule.Option.CONCEPT);
        }
        @Override
        public Evaluator getEvaluator(DanceState ds, List<Expr> args) {
            assert args.size() == 1;
            return new SolidEvaluator(args.get(0), FormationList.TANDEM,
                                      SolidMatch.ALL, SolidType.TWOSOME);
        }
    };

    public static final Call COUPLES_TWOSOME = new C3BCall("couples twosome") {
        @Override
        public int getMinNumberOfArguments() { return 1; }
        @Override
        public Rule getRule() {
            Grm g = Grm.parse("couples twosome <0=anything>");
            return new Rule("anything", g, Fraction.ZERO, Rule.Option.CONCEPT);
        }
        @Override
        public Evaluator getEvaluator(DanceState ds, List<Expr> args) {
            assert args.size() == 1;
            return new SolidEvaluator(args.get(0), FormationList.COUPLE,
                                      SolidMatch.ALL, SolidType.TWOSOME);
        }
    };

    public static final Call SIAMESE_TWOSOME = new C3BCall("siamese twosome") {
        @Override
        public int getMinNumberOfArguments() { return 1; }
        @Override
        public Rule getRule() {
            Grm g = Grm.parse("siamese twosome <0=anything>");
            return new Rule("anything", g, Fraction.ZERO, Rule.Option.CONCEPT);
        }
        @Override
        public Evaluator getEvaluator(DanceState ds, List<Expr> args) {
            assert args.size() == 1;
            return new SolidEvaluator(args.get(0), "siamese",
                                      MatcherList.SIAMESE, SolidType.TWOSOME);
        }
    };
}
