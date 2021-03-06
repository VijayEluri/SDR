package net.cscott.sdr.calls;

import java.util.ArrayList;
import java.util.List;

import net.cscott.jdoctest.JDoctestRunner;
import net.cscott.sdr.calls.DancerPath.PointOfRotation;
import net.cscott.sdr.calls.ast.Prim;
import net.cscott.sdr.calls.ast.Prim.Direction;
import net.cscott.sdr.util.Fraction;
import net.cscott.sdr.util.Point;

import org.junit.runner.RunWith;

/** Apply a {@link Prim} to a {@link Dancer}'s {@link Position} to yield a
 * {@link DancerPath} (which contains a new {@link Position}).
 * @author C. Scott Ananian
 * @version $Id: EvalPrim.java,v 1.1 2007-03-07 22:11:09 cananian Exp $
 * @doc.test Partner trade:
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> fm = MatcherList.COUPLE.match(Formation.FOUR_SQUARE); undefined
 *  js> f=[ff for (ff in Iterator(fm.matches.values()))
 *    >    if (ff.dancers().contains(StandardDancer.COUPLE_1_BOY))][0]
 *  net.cscott.sdr.calls.TaggedFormation[
 *    location={COUPLE 1 BOY=-1,0,n, COUPLE 1 GIRL=1,0,n}
 *    selected=[COUPLE 1 BOY, COUPLE 1 GIRL]
 *    tags={COUPLE 1 BOY=BEAU, COUPLE 1 GIRL=BELLE}
 *  ]
 *  js> f.toStringDiagram()
 *  1B^  1G^
 *  js> // first part of partner trade
 *  js> p1b = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f,
 *    >                      AstNode.valueOf('(Prim 1, 3, right, 3)'))
 *  DancerPath[from=-1,0,n,to=0,3,e,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> p1g = EvalPrim.apply(StandardDancer.COUPLE_1_GIRL, f,
 *    >                      AstNode.valueOf('(Prim -1, 1, left, 3)'))
 *  DancerPath[from=1,0,n,to=0,1,w,[ROLL_LEFT, SWEEP_RIGHT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1b.to).move(StandardDancer.COUPLE_1_GIRL, p1g.to); f.toStringDiagram()
 *  1B>
 *  
 *  1G<
 *  
 *  js> // second part of partner trade
 *  js> p1b = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f,
 *    >                      AstNode.valueOf('(Prim 3, 1, right, 3)'))
 *  DancerPath[from=0,3,e,[ROLL_RIGHT, SWEEP_LEFT],to=1,0,s,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> p1g = EvalPrim.apply(StandardDancer.COUPLE_1_GIRL, f,
 *    >                      AstNode.valueOf('(Prim -1, 1, left, 3)'))
 *  DancerPath[from=0,1,w,[ROLL_LEFT, SWEEP_RIGHT],to=-1,0,s,[ROLL_LEFT, SWEEP_RIGHT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1b.to).move(StandardDancer.COUPLE_1_GIRL, p1g.to); f.toStringDiagram()
 *  1Gv  1Bv
 * @doc.test Check that sweep direction computation doesn't crash if
 *  a dancer ends up on the center point:
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> f = Formation.FOUR_SQUARE ; undefined
 *  js> // first part of partner trade
 *  js> p1g = EvalPrim.apply(StandardDancer.COUPLE_1_GIRL, f,
 *    >                      AstNode.valueOf('(Prim -1, 1, left, 3)'))
 *  DancerPath[from=1,-1,n,to=0,0,w,[ROLL_LEFT],time=3,pointOfRotation=FOUR_DANCERS]
 * @doc.test Check that in/out motions are computed correctly:
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> fm = MatcherList.COUPLE.match(Formation.FOUR_SQUARE); undefined
 *  js> f=[ff for (ff in Iterator(fm.matches.values()))
 *    >    if (ff.dancers().contains(StandardDancer.COUPLE_1_BOY))][0]; f.toStringDiagram()
 *  1B^  1G^
 *  js> // boy face in, girl face out
 *  js> p1b = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f,
 *    >                      AstNode.valueOf('(Prim 0, 0, in 1/4, 3)'))
 *  DancerPath[from=-1,0,n,to=-1,0,e,[ROLL_RIGHT],time=3,pointOfRotation=SINGLE_DANCER]
 *  js> p1g = EvalPrim.apply(StandardDancer.COUPLE_1_GIRL, f,
 *    >                      AstNode.valueOf('(Prim 0, 0, out 1/4, 3)'))
 *  DancerPath[from=1,0,n,to=1,0,e,[ROLL_RIGHT],time=3,pointOfRotation=SINGLE_DANCER]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1b.to).move(StandardDancer.COUPLE_1_GIRL, p1g.to); f.toStringDiagram()
 *  1B>  1G>
 * @doc.test Check roll/sweep direction modifiers with a circle left:
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> f = Formation.FOUR_SQUARE; f.toStringDiagramWithDetails()
 *  3Gv  3Bv           v    v
 *  
 *  1B^  1G^           ^    ^
 *  js> // circle left 1/4 to set up sweep and roll
 *  js> // (modifiers should yield zeros here; not yet any sweep or roll)
 *  js> primBoy= AstNode.valueOf('(Prim sweep 2, 2, right, 2)')
 *  (Prim sweep 2, 2, right, 2)
 *  js> primGirl= AstNode.valueOf('(Prim -2, roll 2, right, 2)')
 *  (Prim -2, roll 2, right, 2)
 *  js> paths =
 *    > [[d,EvalPrim.apply(d, f, d.isBoy() ? primBoy : primGirl)] for each
 *    >   (d in Iterator(f.dancers()))]; undefined
 *  js> paths.forEach(function(e) { f = f.move(e[0], e[1].to); });
 *  js> // now we have roll right and sweep left
 *  js> f.toStringDiagramWithDetails()
 *  1B>  3G<         RL>  RL<
 *  
 *  1G>  3B<         RL>  RL<
 *  js> primBoy = AstNode.valueOf('(Prim 0, sweep 2, roll 1/4, 2)')
 *  (Prim 0, sweep 2, roll 1/4, 2)
 *  js> primGirl = AstNode.valueOf('(Prim sweep 2, 0, roll 1/4, 2)')
 *  (Prim sweep 2, 0, roll 1/4, 2)
 *  js> paths =
 *    > [[d,EvalPrim.apply(d, f, d.isBoy() ? primBoy : primGirl)] for each
 *    >   (d in Iterator(f.dancers()))]; undefined
 *  js> paths.forEach(function(e) { f = f.move(e[0], e[1].to); });
 *  js> f.toStringDiagramWithDetails()
 *  1Gv  1Bv         RLv  RLv
 *  
 *  3B^  3G^         RL^  RL^
 * @doc.test Check roll/sweep direction modifiers with a counter rotate:
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> f = Formation.FOUR_SQUARE; f.toStringDiagramWithDetails()
 *  3Gv  3Bv           v    v
 *  
 *  1B^  1G^           ^    ^
 *  js> // counter rotate 1/4 to set up sweep and roll
 *  js> // (modifiers should yield zeros here; not yet any sweep or roll)
 *  js> prim = AstNode.valueOf('(Prim sweep 2, 2, in 1/4, 2)')
 *  (Prim sweep 2, 2, in 1/4, 2)
 *  js> paths = [[d,EvalPrim.apply(d, f, prim)] for each
 *    >   (d in Iterator(f.dancers()))]; undefined
 *  js> paths.forEach(function(e) { f = f.move(e[0], e[1].to); });
 *  js> // now we have a mix of roll and sweep directions
 *  js> f.toStringDiagramWithDetails()
 *  1B>  1G<         RL>  LR<
 *  
 *  3G>  3B<         LR>  RL<
 *  js> prim = AstNode.valueOf('(Prim 0, sweep 2, roll -1/4, 2)')
 *  (Prim 0, sweep 2, roll -1/4, 2)
 *  js> paths = [[d,EvalPrim.apply(d, f, prim)] for each
 *    >   (d in Iterator(f.dancers()))]; undefined
 *  js> paths.forEach(function(e) { f = f.move(e[0], e[1].to); });
 *  js> f.toStringDiagramWithDetails()
 *  1G^  1B^         RR^  LL^
 *  
 *  3Bv  3Gv         LLv  RRv
 *  js> // veer in reverse sweep direction, should put everyone on origin
 *  js> prim = AstNode.valueOf('(Prim sweep -1, in 1, none, 2)')
 *  (Prim sweep -1, in 1, none, 2)
 *  js> [EvalPrim.apply(d, f, prim).to for each
 *    >  (d in Iterator(f.sortedDancers()))]
 *  0,0,n,0,0,n,0,0,s,0,0,s
 * @doc.test Check that roll/sweep work, even if you turn more than 360 degrees:
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> f=Formation.SQUARED_SET.select(StandardDancer.COUPLE_1_BOY,
 *    >       StandardDancer.COUPLE_1_GIRL).onlySelected(); f.toStringDiagram()
 *  
 *  
 *  1B^  1G^
 *  js> prim = AstNode.valueOf('(Prim -2, 4, right, 3)')
 *  (Prim -2, 4, right, 3)
 *  js> f.location(StandardDancer.COUPLE_1_BOY).facing.amount
 *  0/1
 *  js> p1a = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim)
 *  DancerPath[from=-1,-3,n,to=-3,1,e,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1a.to); f.toStringDiagram()
 *  1B>
 *  
 *  
 *  
 *            1G^
 *  js> f.location(StandardDancer.COUPLE_1_BOY).facing.amount
 *  1/4
 *  js> p1b = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim)
 *  DancerPath[from=-3,1,e,[ROLL_RIGHT, SWEEP_LEFT],to=1,3,s,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1b.to); f.toStringDiagram()
 *   1Bv
 *  
 *  
 *  
 *  
 *  
 *   1G^
 *  js> f.location(StandardDancer.COUPLE_1_BOY).facing.amount
 *  1/2
 *  js> p1c = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim)
 *  DancerPath[from=1,3,s,[ROLL_RIGHT, SWEEP_LEFT],to=3,-1,w,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1c.to); f.toStringDiagram()
 *        1B<
 *  
 *   1G^
 *  js> f.location(StandardDancer.COUPLE_1_BOY).facing.amount
 *  3/4
 *  js> p1d = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim)
 *  DancerPath[from=3,-1,w,[ROLL_RIGHT, SWEEP_LEFT],to=-1,-3,n,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1d.to); f.toStringDiagram()
 *  
 *  
 *  1B^  1G^
 *  js> f.location(StandardDancer.COUPLE_1_BOY).facing.amount
 *  1/1
 *  js> p1e = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim)
 *  DancerPath[from=-1,-3,n,[ROLL_RIGHT, SWEEP_LEFT],to=-3,1,e,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=TWO_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1e.to); f.toStringDiagram()
 *  1B>
 *  
 *  
 *  
 *            1G^
 *  js> f.location(StandardDancer.COUPLE_1_BOY).facing.amount
 *  5/4
 * @doc.test Scoot back. Note: trailers can't roll at finish; and the
 *  first (extend) part of the call doesn't have a sweep direction because the
 *  dancers don't end up facing the center.
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> f = Formation.FOUR_SQUARE; f.toStringDiagram()
 *  3Gv  3Bv
 *  
 *  1B^  1G^
 *  js> // girls u-turn back
 *  js> for each (d in [StandardDancer.COUPLE_1_GIRL, StandardDancer.COUPLE_3_GIRL]) {
 *    >   f=f.move(d,f.location(d).turn(net.cscott.sdr.util.Fraction.ONE_HALF,false));
 *    > }; f.toStringDiagram()
 *  3G^  3Bv
 *  
 *  1B^  1Gv
 *  js> // make the primitives we'll need
 *  js> prim1 = AstNode.valueOf('(Prim 0,1,none,1 1/2)')
 *  (Prim 0, 1, none, 1 1/2)
 *  js> prim2 = AstNode.valueOf('(Prim in 1,1,in 1/4,1 1/2)')
 *  (Prim in 1, 1, in 1/4, 1 1/2)
 *  js> prim3 = AstNode.valueOf('(Prim in 1,1,in 1/4,3)')
 *  (Prim in 1, 1, in 1/4, 3)
 *  js> // trailers extend
 *  js> p1 = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim1)
 *  DancerPath[from=-1,-1,n,to=-1,0,n,[SWEEP_LEFT],time=1 1/2,pointOfRotation=<null>]
 *  js> p3 = EvalPrim.apply(StandardDancer.COUPLE_3_BOY, f, prim1)
 *  DancerPath[from=1,1,s,to=1,0,s,[SWEEP_LEFT],time=1 1/2,pointOfRotation=<null>]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1.to).move(
 *    >            StandardDancer.COUPLE_3_BOY, p3.to); f.toStringDiagram()
 *  3G^
 *  1B^  3Bv
 *       1Gv
 *  js> Breather.breathe(f).toStringDiagram() // show proper spacing
 *  3G^
 *  
 *  1B^  3Bv
 *  
 *       1Gv
 *  js> // everyone start a trade
 *  js> p1 = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim2)
 *  DancerPath[from=-1,0,n,[SWEEP_LEFT],to=0,1,e,[ROLL_RIGHT, SWEEP_LEFT],time=1 1/2,pointOfRotation=FOUR_DANCERS]
 *  js> p2 = EvalPrim.apply(StandardDancer.COUPLE_1_GIRL, f, prim3)
 *  DancerPath[from=1,-1,s,to=0,-2,w,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=FOUR_DANCERS]
 *  js> p3 = EvalPrim.apply(StandardDancer.COUPLE_3_BOY, f, prim2)
 *  DancerPath[from=1,0,s,[SWEEP_LEFT],to=0,-1,w,[ROLL_RIGHT, SWEEP_LEFT],time=1 1/2,pointOfRotation=FOUR_DANCERS]
 *  js> p4 = EvalPrim.apply(StandardDancer.COUPLE_3_GIRL, f, prim3)
 *  DancerPath[from=-1,1,n,to=0,2,e,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=FOUR_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1.to).move(
 *    >            StandardDancer.COUPLE_1_GIRL, p2.to).move(
 *    >            StandardDancer.COUPLE_3_BOY, p3.to).move(
 *    >            StandardDancer.COUPLE_3_GIRL, p4.to); f.toStringDiagram('| ',Formation.dancerNames)
 *  | 3G>
 *  | 1B>
 *  | 
 *  | 3B<
 *  | 1G<
 *  js> // boys finish the trade
 *  js> p1 = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim2)
 *  DancerPath[from=0,1,e,[ROLL_RIGHT, SWEEP_LEFT],to=1,0,s,[ROLL_RIGHT, SWEEP_LEFT],time=1 1/2,pointOfRotation=FOUR_DANCERS]
 *  js> p3 = EvalPrim.apply(StandardDancer.COUPLE_3_BOY, f, prim2)
 *  DancerPath[from=0,-1,w,[ROLL_RIGHT, SWEEP_LEFT],to=-1,0,n,[ROLL_RIGHT, SWEEP_LEFT],time=1 1/2,pointOfRotation=FOUR_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1.to).move(
 *    >            StandardDancer.COUPLE_3_BOY, p3.to); f.toStringDiagram()
 *    3G>
 *  
 *  3B^  1Bv
 *  
 *    1G<
 *  js> // boys extend, girls finish the trade. girls can roll and sweep.
 *  js> p1 = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f, prim1)
 *  DancerPath[from=1,0,s,[ROLL_RIGHT, SWEEP_LEFT],to=1,-1,s,[SWEEP_LEFT],time=1 1/2,pointOfRotation=<null>]
 *  js> p2 = EvalPrim.apply(StandardDancer.COUPLE_1_GIRL, f, prim3)
 *  DancerPath[from=0,-2,w,[ROLL_RIGHT, SWEEP_LEFT],to=-1,-1,n,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=FOUR_DANCERS]
 *  js> p3 = EvalPrim.apply(StandardDancer.COUPLE_3_BOY, f, prim1)
 *  DancerPath[from=-1,0,n,[ROLL_RIGHT, SWEEP_LEFT],to=-1,1,n,[SWEEP_LEFT],time=1 1/2,pointOfRotation=<null>]
 *  js> p4 = EvalPrim.apply(StandardDancer.COUPLE_3_GIRL, f, prim3)
 *  DancerPath[from=0,2,e,[ROLL_RIGHT, SWEEP_LEFT],to=1,1,s,[ROLL_RIGHT, SWEEP_LEFT],time=3,pointOfRotation=FOUR_DANCERS]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1.to).move(
 *    >            StandardDancer.COUPLE_1_GIRL, p2.to).move(
 *    >            StandardDancer.COUPLE_3_BOY, p3.to).move(
 *    >            StandardDancer.COUPLE_3_GIRL, p4.to); f.toStringDiagram()
 *  3B^  3Gv
 *  
 *  1G^  1Bv
 *  js> // roll!
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1.to.turn(p1.to.roll(),false)).move(
 *    >            StandardDancer.COUPLE_1_GIRL, p2.to.turn(p2.to.roll(),false)).move(
 *    >            StandardDancer.COUPLE_3_BOY, p3.to.turn(p3.to.roll(),false)).move(
 *    >            StandardDancer.COUPLE_3_GIRL, p4.to.turn(p4.to.roll(),false)); f.toStringDiagram()
 *  3B^  3G<
 *  
 *  1G>  1Bv
 * @doc.test Prims which "stand still" preserve roll by including the
 *  PRESERVE_ROLL flag.
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> fm = MatcherList.COUPLE.match(Formation.FOUR_SQUARE); undefined
 *  js> f=[ff for (ff in Iterator(fm.matches.values()))
 *    >    if (ff.dancers().contains(StandardDancer.COUPLE_1_BOY))][0]; f.toStringDiagram()
 *  1B^  1G^
 *  js> // boy face out
 *  js> p1a = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f,
 *    >                      AstNode.valueOf('(Prim 0, 0, out 1/4, 3)'))
 *  DancerPath[from=-1,0,n,to=-1,0,w,[ROLL_LEFT],time=3,pointOfRotation=SINGLE_DANCER]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1a.to); f.toStringDiagram()
 *  1B<  1G^
 *  js> // do nothing (but preserve roll)
 *  js> p1b = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f,
 *    >                      AstNode.valueOf('(Prim 0, 0, none, 3, preserve-roll)'))
 *  DancerPath[from=-1,0,w,[ROLL_LEFT],to=-1,0,w,[ROLL_LEFT],time=3,pointOfRotation=<null>]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1b.to); f.toStringDiagram()
 *  1B<  1G^
 *  js> // do nothing (not preserving roll)
 *  js> p1c = EvalPrim.apply(StandardDancer.COUPLE_1_BOY, f,
 *    >                      AstNode.valueOf('(Prim 0, 0, none, 3)'))
 *  DancerPath[from=-1,0,w,[ROLL_LEFT],to=-1,0,w,time=3,pointOfRotation=<null>]
 *  js> f = f.move(StandardDancer.COUPLE_1_BOY, p1c.to); f.toStringDiagram()
 *  1B<  1G^
 * @doc.test Tricky 45-degree off Prim.  This is the outsides part of a fan the
 *  top, broken in halves.
 *  js> importPackage(net.cscott.sdr.calls.ast)
 *  js> f = FormationList.SINGLE_DANCER; d = f.dancers().iterator().next();
 *  <phantom@7b>
 *  js> p = EvalPrim.apply(d, f, AstNode.valueOf('(Prim 0, 3, -1/8, 1 1/2)'))
 *  DancerPath[from=0,0,n,to=0,3,nw,[ROLL_LEFT],time=1 1/2,pointOfRotation=SINGLE_DANCER]
 *  js> f = f.move(d, p.to) ; undefined
 *  js> p = EvalPrim.apply(d, f, AstNode.valueOf('(Prim -1 1/2, 1 1/2, -1/8, 1 1/2)'))
 *  DancerPath[from=0,3,nw,[ROLL_LEFT],to=-3,3,w,[ROLL_LEFT, SWEEP_RIGHT],time=1 1/2,pointOfRotation=SINGLE_DANCER]
 */
