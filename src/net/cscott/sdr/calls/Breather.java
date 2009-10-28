package net.cscott.sdr.calls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.junit.runner.RunWith;

import EDU.Washington.grad.gjb.cassowary.CL;
import EDU.Washington.grad.gjb.cassowary.ClLinearEquation;
import EDU.Washington.grad.gjb.cassowary.ClLinearExpression;
import EDU.Washington.grad.gjb.cassowary.ClLinearInequality;
import EDU.Washington.grad.gjb.cassowary.ClSimplexSolver;
import EDU.Washington.grad.gjb.cassowary.ClStrength;
import EDU.Washington.grad.gjb.cassowary.ClVariable;
import EDU.Washington.grad.gjb.cassowary.ExCLError;
import net.cscott.jdoctest.JDoctestRunner;
import net.cscott.jutil.GenericMultiMap;
import net.cscott.sdr.util.Box;
import net.cscott.sdr.util.Fraction;
import net.cscott.sdr.util.Point;
import net.cscott.sdr.util.SdrToString;
import net.cscott.sdr.util.Tools.ListMultiMap;
import static net.cscott.sdr.util.Tools.F; // list comprehension helper
import static net.cscott.sdr.util.Tools.foreach; // list comprehension
import static net.cscott.sdr.util.Tools.m;//map constructor
import static net.cscott.sdr.util.Tools.mml;//listmultimap constructor
import static net.cscott.sdr.util.Tools.p;//pair constructor
import static net.cscott.sdr.util.Tools.l;//list constructor

/**
 * The {@link Breather} class contains methods to reassemble and
 * breathe formations.
 *
 * <p>The {@link #insert(Formation,Map) insert()} method pushes
 * sub-formations into a meta-formation after performing (say) a four
 * person call &mdash; ie, starting with a tidal wave, {@link
 * Selector} will pull out two four-person waves as a mini-wave as the
 * meta-formation.  We do a crossfire (say) from the mini-waves to get
 * boxes.  Now {@link #insert(Formation,Map)} will shove the boxes
 * into the mini-wave meta-formation to get parallel ocean waves.</p>
 *
 * <p>The {@link #breathe(List) breathe()} method is a part of {@link
 * #insert(Formation,Map) insert()} which is useful in its own right:
 * it takes a {@link Formation} (or a list of {@link FormationPiece}s)
 * and breathes it in or out to normalize the spacing between dancers.
 * For example, after "trailers extend" from boxes, we need to make
 * room for the resulting mini-wave in the center.  If the ends then
 * u-turn back and everyone extends again, the formation has to
 * squeeze in again to erase the space.</p>
 *
 * <h3>Theory of breathing</h3>
 * <p>First: identify collisions.  Collided dancers are
 * inserted into a miniwave which replaces them in the remainder of the
 * algorithm.  Second: resolve overlaps.  Dancers which overlap have their
 * boundaries adjusted so that they share a boundary at the midpoint of the
 * overlap.  Order the resolution from "closest" overlapping dancers to
 * "furthest apart" (smallest overlap), and secondarily from center out, so
 * that extreme overlaps (ie, dancers spaced 1/4 apart) are handled sanely.
 * Third: Sort and order the boundary coordinates, and then allocate space
 * between boundaries so that it is "just enough" to fit the dancers between
 * them.  If a dancer spans multiple boundary points, their allocation is
 * divided equally between them.  Finally, the output formations are
 * relocated so that they are centered between their new boundaries.
 *
 * @author C. Scott Ananian
 * @version $Id: Breather.java,v 1.10 2006-10-30 22:09:29 cananian Exp $
 */
@RunWith(value=JDoctestRunner.class)
public class Breather {

    /**
     * Insert formations into a meta-formation.  This reassembles the
     * formation after we've decomposed it into (say) boxes to do a
     * four-person call.
     *
     * @doc.test Insert COUPLEs, then TANDEMs into a RH_OCEAN_WAVE.  Then, for
     *  a challenge, insert TANDEMs into a DIAMOND to give a t-bone column:
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> function xofy(meta, f) {
     *    >   var i=0
     *    >   var m=new java.util.LinkedHashMap()
     *    >   for (d in Iterator(meta.sortedDancers())) {
     *    >     var mm=new java.util.LinkedHashMap()
     *    >     for (dd in Iterator(f.sortedDancers())) {
     *    >       mm.put(dd, StandardDancer.values()[i++])
     *    >     }
     *    >     m.put(d, f.map(mm))
     *    >     print(m.get(d).toStringDiagram())
     *    >   }
     *    >   return m
     *    > }
     *  js> meta = FormationList.RH_OCEAN_WAVE ; meta.toStringDiagram()
     *  ^    v    ^    v
     *  js> m = xofy(meta, FormationList.COUPLE); undefined
     *  1B^  1G^
     *  2B^  2G^
     *  3B^  3G^
     *  4B^  4G^
     *  js> Breather.insert(meta, m).toStringDiagram()
     *  1B^  1G^  2Gv  2Bv  3B^  3G^  4Gv  4Bv
     *  js> m = xofy(meta, FormationList.TANDEM); undefined
     *  1B^
     *  
     *  1G^
     *  2B^
     *  
     *  2G^
     *  3B^
     *  
     *  3G^
     *  4B^
     *  
     *  4G^
     *  js> Breather.insert(meta, m).toStringDiagram()
     *  1B^  2Gv  3B^  4Gv
     *  
     *  1G^  2Bv  3G^  4Bv
     *  js> meta = FormationList.RH_DIAMOND ; meta.toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |
     *  |
     *  |^    v
     *  |
     *  |
     *  |  <
     *  js> m = xofy(meta, FormationList.TANDEM); undefined
     *  1B^
     *  
     *  1G^
     *  2B^
     *  
     *  2G^
     *  3B^
     *  
     *  3G^
     *  4B^
     *  
     *  4G^
     *  js> Breather.insert(meta, m).toStringDiagram()
     *  1G>  1B>
     *  
     *  2B^  3Gv
     *  
     *  2G^  3Bv
     *  
     *  4B<  4G<
     */
    public static Formation insert(final Formation meta,
                                   final Map<Dancer,Formation> components) {
        List<FormationPiece> l =
            new ArrayList<FormationPiece>(meta.dancers().size());
        for (Dancer d : meta.dancers()) {
            assert meta.location(d).facing.isExact();
            l.add(new FormationPiece(meta.select(d).onlySelected(),
                                     components.get(d),
                                     (ExactRotation) meta.location(d).facing));
        }
        return breathe(l);
    }

