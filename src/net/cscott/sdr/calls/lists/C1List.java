package net.cscott.sdr.calls.lists;

import static net.cscott.sdr.util.Tools.l;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.cscott.sdr.calls.BadCallException;
import net.cscott.sdr.calls.Breather;
import net.cscott.sdr.calls.Call;
import net.cscott.sdr.calls.DanceState;
import net.cscott.sdr.calls.Dancer;
import net.cscott.sdr.calls.DancerPath;
import net.cscott.sdr.calls.ExactRotation;
import net.cscott.sdr.calls.Formation;
import net.cscott.sdr.calls.Position;
import net.cscott.sdr.calls.Program;
import net.cscott.sdr.calls.TaggedFormation;
import net.cscott.sdr.calls.TimedFormation;
import net.cscott.sdr.calls.TaggedFormation.Tag;
import net.cscott.sdr.calls.ast.Apply;
import net.cscott.sdr.calls.ast.Comp;
import net.cscott.sdr.calls.ast.Seq;
import net.cscott.sdr.calls.grm.Rule;
import net.cscott.sdr.calls.transform.Evaluator;
import net.cscott.sdr.util.Box;
import net.cscott.sdr.util.Fraction;

/**
 * The <code>C1List</code> class contains complex call
 * and concept definitions which are on the 'C1' program.
 * Note that "simple" calls and concepts are defined in
 * the resource file at
 * <a href="doc-files/c1.calls"><code>net/cscott/sdr/calls/lists/c1.calls</code></a>;
 * this class contains only those definitions for which an
 * executable component is required.
 * @author C. Scott Ananian
 */
public abstract class C1List {
    // hide constructor.
    private C1List() { }

    private static abstract class C1Call extends Call {
        private final String name;
        C1Call(String name) { this.name = name; }
        @Override
        public final String getName() { return name; }
        @Override
        public final Program getProgram() { return Program.C1; }
        @Override
        public List<Apply> getDefaultArguments() {
            return Collections.emptyList();
        }
    }

    public static final Call CONCENTRIC = new C1Call("_concentric") {
        @Override
        public Comp apply(Apply ast) {
            assert false : "This concept uses a custom Evaluator";
            return null;
        }
        @Override
        public int getMinNumberOfArguments() { return 2; }
        @Override
        public Rule getRule() { return null; /* internal call */ }
        @Override
        public Evaluator getEvaluator(Apply ast) {
            assert ast.callName.equals(getName());
            assert ast.args.size() == 2;
            return new ConcentricEvaluator(ast.getArg(0), ast.getArg(1),
                                           ConcentricType.CONCENTRIC);
        }
    };