// xxx should test circle left & check roll
//     add "force roll" flags to Prim (only print if set)
@RunWith(value=JDoctestRunner.class)
public abstract class EvalPrim {

    /** "Dance" the given primitive for the given dancer in the given
     * formation to yield a {@link DancerPath} for that dancer. */
    public static DancerPath apply(Dancer d, Formation f, Prim p) {
        return apply(p, f.location(d), f.dancers().size());
    }
    private static Fraction computeRollSweep(Fraction amt, Direction d,
                                             Position reference,
                                             Position p1, Position p2) {
        Fraction rollSweep;
        if (d == Direction.SWEEP) {
            rollSweep = reference.sweep();
        } else if (d == Direction.ROLL) {
            rollSweep = reference.roll();
        } else {
            return amt;
        }
        // is p1 -> p2 cw or ccw?
        if (p1 != null && p2 != null && !rollSweep.equals(Fraction.ZERO)) {
            ExactRotation r1 = ExactRotation.fromXY(p1.x, p1.y);
            ExactRotation r2 = ExactRotation.fromXY(p2.x, p2.y);
            // only the sign (and zero-ness) is important here
            rollSweep = rollSweep.multiply(r1.minSweep(r2));
            // flip sign by multiplying by amt again, since if amt is negative
            // we want to go in the *anti* roll/sweep direction.
            rollSweep = rollSweep.multiply(amt);
        }
        int cmp = rollSweep.compareTo(Fraction.ZERO);
        return (cmp==0) ? Fraction.ZERO : (cmp < 0) ? amt.negate() : amt;
    }
    /** "Dance" the given primitive from the given position (in a
     * formation of the given size) to yield a {@link DancerPath}. */
    public static DancerPath apply(Prim prim, Position from, int formationSize) {
	// use the center of the formation as the center point for
        // in/out/etc.  We deal with 'pass in' (which explicitly wants
        // to reference the flagpole center of the set) by
        // re-evaluating to an 8-person formation before the quarter in.
        final Point center = new Point(Fraction.ZERO, Fraction.ZERO);
        // evaluate using an exact rotation; we'll check if that makes
        // sense later
        Rotation fromRot = from.facing;
        if (!fromRot.isExact())
            from = from.relocate(new ExactRotation(fromRot.amount));
        // evaluate ROLL/SWEEP modifiers
        Fraction fwdAmt = computeRollSweep
            (prim.y, prim.dirY, from,
             from.forwardStep(prim.y, false),
             from.forwardStep(prim.y.negate(), false));
        Fraction sideAmt = computeRollSweep
            (prim.x, prim.dirX, from,
             from.sideStep(prim.x, false),
             from.sideStep(prim.x.negate(), false));
        Fraction turnAmt =
            computeRollSweep(prim.rot.amount, prim.dirRot, from, null, null);
        // apply 'in/out' modifier to x/y/rotation
	Position to = from
	    .forwardStep(fwdAmt, prim.dirY==Direction.IN)
	    .sideStep(sideAmt, prim.dirX==Direction.IN)
            .turn(turnAmt, prim.dirRot==Direction.IN, from);
        // set the point of rotation based on the size of the formation
        // XXX: we may need to do something smarter here eventually.
        // XXX: por should be the center of our formation, and mapped back
        //      to a real physical center?
        PointOfRotation por = null;
        switch(formationSize) {
        case 1: por = PointOfRotation.SINGLE_DANCER; break;
        case 2: por = PointOfRotation.TWO_DANCERS; break;
        case 4: por = PointOfRotation.FOUR_DANCERS; break;
        case 8: por = PointOfRotation.SQUARE_CENTER; break;
        default: break;
        }
        // the arc center is the center of the formation, although we should
        // set it to null if this motion doesn't involve rotation, or to the
        // dancer's location if only rotation is involved.
        Point arcCenter;
        if (prim.flags.contains(Prim.Flag.FORCE_ARC))
            arcCenter = center;
        else if (to.facing.equals(from.facing)) {
            arcCenter = null;
            por = null;
        } else if (to.x.equals(from.x) && to.y.equals(from.y)) {
            arcCenter = new Point(to.x, to.y);
            por = PointOfRotation.SINGLE_DANCER;
        } else
            arcCenter = center;
        // we'll set rolldir equal to dr.
        ExactRotation rollDir;
        if (from.facing.isExact())
            rollDir = (ExactRotation) to.facing.subtract(from.facing.amount);
        else
            rollDir = ExactRotation.ZERO;

        // sweep dir is set based on angle swept through center from 'from' to
        // 'to', although of course remember the 'sweep' call is only valid
        // if we end up 'as couples'
        ExactRotation sweepDir;
        if ((from.x.compareTo(center.x)==0 && from.y.compareTo(center.y)==0) ||
            (to.x.compareTo(center.x)==0 && to.y.compareTo(center.y)==0)) {
            // we start or end dead center.  probably this means that our
            // center of rotation is wrong, but it's better just to say
            // "can't sweep".
            sweepDir = ExactRotation.ZERO; // we end up dead center.
        } else {
            ExactRotation fromSweep = ExactRotation.fromXY
                (from.x.subtract(center.x), from.y.subtract(center.y));
            ExactRotation toSweep = ExactRotation.fromXY
                (to.x.subtract(center.x), to.y.subtract(center.y));
            sweepDir = toSweep.subtract(fromSweep.amount).normalize();
            // we used to have code here to
            // zero out sweep dir unless dancer ends "facing center"
            // (ie, in the 90 degrees on either side of the vector joining
            //  them to the center point)
            // removed it because calls ending in "and roll" (ie, recycle,
            // wheel and deal==as couples(hinge and roll)) don't end facing
            // center prior to the roll, yet have sweep.
        }
        if (sweepDir.amount.compareTo(Fraction.ONE_HALF) > 0)
            sweepDir = sweepDir.subtract(Fraction.ONE);
        if (sweepDir.amount.compareTo(Fraction.ONE_HALF)==0)
            sweepDir = ExactRotation.ZERO; // XXX: can't tell sweep direction
        // since the dancers in a sweep are *facing the center*, the sweep
        // direction is the *opposite* of the sweep direction seen by an
        // observer standing at the origin
        sweepDir = sweepDir.negate();

	// preserve roll/sweep flags if this is a "stand still" prim.
        if (prim.flags.contains(Prim.Flag.PRESERVE_ROLL))
	    rollDir = new ExactRotation(from.roll());
        if (prim.flags.contains(Prim.Flag.PRESERVE_SWEEP))
	    sweepDir = new ExactRotation(from.sweep());
	// force-roll from Prim.Flags
	if (prim.flags.contains(Prim.Flag.FORCE_ROLL_RIGHT)) {
	    assert !prim.flags.contains(Prim.Flag.FORCE_ROLL_LEFT);
	    rollDir = ExactRotation.ONE_QUARTER;
	} else if (prim.flags.contains(Prim.Flag.FORCE_ROLL_LEFT)) {
	    rollDir = ExactRotation.mONE_QUARTER;
        } else if (prim.flags.contains(Prim.Flag.FORCE_ROLL_NONE)) {
            rollDir = ExactRotation.ZERO;
        }

	// set position flags
	to = addFlag(to, rollDir,
		     Position.Flag.ROLL_RIGHT, Position.Flag.ROLL_LEFT);
	to = addFlag(to, sweepDir,
		     Position.Flag.SWEEP_RIGHT, Position.Flag.SWEEP_LEFT);
	if (prim.flags.contains(Prim.Flag.PASS_LEFT))
	    to = to.addFlags(Position.Flag.PASS_LEFT);

        // set dancerpath flags
        List<DancerPath.Flag> flags = new ArrayList<DancerPath.Flag>();
        if (prim.flags.contains(Prim.Flag.SASHAY_START))
            flags.add(DancerPath.Flag.SASHAY_START);
        if (prim.flags.contains(Prim.Flag.SASHAY_FINISH))
            flags.add(DancerPath.Flag.SASHAY_FINISH);

        // restore inexact rotation, if necessary, and sanity-check
        if (!fromRot.isExact()) {
            to = to.relocate(new Rotation(to.facing.amount, fromRot.modulus));
            if ((prim.dirX==Direction.ASIS && !prim.x.equals(Fraction.ZERO)) ||
                (prim.dirY==Direction.ASIS && !prim.y.equals(Fraction.ZERO)))
                throw new BadCallException("absolute motion not possible with "+
                                           "uncertain facing direction");
            if (fromRot.modulus.compareTo(Fraction.ONE_HALF) < 0 &&
                !(prim.x.equals(Fraction.ZERO) && prim.y.equals(Fraction.ZERO)))
                throw new BadCallException("don't know where my nose is");
        }
        return new DancerPath(from, to, prim.time, por, flags);
    }
    /** Normalize an ExactRotation, and set appropriate 'right' or 'left'
     * {@link net.cscott.sdr.calls.Position.Flag}s to a given {@link Position} as appropriate.
     */
    private static Position addFlag(Position p, ExactRotation r,
				    Position.Flag right, Position.Flag left) {
        int c = r.amount.compareTo(Fraction.ZERO);
        if (c>0) return p.addFlags(right);
        else if (c<0) return p.addFlags(left);
        else return p;
    }
 }