    /*-----------------------------------------------------------------------*/

    public static class FormationPiece {
        /** Input formation piece. The original formation is a simple
         * superposition of these. */
        public final Formation input;
        /** The formation which will correspond to {@link #input} in the output
         * (meta) formation.  This might be a formation of a single
         * {@link PhantomDancer Phantom}, for example.
         * @see FormationList#SINGLE_DANCER
         */
        public final Formation output;
        /**
         * Prepare an argument to the {@link #breathe} method.
         * @param input  Input formation piece.
         * @param output Output formation piece.
         * @param r
         * The rotation to use for the output formation in the eventual
         * result. Typically this is the rotation of formation {@link #input}
         * from whatever the 'canonical' orientation of {@link #output} is.
         * For example, if we are mapping single dancers to single dancers,
         * then {@link #input} is the rotated offset result of
         * {@link Formation#onlySelected()}, {@link #output} is
         * {@link FormationList#SINGLE_DANCER} (which is facing north), and
         * the rotation {@code imr} matches the rotation of the dancer in
         * {@link #input}.
         */
        public FormationPiece(Formation input, Formation output, ExactRotation r) {
            this(input, output.rotate(r));
        }
        public FormationPiece(Formation input, Formation output) {
            this.input = input;
            this.output = output;
        }
        @Override
        public String toString() {
            return new ToStringBuilder(this, SdrToString.STYLE)
            .append("input", input)
            .append("output", output)
            .toString();
        }
    }
    /**
     * Create a canonical formation by compressing the given one.  This
     * is just an invokation of {@link #breathe(List)} with
     * trivial {@link FormationPiece}s consisting of a single dancer each.
     *
     * @doc.test From couples back to back, step out; then breathe in:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.BACK_TO_BACK_COUPLES ; f.toStringDiagram()
     *  ^    ^
     *  
     *  v    v
     *  js> for (d in Iterator(f.dancers())) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE, false));
     *    > }; f.toStringDiagram()
     *  ^    ^
     *  
     *  
     *  
     *  v    v
     *  js> Breather.breathe(f).toStringDiagram()
     *  ^    ^
     *  
     *  v    v
     * @doc.test From facing couples, take half a step in; breathe out:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.FACING_COUPLES ; f.toStringDiagram()
     *  v    v
     *  
     *  ^    ^
     *  js> for (d in Iterator(f.dancers())) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE_HALF, false));
     *    > }; f.toStringDiagram()
     *  v    v
     *  ^    ^
     *  js> Breather.breathe(f).toStringDiagram()
     *  v    v
     *  
     *  ^    ^
     * @doc.test From single three quarter tag, step out; then breathe in:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.RH_SINGLE_THREE_QUARTER_TAG ; f.toStringDiagram()
     *    ^
     *  
     *  ^    v
     *  
     *    v
     *  js> for (d in Iterator(f.tagged(TaggedFormation.Tag.END))) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE, false));
     *    > }; f.toStringDiagram()
     *    ^
     *  
     *  
     *  ^    v
     *  
     *  
     *    v
     *  js> Breather.breathe(f).toStringDiagram()
     *    ^
     *  
     *  ^    v
     *  
     *    v
     * @doc.test From single quarter tag, take half a step in; breathe out:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.RH_SINGLE_QUARTER_TAG ; f.toStringDiagram()
     *    v
     *  
     *  ^    v
     *  
     *    ^
     *  js> for (d in Iterator(f.tagged(TaggedFormation.Tag.END))) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE, false));
     *    > }; f.toStringDiagram()
     *    v
     *  ^    v
     *    ^
     *  js> Breather.breathe(f).toStringDiagram()
     *    v
     *  
     *  ^    v
     *  
     *    ^
     * @doc.test From right-hand diamond, take a step and a half in; breathe out:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.RH_DIAMOND ; f.toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |
     *  |
     *  |^    v
     *  |
     *  |
     *  |  <
     *  js> // XXX have to change this amount if we shrink diamonds
     *  js> for (d in Iterator(f.tagged(TaggedFormation.Tag.POINT))) {
     *    >   f=f.move(d,f.location(d).sideStep(Fraction.valueOf(3,2), true));
     *    > }; f.toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |
     *  |^    v
     *  |  <
     *  js> Breather.breathe(f).toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |
     *  |^    v
     *  |
     *  |  <
     * @doc.test From right-hand diamond, take 2 1/2 steps in; breathe out to stars:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.RH_DIAMOND ; f.toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |
     *  |
     *  |^    v
     *  |
     *  |
     *  |  <
     *  js> // XXX have to change this amount if we shrink diamonds
     *  js> for (d in Iterator(f.tagged(TaggedFormation.Tag.POINT))) {
     *    >   f=f.move(d,f.location(d).sideStep(Fraction.valueOf(5,2), true));
     *    > }; f.toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |^ <  v
     *  js> Breather.breathe(f).toStringDiagram("|", Formation.dancerNames)
     *  |  >
     *  |^    v
     *  |  <
     * @doc.test Facing dancers step forward; resolve collision.
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.FACING_DANCERS ; f.toStringDiagram()
     *  v
     *  
     *  ^
     *  js> for (d in Iterator(f.dancers())) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE, false));
     *    > }; f
     *  net.cscott.sdr.calls.TaggedFormation[
     *    location={<phantom@7f>=0,0,n, <phantom@7e>=0,0,s}
     *    selected=[<phantom@7f>, <phantom@7e>]
     *    tags={<phantom@7f>=TRAILER, <phantom@7e>=TRAILER}
     *  ]
     *  js> Breather.breathe(f).toStringDiagram()
     *  ^    v
     * @doc.test Facing couples step forward with a left-shoulder pass;
     *  resolve collision and breathe.
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f = FormationList.FACING_COUPLES ; f.toStringDiagram()
     *  v    v
     *  
     *  ^    ^
     *  js> for (d in Iterator(f.dancers())) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE, false).addFlags(Position.Flag.PASS_LEFT));
     *    > }; undefined
     *  js> Breather.breathe(f).toStringDiagram()
     *  v    ^    v    ^
     */
    public static Formation breathe(Formation f) {
        List<FormationPiece> fpl = new ArrayList<FormationPiece>
            (f.dancers().size());
        for (Dancer d: f.dancers()) {
            Formation in = f.select(Collections.singleton(d)).onlySelected();
            Position p = in.location(d);
            p = p.relocate(Fraction.ZERO, Fraction.ZERO, p.facing);
            Formation out = new Formation(m(p(d, p)));
            fpl.add(new FormationPiece(in, out));
        }
        return breathe(fpl);
    }
    /**
     * Take a set of input formation pieces and substitute the
     * given output formation pieces for them, breathing the result
     * together so that the formation is compact.  (The map giving the
     * correspondence between dancers in
     * the new formation and the input formations is given by the
     * individual {@link FormationPiece} objects.)  We also resolve
     * collisions to right or left hands, depending on whether the
     * pass-left flag is set for the {@link Position}s involved.
     * @doc.test Triangle point breathes to center of the base:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> FormationList = FormationListJS.initJS(this); undefined;
     *  js> f2 = new Formation(Tools.m(
     *    >         Tools.p(StandardDancer.COUPLE_2_BOY, Position.getGrid(1,-1,"n")),
     *    >         Tools.p(StandardDancer.COUPLE_2_GIRL, Position.getGrid(3,-1,"n"))))
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 2 BOY=1,-1,n, COUPLE 2 GIRL=3,-1,n}
     *    selected=[COUPLE 2 BOY, COUPLE 2 GIRL]
     *  ]
     *  js> f2.toStringDiagram()
     *   2B^  2G^
     *  js> fp2 = new Breather.FormationPiece(f2, FormationList.RH_MINIWAVE); undefined
     *  js> // point on far side
     *  js> f1 = new Formation(Tools.m(
     *    >         Tools.p(StandardDancer.COUPLE_1_BOY, Position.getGrid(3,1,"e"))))
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 1 BOY=3,1,e}
     *    selected=[COUPLE 1 BOY]
     *  ]
     *  js> fp1 = new Breather.FormationPiece(f1, FormationList.SINGLE_DANCER); undefined
     *  js> Breather.breathe(Tools.l(fp1, fp2)).toStringDiagram()
     *      ^
     *  
     *     ^    v
     *  js> // now just slightly off-center
     *  js> f1 = new Formation(Tools.m(
     *    >         Tools.p(StandardDancer.COUPLE_1_BOY, Position.getGrid(1,1,"e")
     *    >                                    .forwardStep(Fraction.ONE_HALF, false))))
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 1 BOY=1 1/2,1,e}
     *    selected=[COUPLE 1 BOY]
     *  ]
     *  js> fp1 = new Breather.FormationPiece(f1, FormationList.SINGLE_DANCER); undefined
     *  js> Breather.breathe(Tools.l(fp1, fp2)).toStringDiagram()
     *      ^
     *  
     *     ^    v
     *  js> // point butting up against centerline
     *  js> // NOTE doesn't float to center.  Is this correct?
     *  js> f1 = new Formation(Tools.m(
     *    >         Tools.p(StandardDancer.COUPLE_1_BOY, Position.getGrid(1,1,"e"))))
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 1 BOY=1,1,e}
     *    selected=[COUPLE 1 BOY]
     *  ]
     *  js> fp1 = new Breather.FormationPiece(f1, FormationList.SINGLE_DANCER); undefined
     *  js> Breather.breathe(Tools.l(fp1, fp2)).toStringDiagram()
     *     ^
     *  
     *     ^    v
     * @doc.test Middle of a run, runner breathes out slightly to make room:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> f = new Formation(Tools.m(
     *    >         Tools.p(StandardDancer.COUPLE_2_BOY, Position.getGrid(0,0,"n")),
     *    >         Tools.p(StandardDancer.COUPLE_2_GIRL, Position.getGrid(0,1,"e"))))
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 2 GIRL=0,1,e, COUPLE 2 BOY=0,0,n}
     *    selected=[COUPLE 2 GIRL, COUPLE 2 BOY]
     *  ]
     *  js> f.toStringDiagram()
     *  2G>
     *  2B^
     *  js> f = Breather.breathe(f)
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 2 GIRL=0,2,e, COUPLE 2 BOY=0,0,n}
     *    selected=[COUPLE 2 GIRL, COUPLE 2 BOY]
     *  ]
     *  js> f.toStringDiagram()
     *  2G>
     *  
     *  2B^
     * @doc.test Same thing with more dancers:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> f = new Formation(Tools.m(
     *    >         Tools.p(StandardDancer.COUPLE_3_BOY, Position.getGrid(-3,1,"n")),
     *    >         Tools.p(StandardDancer.COUPLE_4_GIRL,Position.getGrid(-3,0,"w")),
     *    >         Tools.p(StandardDancer.COUPLE_3_GIRL,Position.getGrid(-1,1,"e")),
     *    >         Tools.p(StandardDancer.COUPLE_4_BOY, Position.getGrid(-1,0,"s")),
     *    >         Tools.p(StandardDancer.COUPLE_2_BOY, Position.getGrid(1,1,"n")),
     *    >         Tools.p(StandardDancer.COUPLE_1_GIRL,Position.getGrid(1,0,"w")),
     *    >         Tools.p(StandardDancer.COUPLE_2_GIRL,Position.getGrid(3,1,"e")),
     *    >         Tools.p(StandardDancer.COUPLE_1_BOY, Position.getGrid(3,0,"s")))
     *    >     ).recenter();
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 3 BOY=-3,1/2,n, COUPLE 3 GIRL=-1,1/2,e, COUPLE 2 BOY=1,1/2,n, COUPLE 2 GIRL=3,1/2,e, COUPLE 4 GIRL=-3,-1/2,w, COUPLE 4 BOY=-1,-1/2,s, COUPLE 1 GIRL=1,-1/2,w, COUPLE 1 BOY=3,-1/2,s}
     *    selected=[COUPLE 3 BOY, COUPLE 3 GIRL, COUPLE 2 BOY, COUPLE 2 GIRL, COUPLE 4 GIRL, COUPLE 4 BOY, COUPLE 1 GIRL, COUPLE 1 BOY]
     *  ]
     *  js> f.toStringDiagram()
     *  3B^  3G>  2B^  2G>
     *  4G<  4Bv  1G<  1Bv
     *  js> f = Breather.breathe(f)
     *  net.cscott.sdr.calls.Formation[
     *    location={COUPLE 3 BOY=-3,1,n, COUPLE 3 GIRL=-1,1,e, COUPLE 2 BOY=1,1,n, COUPLE 2 GIRL=3,1,e, COUPLE 4 GIRL=-3,-1,w, COUPLE 4 BOY=-1,-1,s, COUPLE 1 GIRL=1,-1,w, COUPLE 1 BOY=3,-1,s}
     *    selected=[COUPLE 3 BOY, COUPLE 3 GIRL, COUPLE 2 BOY, COUPLE 2 GIRL, COUPLE 4 GIRL, COUPLE 4 BOY, COUPLE 1 GIRL, COUPLE 1 BOY]
     *  ]
     *  js> f.toStringDiagram()
     *  3B^  3G>  2B^  2G>
     *
     *  4G<  4Bv  1G<  1Bv
     */
    // note that we resolve collisions in input formation, but ignore any
    // present in output formation.  That ensures that we don't unnecessarily
    // breathe space-invader calls, esp if the input formation is a single
    // dancer giving the orientation only (or the match was used solely
    // to assign tags).
    public static Formation breathe(List<FormationPiece> pieces) {
        try {
            return _breathe(pieces);
        } catch (ExCLError e) {
            throw new BadCallException("Can't breathe");
        }
    }
    private static Formation _breathe(List<FormationPiece> pieces) throws ExCLError {
        // Locate collisions and resolve them to miniwaves.
        pieces = resolveCollisions(pieces);
        // center all output formations
        pieces = centerOutputPieces(pieces);
        // Trim boundaries to resolve overlaps
        List<Box> inputBounds = trimOverlap(pieces);
	// Find and sort boundaries of component formations.
        Axis x = new Axis(), y = new Axis();
        for (int i=0; i<pieces.size(); i++) {
            FormationPiece fp = pieces.get(i);
            Box inBound = inputBounds.get(i);
            Box outBound = fp.output.bounds();
            x.bounds.put(inBound.ll.x, Fraction.ZERO);
            x.bounds.put(inBound.ur.x, Fraction.ZERO);
            y.bounds.put(inBound.ll.y, Fraction.ZERO);
            y.bounds.put(inBound.ur.y, Fraction.ZERO);
            x.addBit(inBound.ll.x, inBound.ur.x, outBound.width());
            y.addBit(inBound.ll.y, inBound.ur.y, outBound.height());
        }
        // make sure there's an entry for the centerline, even if no dancer
        // is adjacent.
        x.bounds.put(Fraction.ZERO, Fraction.ZERO);
        y.bounds.put(Fraction.ZERO, Fraction.ZERO);
        // okay, now expand our formations, until all our constraints are met
        for (Axis axis: l(x, y)) {
            // use Cassowary constraint solver (basic linear programming)
            // to expand formation.

            // solver setup: create variables for each boundary point;
            //               objective function minimizes all boundaries
            ClSimplexSolver solver = new ClSimplexSolver();
            Map<Fraction, ClVariable> vars =
                new LinkedHashMap<Fraction, ClVariable>();
            for (Fraction f: axis.bounds.keySet()) {
                ClVariable v = new ClVariable(Fraction.ZERO);
                ClStrength s = f.equals(Fraction.ZERO) ?
                        ClStrength.required : ClStrength.weak;
                solver.addConstraint(new ClLinearEquation(v, Fraction.ZERO, s));
                vars.put(f, v);
            }
            assert vars.containsKey(Fraction.ZERO);
            // Constraint 1: Boundaries need to be strictly increasing
            //               (required constraint)
            Fraction last = null;
            for (Fraction f : axis.bounds.keySet()) {
                if (last!=null)
                    solver.addConstraint(new ClLinearInequality
                            (vars.get(f), CL.Op.GEQ, vars.get(last)));
                last = f;
            }
            // Constraint 2: Must fit formation (outer-inner >= size)
            //               (required constraint)
            for (Bit b : axis.bits) {
                ClVariable lo = vars.get(b.start), hi = vars.get(b.end);
                solver.addConstraint(new ClLinearInequality
                        (CL.Plus(lo, b.size), CL.Op.LEQ, hi));
            }
            // Symmetry constraint: moving from edges in, gaps should
            // be equal. (strong constraint, not required)
            for (Bit b : axis.bits) {
                // (inner and outer are actually reversed for negative coords,
                //  but it doesn't matter)
                Fraction lastInner = b.start, lastOuter = b.end;
                while(true) {
                    Fraction inner = axis.bounds.higherKey(lastInner);
                    Fraction outer = axis.bounds.lowerKey(lastOuter);
                    if (inner.compareTo(outer) >= 0) break; // done.
                    // okay, compare size of inner gap (inner-lastInner)
                    // to outer gap (lastOuter-outer).
                    ClLinearExpression innerSize =
                        CL.Minus(vars.get(inner), vars.get(lastInner));
                    ClLinearExpression outerSize =
                        CL.Minus(vars.get(lastOuter), vars.get(outer));
                    solver.addConstraintNoException(new ClLinearEquation
                            (innerSize, outerSize, ClStrength.strong));
                    lastInner = inner; lastOuter = outer;
                }
            }
            // okay, read out the results.
            for (Fraction f : axis.bounds.keySet()) {
                axis.bounds.put(f, vars.get(f).value());
            }
        }
        // assemble meta formation.
        Map<Dancer,Position> nf = new LinkedHashMap<Dancer,Position>();
        for (int i=0; i<pieces.size(); i++) {
            FormationPiece fp = pieces.get(i);
            Box origBounds = inputBounds.get(i);
            Box newBounds = new Box(new Point(x.bounds.get(origBounds.ll.x),
                                              y.bounds.get(origBounds.ll.y)),
                                    new Point(x.bounds.get(origBounds.ur.x),
                                              y.bounds.get(origBounds.ur.y)));
            Point newCenter = newBounds.center();
            // translate the output formation to this center.
            for (Dancer d: fp.output.dancers()) {
                Position oldPos = fp.output.location(d);
                nf.put(d, oldPos.relocate(oldPos.x.add(newCenter.x),
                                          oldPos.y.add(newCenter.y),
                                          oldPos.facing));
            }
        }
        return new Formation(nf);
    }
    /** Abstract representation of one dimension of a formation, used
     * for the expansion algorithm. */
    private static class Bit {
        /** Original boundary corresponding to the inner border of the
         *  formation. */
        final Fraction start;
        /** Original boundary corresponding to the outer border of the
         *  formation. */
        final Fraction end;
        /** Minimum size needed for this formation piece. */
        final Fraction size;
        public Bit(Fraction start, Fraction end, Fraction size) {
            this.start = start;
            this.end = end;
            this.size = size;
        }
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("start", start.toProperString())
                .append("end", end.toProperString())
                .append("size", size.toProperString())
                .toString();
        }
    }
    /** State associated with the x or y axis; since we expand each axis
     * separately, it's nice to abstract away exactly which one we're dealing
     * with. */
    private static class Axis {
        final TreeMap<Fraction,Fraction> bounds =
            new TreeMap<Fraction,Fraction>();
        final List<Bit> bits =
            new ArrayList<Bit>();
        public Axis() {}
        void addBit(Fraction start, Fraction end, Fraction size) {
            // if this bit straddles zero, add two bits of half the size
            if ((start.compareTo(Fraction.ZERO) >= 0) !=
                (end.compareTo(Fraction.ZERO) > 0) ) {
                addBit(start, Fraction.ZERO, size.divide(Fraction.TWO));
                addBit(Fraction.ZERO, end, size.divide(Fraction.TWO));
            } else {
                // otherwise, just add the bit
                bits.add(new Bit(start, end, size));
            }
        }
        public String toString() {
            return new ToStringBuilder(this, SdrToString.STYLE)
                .append("bounds", bounds)
                .append("bits", bits)
                .toString();
        }
    }
    /** Locate collisions and resolve them to miniwaves. */
    private static List<FormationPiece> resolveCollisions(List<FormationPiece>
                                                          pieces) {
        // hash to collect pieces with the same center
        ListMultiMap<Point,FormationPiece> mm = mml();
        for (FormationPiece fp : pieces)
            mm.add(fp.input.bounds().center(), fp);
        // now assemble result list of FormationPieces, merging collisions as
        // we find them.
        List<FormationPiece> result =
            new ArrayList<FormationPiece>(pieces.size());
        for (Point center: mm.keySet()) {
            List<FormationPiece> l= mm.getValues(center);
            switch(l.size()) {
            case 1:
                // no collision
                result.add(l.get(0));
                break;
            case 2:
                // collision!
                result.add(collide(l.get(0), l.get(1)));
                break;
            default:
                // illegal if more then 2 dancers collide on a spot
                throw new BadCallException("more than two dancers colliding");
            }
        }
        return result;
    }
    /* Ensure that fp.output is centered, for every formation pieces. */
    private static List<FormationPiece> centerOutputPieces(List<FormationPiece>
                                                           pieces) {
        return foreach(pieces, new F<FormationPiece,FormationPiece>() {
            @Override
            public FormationPiece map(FormationPiece fp) {
                return new FormationPiece(fp.input, fp.output.recenter());
            }
        });
    }
    /** Collide two formation pieces, creating a new FormationPiece
     * with the resulting miniwave of pieces. */
    private static FormationPiece collide(FormationPiece a, FormationPiece b) {
        boolean passLeft = isLeft(a.input);
        if (passLeft != isLeft(b.input))
            throw new BadCallException("inconsistent passing shoulder");
        Formation meta = passLeft ?
                FormationList.LH_MINIWAVE : FormationList.RH_MINIWAVE ;
        Dancer[] dd = meta.sortedDancers().toArray(new Dancer[2]);
        // use rotation of a.input and b.input to determine
        // how to rotate meta formation, such that 'a' maps to dd[0]
        // and 'b' maps to dd[1]
        ExactRotation[] rr = new ExactRotation[] {
                (ExactRotation) formationFacing(a.input, Fraction.ONE),
                (ExactRotation) formationFacing(b.input, Fraction.ONE)
        };
        if (rr[0]==null || rr[1]==null)
            throw new BadCallException("inconsistent facing direction");
        if (!rr[0].add(Fraction.ONE_HALF).equals(rr[1]))
            throw new BadCallException("collision but not facing opposite");
        meta = meta.rotate(rr[0].subtract(meta.location(dd[0]).facing.amount));
        // this is a little odd; insert wants to rotate the output formations
        // to match the facing directions in the meta formation.  But our
        // output formations are already facing the right way, so force all
        // the facing directions in the meta to 'north'
        for (Dancer d : meta.dancers()) {
            Position p = meta.location(d);
            meta = meta.move(d, p.relocate(p.x, p.y, ExactRotation.ZERO));
        }
        // okay, put the pieces together!
        Formation f = insert(meta, m(p(dd[0],a.output),p(dd[1],b.output)));
        return new FormationPiece(a/*pick one arbitrarily*/.input, f);
    }
    /** Check that the {@link Position.Flag#PASS_LEFT} flag is consistent
     * on all dancers in {@code fp.input}, and return true if it is
     * present.
     */
    private static boolean isLeft(Formation f) {
        boolean sawRight = false, sawLeft = false;
        for (Dancer d : f.dancers()) {
            if (f.location(d).flags.contains(Position.Flag.PASS_LEFT))
                sawLeft = true;
            else
                sawRight = true;
        }
        assert sawLeft || sawRight : "what, no dancers?";
        if (sawLeft && sawRight)
            throw new BadCallException("inconsistent passing shoulder");
        return sawLeft;
    }
    /** Return a consistent facing direction (modulo the modulus) if the
     * formation f has one, or else return null. */
    private static Rotation formationFacing(Formation f, Fraction modulus){
        Rotation r = null;
        // find "most exact" facing direction (largest modulus)
        for (Dancer d : f.dancers()) {
            Rotation rr = f.location(d).facing;
            if (r==null || r.modulus.compareTo(rr.modulus) < 0)
                r = rr;
        }
        assert r!=null : "what, no dancers?";
        // normalize to the desired level of (in)exactness.
        if (r.modulus.compareTo(modulus) < 0)
            return null; // formation direction is vague
        r = Rotation.create(r.amount, modulus);
        // ensure all dancers are consistent with this.
        for (Dancer d : f.dancers()) {
            Rotation rr = f.location(d).facing;
            if (!r.includes(rr))
                return null; // inconsistent facing direction
        }
        return r;
    }
    /**
     * Create a list of trimmed bounding boxes <i>which do not overlap</i>
     * from the given list of potentially-overlapping {@link FormationPiece}s.
     * We are only concerned with the {@link FormationPiece#input} formations
     * in the {@link FormationPiece}s.  This is a mess of heuristics and
     * hacks.  Currently: we prefer to trim boundaries which are not shared
     * (ie, don't break existing proper handholds), and then order by the
     * size of the overlap, trimming smallest to largest overlap.  We also
     * have a special "star" recognition algorithm, and don't attempt to
     * trim edges involved in a star.  The goal is to ensure that as you move
     * the points of a diamond inward, you never force the centers apart, until
     * you get to the point where the points and centers are equidistant --
     * at that point you have a star.  If you continue bringing the points
     * in, you should really breathe out to a star (ie, still avoid breaking
     * the centers' existing handhold), but at the moment we'll breathe out
     * to a diamond instead: we only preserve stars if they already exist,
     * we never make stars.
     */
    // xxx test cases for all this
    private static List<Box> trimOverlap(List<FormationPiece> pieces) {
        List<Box> boundsList = new ArrayList<Box>(pieces.size());
        List<TrimBit> trimX = new ArrayList<TrimBit>(pieces.size());
        List<TrimBit> trimY = new ArrayList<TrimBit>(pieces.size());
        BorderCountMap borderCountX = new BorderCountMap();
        BorderCountMap borderCountY = new BorderCountMap();
        HandholdMap handholdMap = new HandholdMap();
        for (int i=0; i<pieces.size(); i++) {
            Formation input = pieces.get(i).input;
            Box bounds = input.bounds();
            boundsList.add(bounds);
            trimX.add(new TrimBit(input, boundsList, i, bounds.center(),
                                  true, borderCountX));
            trimY.add(new TrimBit(input, boundsList, i, bounds.center(),
                                  false, borderCountY));
            // keep track of handholds
            Rotation handholdDir = trimX.get(trimX.size()-1).handholdDir;
            if (handholdDir!=null) {
                HandPair hp = hands(bounds, handholdDir);
                handholdMap.add(hp.right, input);
                handholdMap.add(hp.left, input);
            }
        }
        // form all pairs
        List<TrimBitPair> trims = new ArrayList<TrimBitPair>();
        for (int i=0; i<pieces.size(); i++) {
            for (int j=i+1; j<pieces.size(); j++) {
                TrimBit aX = trimX.get(i), bX = trimX.get(j);
                TrimBit aY = trimY.get(i), bY = trimY.get(j);
                assert aX.input == aY.input && bX.input == bY.input;
                trims.add(new TrimBitPair(aX, bX, borderCountX, handholdMap));
                trims.add(new TrimBitPair(aY, bY, borderCountY, handholdMap));
            }
        }
        // sort the pairs into the desired resolution order
        // XXX this is a mess of hacks.
        Collections.sort(trims);
        // okay, now go through the pairs resolving the overlaps
        for (TrimBitPair tbp: trims) {
            // check that the formations actually overlap; may have already
            // been resolved.
            if (!tbp.lo.bounds().overlaps(tbp.hi.bounds())) continue;
            // and that this dimension overlaps, in case it's been fixed
            if (!tbp.lo.overlaps(tbp.hi)) continue;
            // maybe this has become a star; double check.
            // (don't try to push stars apart, or else breathing thars gets to
            // be a real problem!)
            if (isStar(tbp.lo, tbp.hi)) continue;
            // okay, a real problem!  Trim both sides back to the midpoint
            // of the overlap.
            tbp.trim();
        }
        // boundsList has been modified; return it now!
        return boundsList;
    }
    /** Returns true if a and b are a 'star'; that is, their "left hands"
     * or "right hands" meet at a point. Be conservative.
     */
    private static boolean isStar(TrimBit a, TrimBit b) {
        Rotation aR = a.handholdDir;
        Rotation bR = b.handholdDir;
        if (aR==null || bR==null)
            return false; // inconsistent facing dirs, not a star
        if ((!bR.add(Fraction.ONE_QUARTER).equals(aR)) &&
            (!aR.add(Fraction.ONE_QUARTER).equals(bR)))
            return false; // only a star if we're exactly 90 degrees off
        HandPair aH = hands(a.bounds(), aR), bH = hands(b.bounds(), bR);
        if (aH.right.equals(bH.right) || aH.right.equals(bH.left) ||
            aH.left.equals(bH.right) || aH.left.equals(bH.left)) {
            return true; // it's a perfect star!
        }
        return false;
    }
    private static class HandPair {
        Point right, left;
        HandPair(Point right, Point left) { this.right=right; this.left=left; }
    }
    private static HandPair hands(Box bounds, Rotation handholdDir) {
        ExactRotation er = new ExactRotation(handholdDir.normalize().amount);
        // left and right labels are somewhat arbitrary, since we've normalized
        // to a modulus of 1/2
        Point rightHand = boundaryPoint(bounds, er);
        Point leftHand = boundaryPoint(bounds, er.add(Fraction.ONE_HALF));
        return new HandPair(rightHand, leftHand);
    }
    private static Point boundaryPoint(Box bounds, ExactRotation facing) {
        Fraction x = facing.toX().multiply(bounds.width()).divide(Fraction.TWO);
        Fraction y = facing.toY().multiply(bounds.height()).divide(Fraction.TWO);
        Point center = bounds.center();
        return new Point(center.x.add(x), center.y.add(y));
    }
    private static class TrimBitPair implements Comparable<TrimBitPair>{
        final TrimBit lo, hi;
        final Fraction overlap;
        final int sharedEdges;
        final int handholds;
        TrimBitPair(TrimBit a, TrimBit b, BorderCountMap bcm, HandholdMap hhm) {
            // order inputs into a 'lo' and 'hi' trim bit.
            int c = a.getStart().compareTo(b.getStart());
            if (c==0) c = a.getEnd().compareTo(b.getEnd());
            if (c < 0) {
                this.lo = a;
                this.hi = b;
            } else {
                this.lo = b;
                this.hi = a;
            }
            // min of the highs
            Fraction minEnd = Collections.min(l(a.getEnd(),b.getEnd()));
            Fraction maxStart = Collections.max(l(a.getStart(),b.getStart()));
            // overlap is min of the highs minus max of the lows
            this.overlap = minEnd.subtract(maxStart);
            // now look at the borders which would be trimmed, and count
            // how many input formations share this border exactly (try not to
            // interfere with existing handholds)
            this.sharedEdges = bcm.get(minEnd) + bcm.get(maxStart);
            // how many handholds would break if we trimmed this bit?
            // (trimming would modify lo.end and hi.start)
            int handholds = 0;
            // does 'lo' have a handhold at its end?
            if (hasHandhold(hhm, lo, lo.getEnd()))
                handholds++;
            // does 'hi' have a handhold at its start?
            if (hasHandhold(hhm, hi, hi.getStart()))
                handholds++;
            this.handholds = handholds;
        }
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("lo", lo)
                .append("hi", hi)
                .append("overlap", overlap.toProperString())
                .append("sharedEdges", sharedEdges)
                .append("handholds", handholds)
                .toString();
        }
        private static boolean hasHandhold(HandholdMap hhm, TrimBit tb, Fraction edge) {
            if (!tb.handholdOnAxis()) return false;
            HandPair handPair = hands(tb.bounds(), tb.handholdDir);
            Fraction left = tb.isX ? handPair.left.x : handPair.left.y;
            Fraction right = tb.isX ? handPair.right.x : handPair.right.y;
            assert !(edge.equals(left) && edge.equals(right));
            Point p = edge.equals(left) ? handPair.left :
                      edge.equals(right) ? handPair.right : null;
            if (p==null) return false;
            int h = hhm.getValues(p).size();
            return (h>1);
        }

        /** Compare pairs by (first) whether it's a complete overlap (try to
         * resolve all other conflicts first, (second) the number of dancers
         * sharing the trimable edges (min first), and (third) by the amount of
         * overlap between them (min first).
         */
        public int compareTo(TrimBitPair tbp) {
            if (this.isCompleteOverlap() && !tbp.isCompleteOverlap())
                return 1;
            if (tbp.isCompleteOverlap() && !this.isCompleteOverlap())
                return -1;
            int c = this.handholds - tbp.handholds;
            if (c!=0) return c;
            c = this.sharedEdges - tbp.sharedEdges;
            if (c!=0) return c;
            c = this.overlap.compareTo(tbp.overlap);
            return c;
        }
        private boolean isCompleteOverlap() {
            return lo.getStart().equals(hi.getStart()) &&
                   lo.getEnd().equals(hi.getEnd());
        }
        /** Trim a pair down to the midpoint of their overlap. */
        public void trim() {
            // figure out which of a and b is the "low" one.
            if (isCompleteOverlap())
                throw new Error("can't trim when exactly overlapping!");
            // okay, now we're going to trim lo.end and hi.start to their avg
            Fraction newEdge = lo.getEnd().add(hi.getStart())
                .divide(Fraction.TWO);
            lo.setEnd(newEdge);
            hi.setStart(newEdge);
            // ta-da!
        }
    }
    private static class TrimBit {
        /** The index of the FormationPiece corresponding to this. */
        final int idx;
        /** Pointer to a shared copy of the bounds list. */
        final List<Box> boundsList;
        /** Is this an X slice or a Y slice? */
        final boolean isX;
        /** Original input {@link Formation}. */
        final Formation input;
        /**
         * Formation "handhold" direction modulo 1/2, or {@code} null if it does
         * not have a consistent handhold direction.  For a dancer facing
         * north or south, the hand hold direction is "east and west"; for
         * dancers facing east or west, the hand hold direction is "north and
         * south", etc.
         */
        final Rotation handholdDir;

        public Box bounds() { return boundsList.get(idx); }
        void setBounds(Box newBounds) { boundsList.set(idx, newBounds); }
        public Fraction getStart() {
            return isX ? bounds().ll.x : bounds().ll.y;
        }
        public void setStart(Fraction f) {
            Box oldBounds = bounds();
            Point oldll = oldBounds.ll;
            Point newll = isX ? new Point(f, oldll.y) : new Point(oldll.x, f);
            setBounds(new Box(newll, oldBounds.ur));
        }
        public Fraction getEnd() {
            return isX ? bounds().ur.x : bounds().ur.y;
        }
        public void setEnd(Fraction f) {
            Box oldBounds = bounds();
            Point oldur = oldBounds.ur;
            Point newur = isX ? new Point(f, oldur.y) : new Point(oldur.x, f);
            setBounds(new Box(oldBounds.ll, newur));
        }
        /** Returns true if these bits overlap on this axis; the full 2d
         * boxes may not actually overlap. */
        public boolean overlaps(TrimBit b) {
            return this.getStart().compareTo(b.getEnd()) < 0 &&
                   b.getStart().compareTo(this.getEnd()) < 0;
        }
        /** Returns true if the "handholdDir" is consistent with "isX" */
        public boolean handholdOnAxis() {
            if (this.handholdDir==null) return false;
            return this.handholdDir.includes
                (isX ? ExactRotation.EAST : ExactRotation.NORTH);
        }
        TrimBit(Formation input, List<Box> boundsList, int idx, Point center,
                boolean isX, BorderCountMap borderCount) {
            this.input = input;
            Rotation hhD = formationFacing(input, Fraction.ONE_HALF);
            this.handholdDir = (hhD==null)?null:hhD.add(Fraction.ONE_QUARTER);
            this.boundsList = boundsList;
            this.idx = idx;
            this.isX = isX;
            borderCount.increment(getStart());
            borderCount.increment(getEnd());
        }
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("isX", isX)
            .append("start", getStart().toProperString())
            .append("end", getEnd().toProperString())
            .append("handhold", handholdDir.toAbsoluteString())
            .toString();
        }
    }
    private static class BorderCountMap extends HashMap<Fraction,Integer> {
            BorderCountMap() { super(); }
            public void increment(Fraction f) {
                this.put(f, this.get(f)+1);
            }
            public Integer get(Fraction f) {
                Integer i = super.get(f);
                return (i==null) ? Integer.valueOf(0) : i;
            }
    }
    private static class HandholdMap extends GenericMultiMap<Point, Formation> {
        HandholdMap() { super(); }
    }
}