    /** Variant of 'concentric' to do. */
    public static enum ConcentricType { QUASI, CONCENTRIC, CROSS };
    /** Evaluator for concentric and quasi-concentric. */
    public static class ConcentricEvaluator extends Evaluator {
        private final Comp centersPart, endsPart;
        private final ConcentricType which;
        public ConcentricEvaluator(Apply centersPart, Apply endsPart,
                                   ConcentricType which) {
            this.centersPart = new Seq(centersPart);
            this.endsPart = new Seq(endsPart);
            this.which = which;
        }
        @Override
        public Evaluator evaluate(DanceState ds) {
            // step 1: pull out the centers/ends
            TaggedFormation f = TaggedFormation.coerce(ds.currentFormation());
            Set<Dancer> centerDancers = f.tagged(Tag.CENTER);
            Set<Dancer> endDancers = new HashSet<Dancer>(f.dancers());
            endDancers.removeAll(centerDancers); // all those who aren't CENTERs
            Formation centerF = f.select(centerDancers).onlySelected();
            Formation endF = f.select(endDancers).onlySelected();
            // xxx should look at whether "eventual ends" (ie, the centers when
            //     doing CROSS concentric) think they are in lines or columns.
            boolean isWide = endF.bounds().width()
                                .compareTo(endF.bounds().height()) > 0;
            // do the call in each separate formation.
            // (breathe to eliminate space left by centers in end formation)
            DanceState centerS = ds.cloneAndClear(centerF);
            DanceState endS = ds.cloneAndClear(Breather.breathe(endF));
            // do the call in the mirrored formation
            TreeSet<Fraction> moments = new TreeSet<Fraction>();
            new Evaluator.Standard(this.centersPart).evaluateAll(centerS);
            for (TimedFormation tf: centerS.formations())
                moments.add(tf.time);
            // in CROSS, have the ends wait until the centers are done
            Fraction endsOffset = Fraction.ZERO;
            if (which==ConcentricType.CROSS) {
                endsOffset = moments.last();
                endS.syncDancers(endsOffset);
            }
            new Evaluator.Standard(this.endsPart).evaluateAll(endS);
            for (TimedFormation tf: endS.formations())
                moments.add(tf.time);
            for (DanceState nds: l(centerS, endS))
                nds.syncDancers(moments.last());

            // XXX adjust isWide depending on ending formation, lines to lines,
            //     etc.

            // hard part! Merge the resulting dancer paths.
            // XXX this is largely cut-and-paste from MetaEvaluator; we should
            //     refactor out the common code.

            // go through all the moments in time, constructing an appropriately
            // breathed formation.
            TreeMap<Fraction,Formation> merged =
                new TreeMap<Fraction,Formation>();
            for (Fraction t : moments) {
                boolean isCross = (which==ConcentricType.CROSS &&
                                   t.compareTo(endsOffset) >=0);
                Formation mergeF = merge(centerS.formationAt(t),
                                         endS.formationAt(t),
                                         isWide, isCross);
                merged.put(t, mergeF);
            }
            // okay, now go through the individual dancer paths, adjusting the
            // 'to' and 'from' positions to match breathed.
            for (DanceState nds : l(centerS, endS)) {
                for (Dancer d : nds.dancers()) {
                    Fraction t = Fraction.ZERO;
                    for (DancerPath dp : nds.movements(d)) {
                        Position nfrom = merged.get(t).location(d);
                        t = t.add(dp.time);
                        Position nto = merged.get(t).location(d);
                        DancerPath ndp = dp.translate(nfrom, nto);
                        ds.add(d, ndp);
                    }
                }
            }
            // dancers should all be in sync at this point.
            // no more to evaluate
            return null;
        }
        private static Formation merge(Formation center, Formation end, boolean isWide, boolean isCross) {
            // recurse to deal with wide/cross flags.
            if (isCross)
                return merge(end, center, isWide, !isCross);
            if (!isWide)
                return merge(center.rotate(ExactRotation.ONE_QUARTER),
                             end.rotate(ExactRotation.ONE_QUARTER),
                             !isWide, isCross)
                       .rotate(ExactRotation.mONE_QUARTER);
            // XXX do we need to breathe the center/end formations here?
            return mergeWide(center, end);
        }
        private static Formation mergeWide(Formation center, Formation end) {
            Map<Dancer,Position> location = new HashMap<Dancer,Position>();
            for (Dancer d : center.dancers())
                location.put(d, center.location(d));
            Box centerBounds = center.bounds();
            for (Dancer d : end.dancers()) {
                Position p = end.location(d);
                Fraction nx = p.x, ny = p.y;
                if (p.x.equals(Fraction.ZERO) && p.y.equals(Fraction.ZERO))
                    throw new BadCallException("Outside dancer ends up at origin!");
                if (p.x.compareTo(Fraction.ZERO) == 0) {
                    ny = ny.add((p.y.compareTo(Fraction.ZERO) < 0 ?
                                 centerBounds.ll : centerBounds.ur).y);
                } else {
                    nx = nx.add((p.x.compareTo(Fraction.ZERO) < 0 ?
                                 centerBounds.ll : centerBounds.ur).x);
                }
                location.put(d, p.relocate(nx, ny, p.facing));
            }
            return new Formation(location);
        }
    };
}
