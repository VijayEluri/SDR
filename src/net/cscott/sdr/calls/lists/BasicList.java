package net.cscott.sdr.calls.lists;

import java.util.ArrayList;
import java.util.List;

import net.cscott.sdr.calls.Call;
import net.cscott.sdr.calls.Warp;
import net.cscott.sdr.calls.ast.Apply;
import net.cscott.sdr.calls.ast.Comp;
import net.cscott.sdr.calls.ast.Part;
import net.cscott.sdr.calls.ast.Seq;
import net.cscott.sdr.calls.ast.SeqCall;
import net.cscott.sdr.calls.ast.Warped;
import net.cscott.sdr.util.Fraction;

/** 
 * The <code>BasicList</code> class contains complex call
 * and concept definitions which are on the 'basic' program.
 * @author C. Scott Ananian
 * @version $Id: BasicList.java,v 1.2 2006-10-11 05:28:10 cananian Exp $
 */
public class BasicList {
    // hide constructor.
    private BasicList() { }

    private static abstract class BasicCall extends Call {
        private final String name;
        BasicCall(String name) { this.name = name; }
        @Override
        public final String getName() { return name; }
        @Override
        public final String getProgram() { return "basic"; }
    }
    
    public static final Call SQUARE_THRU = new BasicCall("square thru") {
            @Override
            public Comp apply(Apply ast) {
                assert ast.callName.equals(getName());
                assert ast.getNumberOfChildren()==1;
                // one parameter: a count
                Fraction n = ast.getNumberArg(0);
                // validate.
                if (Fraction.ZERO.compareTo(n) >= 0)
                    throw new IllegalArgumentException
                        ("Can't square thru zero or fewer times");
                // square thru 1 is right pull by
                if (Fraction.ONE.compareTo(n) >= 1)
                    return new Seq(new Part
                                   (new Seq
                                    (Apply.makeApply
                                     ("fractional",
                                      Apply.makeApply(n.toString()),
                                      Apply.makeApply("pull by")))));
                // square thru N is right pull by, quarter in,
                // left square thru (N-1) (even if N is fractional)
                return new Seq(new Part(new Seq(
                        Apply.makeApply("pull by"),
                        Apply.makeApply("quarter in"),
                        Apply.makeApply("left",
                                Apply.makeApply("square thru",
                                        n.subtract(Fraction.ONE))))));
            }
    };
    // simple combining concept.
    public static final Call AND = new BasicCall("and") {
        @Override
        public Comp apply(Apply ast) {
            assert ast.callName.equals(getName());
            assert ast.getNumberOfChildren()>=1;
            List<SeqCall> l = new ArrayList<SeqCall>(ast.getNumberOfChildren());
            Apply a = (Apply) ast.getFirstChild();
            while (a!=null) {
                l.add(a);
                a = (Apply) a.getNextSibling();
            }
            return new Seq(l.toArray(new SeqCall[l.size()]));
        }
    };
    public static final Call LEFT = new BasicCall("left") {
        @Override
        public Comp apply(Apply ast) {
            assert ast.callName.equals(getName());
            assert ast.getNumberOfChildren()==1;
            Apply a = ast.getArg(0);
            Warp warp = Warp.MIRROR;
            return new Warped(warp, new Seq(a));
        }
        
    };
}
