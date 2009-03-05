package net.cscott.sdr.calls;

import java.util.*;

import net.cscott.sdr.util.*;

/**
 * The {@link FormationMapper} class contains methods to reassemble and
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
 * @author C. Scott Ananian
 * @version $Id: FormationMapper.java,v 1.10 2006-10-30 22:09:29 cananian Exp $
 */
public class FormationMapper {
    
    /**
     * Insert formations into a meta-formation.  This reassembles the
     * formation after we've decomposed it into (say) boxes to do a
     * four-person call.
     *
     * @doc.test Insert COUPLEs, then TANDEMs into a RH_OCEAN_WAVE.  Then, for
     *  a challenge, insert TANDEMs into a DIAMOND to give a t-bone column:
     *  js> function xofy(meta, f) {
     *    >   var i=0
     *    >   var m=new java.util.LinkedHashMap()
     *    >   for (d in Iterator(meta.sortedDancers())) {
     *    >     var mm=new java.util.LinkedHashMap()
     *    >     for (dd in Iterator(f.sortedDancers())) {
     *    >       mm.put(dd, StandardDancer.values()[i++])
     *    >     }
     *    >     m.put(d, new Formation(f, mm))
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
     *  js> FormationMapper.insert(meta, m).toStringDiagram()
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
     *  js> FormationMapper.insert(meta, m).toStringDiagram()
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
     *  js> FormationMapper.insert(meta, m).toStringDiagram()
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
        // Rotate components to match orientations of meta dancers.
        Map<Dancer,Formation> sub =
            new HashMap<Dancer,Formation>(components.size());
        for (Dancer d : meta.dancers())
            sub.put(d, components.get(d).rotate
                    ((ExactRotation)meta.location(d).facing));
        // Find 'inner boundaries' of meta dancers.
        Set<Fraction> xiB = new HashSet<Fraction>();
        Set<Fraction> yiB = new HashSet<Fraction>();
        for (Dancer d : meta.dancers())
            addInner(meta.bounds(d), xiB, yiB);
        xiB.add(Fraction.ZERO); yiB.add(Fraction.ZERO);
        // sort boundaries.
        List<Fraction> xBound = new ArrayList<Fraction>(xiB);
        List<Fraction> yBound = new ArrayList<Fraction>(yiB);
        Collections.sort(xBound); Collections.sort(yBound);
        // for each dancer, find its boundaries & stretch to accomodate
        // work from center out.
        // Note that expansion occurs *between* elements of the original
        // boundary list, so it is 1 element shorter than the boundary list.
        // expansion[0] is the expansion between boundary[0] and boundary[1].
        List<Fraction> xExpand = new ArrayList<Fraction>
            (Collections.nCopies(xBound.size()-1,Fraction.ZERO));
        List<Fraction> yExpand = new ArrayList<Fraction>
            (Collections.nCopies(yBound.size()-1,Fraction.ZERO));
        List<Dancer> dancers = new ArrayList<Dancer>(meta.dancers());
        // sort dancers by absolute x
        Collections.sort(dancers, new DancerLocComparator(meta,true,true)); 
        for (Dancer d : meta.dancers()) {
            Formation f = sub.get(d);
            Box b = meta.bounds(d);
            expand(xExpand, xBound, b.ll.x, b.ur.x, f.bounds().width());
        }
        // sort dancers by absolute y
        Collections.sort(dancers, new DancerLocComparator(meta,false,true)); 
        for (Dancer d : meta.dancers()) {
            Formation f = sub.get(d);
            Box b = meta.bounds(d);
            expand(yExpand, yBound, b.ll.y, b.ur.y, f.bounds().height());
        }
        // now reassemble a new formation.
        Map<Dancer,Position> nf = new HashMap<Dancer,Position>();
        for (Dancer d : meta.dancers()) {
            Formation f = sub.get(d);
            // find the boundary this formation is going to hang off
            Box b = meta.bounds(d);
            Integer[] xb = findNearest(xBound, b.ll.x, b.ur.x);
            Integer[] yb = findNearest(yBound, b.ll.y, b.ur.y);
            place(nf, f, computeCenter(f.bounds(),
                    warpPair(xExpand,xBound,xb[0],xb[1]),
                    warpPair(yExpand,yBound,yb[0],yb[1])));
        }
        Formation result = new Formation(nf);
        assert result.isCentered();
        return result.recenter(); // belt & suspenders.
    }
    private static class DancerLocComparator implements Comparator<Dancer> {
        private final Formation f;
        private final boolean isX;
        private final boolean isAbs;
        DancerLocComparator(Formation f, boolean isX, boolean isAbs) {
            this.f = f; this.isX = isX; this.isAbs = isAbs;
        }
        public int compare(Dancer d1, Dancer d2) {
            Position p1 = f.location(d1), p2 = f.location(d2);
            Fraction xy1, xy2;
            if (isX) { xy1=p1.x; xy2=p2.x; }
            else { xy1=p1.y; xy2=p2.y; }
            if (isAbs) { xy1=xy1.abs(); xy2=xy2.abs(); }
            return xy1.compareTo(xy2);
        }
    }
    private static Fraction[] warpPair(List<Fraction> expansion, List<Fraction> boundary, Integer low, Integer high) {
        return new Fraction[] {
                low==null ? null : warp(expansion, boundary, boundary.get(low)),
                high==null ? null : warp(expansion, boundary, boundary.get(high))
        };
    }
    private static Point computeCenter(Box naturalBounds, Fraction[] x, Fraction[] y) {
        // compute the desired center of the formation.  if both bounds are
        // given, then the center is the mean.  Otherwise, align edge to the
        // known bound. If no bounds are given, put on the centerline.
        Fraction cx, cy;
        if (x[0]==null && x[1]==null)
            cx = Fraction.ZERO;
        else if (x[0]==null)
            cx = x[1].subtract(naturalBounds.width().divide(Fraction.TWO));
        else if (x[1]==null)
            cx = x[0].add(naturalBounds.width().divide(Fraction.TWO));
        else
            cx = x[0].add(x[1]).divide(Fraction.TWO);
        if (y[0]==null && y[1]==null)
            cy = Fraction.ZERO;
        else if (y[0]==null)
            cy = y[1].subtract(naturalBounds.height().divide(Fraction.TWO));
        else if (y[1]==null)
            cy = y[0].add(naturalBounds.height().divide(Fraction.TWO));
        else
            cy = y[0].add(y[1]).divide(Fraction.TWO);
        return new Point(cx, cy);
    }
    private static void place(Map<Dancer,Position> nf, Formation f, Point center) {
        for (Dancer d : f.dancers())
            nf.put(d, offset(f.location(d), center));
    }
    private static Position offset(Position p, Point offset) {
        return new Position(p.x.add(offset.x), p.y.add(offset.y), p.facing);
    }
    /** Compute the 'expanded' location of the given boundary value. */
    private static Fraction warp(List<Fraction> expansion,
                                 List<Fraction> boundary, Fraction val) {
        if (val.compareTo(Fraction.ZERO) >= 0) {
            Integer[] boundIndex = findNearest(boundary, Fraction.ZERO, val);
            // this should be an exact match.
            assert boundary.get(boundIndex[0]).compareTo(Fraction.ZERO)==0;
            assert boundary.get(boundIndex[1]).compareTo(val)==0;
            // okay, starting from zero, add up all the expansion.
            // note that expansion[0] corresponds to boundary[0]-boundary[1], etc
            for (int i=boundIndex[0]; i<boundIndex[1]; i++)
                val = val.add(expansion.get(i));
        } else {
            Integer[] boundIndex = findNearest(boundary, val, Fraction.ZERO);
            // this should be an exact match.
            assert boundary.get(boundIndex[1]).compareTo(Fraction.ZERO)==0;
            assert boundary.get(boundIndex[0]).compareTo(val)==0;
            // okay, starting from zero, add up all the expansion.
            // note that expansion[0] corresponds to boundary[0]-boundary[1], etc
            for (int i=boundIndex[0]; i<boundIndex[1]; i++)
                val = val.subtract(expansion.get(i));
        }
        return val; // done!
    }
    private static void expand(List<Fraction> expansion, List<Fraction> boundary,
                               Fraction dancerMin, Fraction dancerMax,
                               Fraction newSize) {
        Integer[] boundIndex = findNearest(boundary, dancerMin, dancerMax);
        // if either of the boundIndices is null, then the formation is
        // unconstrained and we don't need to expand anything.
        if (boundIndex[0]==null || boundIndex[1]==null) return;
        // otherwise, let's compute the (expanded) distance between boundIndices
        // is it big enough?
        assert boundIndex[0] < boundIndex[1];
        assert boundary.get(boundIndex[0]).compareTo(dancerMin) <= 0;
        assert boundary.get(boundIndex[1]).compareTo(dancerMax) >= 0;
        Fraction dist = Fraction.ZERO;
        for (int i=boundIndex[0]; i<boundIndex[1]; i++) {
            // add 'native' distance
            dist = dist.add(boundary.get(i+1).subtract(boundary.get(i)));
            // add in expansion to date.
            dist = dist.add(expansion.get(i));
        }
        if (dist.compareTo(newSize) >= 0) return; // no expansion needed
        // figure out how much expansion is needed...
        Fraction inc = newSize.subtract(dist).divide
            (Fraction.valueOf(boundIndex[1]-boundIndex[0]));
        // ... and add it into the expansion list
        for (int i=boundIndex[0]; i<boundIndex[1]; i++)
            expansion.set(i, expansion.get(i).add(inc));
        // okay, we've done the necessary expansion, we're done!
        return;
    }
    private static Integer[] findNearest(List<Fraction> boundary, Fraction dancerMin, Fraction dancerMax) {
        Integer[] result = new Integer[2];
        
        int bottom = Collections.binarySearch(boundary, dancerMin);
        if (bottom>=0) result[0]=bottom;
        else if (bottom==-1) result[0]=null; // past bottom shared edge.
        else result[0]=-bottom-2;
        
        int top = Collections.binarySearch(boundary, dancerMax);
        if (top>=0) result[1]=top;
        else if (top==(-boundary.size()-1)) result[1]=null; // past top shared edge.
        else result[1]=-top-1;
        
        return result;
    }
    /** Add inner boundaries of the given box to the boundary sets. */
    private static void addInner(Box b, Set<Fraction> xBounds,
            Set<Fraction> yBounds) {
        if (b.ll.x.compareTo(Fraction.ZERO)>0)
            xBounds.add(b.ll.x);
        if (b.ur.x.compareTo(Fraction.ZERO)<0)
            xBounds.add(b.ur.x);
        if (b.ll.y.compareTo(Fraction.ZERO)>0)
            yBounds.add(b.ll.y);
        if (b.ur.y.compareTo(Fraction.ZERO)<0)
            yBounds.add(b.ur.y);
    }

    /*-----------------------------------------------------------------------*/
    
    public static class FormationPiece {
        /** Warped rotated formation. The input formation is a simple
         * superposition of these. */
        public final Formation f;
        /** The (typically {@link PhantomDancer Phantom}) dancer who will
         * correspond to this in the output meta formation. */
        public final Dancer d;
        /** The rotation to use for this dancer in the output meta formation
         * (typically this is the rotation of formation {@code f} from
         * whatever the 'canonical' orientation is. */
        public final Rotation r;
        public FormationPiece(Formation f, Dancer d, Rotation r) {
            this.f = f;
            this.d = d;
            this.r = r;
        }
    }
    /**
     * Create a canonical formation by compressing the given one.  This
     * is just an invokation of {@link #breathe(List)} with
     * trivial {@link FormationPiece}s consisting of a single dancer each.
     *
     * @doc.test From couples back to back, step out; then breathe in:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
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
     *  js> FormationMapper.breathe(f).toStringDiagram()
     *  ^    ^
     *  
     *  v    v
     * @doc.test From facing couples, take half a step in; breathe out:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> f = FormationList.FACING_COUPLES ; f.toStringDiagram()
     *  v    v
     *  
     *  ^    ^
     *  js> for (d in Iterator(f.dancers())) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE_HALF, false));
     *    > }; f.toStringDiagram()
     *  v    v
     *  ^    ^
     *  js> // EXPECT FAIL
     *  js> FormationMapper.breathe(f).toStringDiagram()
     *  v    v
     *  
     *  ^    ^
     * @doc.test From single three quarter tag, step out; then breathe in:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
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
     *  js> FormationMapper.breathe(f).toStringDiagram()
     *    ^
     *  
     *  ^    v
     *  
     *    v
     * @doc.test From single quarter tag, take half a step in; breathe out:
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
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
     *  js> // EXPECT FAIL
     *  js> FormationMapper.breathe(f).toStringDiagram()
     *    v
     *  
     *  ^    v
     *  
     *    ^
     * @doc.test Facing dancers step forward; resolve collision.
     *  js> importPackage(net.cscott.sdr.util) // for Fraction
     *  js> f = FormationList.FACING_DANCERS ; f.toStringDiagram()
     *  v
     *  
     *  ^
     *  js> for (d in Iterator(f.dancers())) {
     *    >   f=f.move(d,f.location(d).forwardStep(Fraction.ONE, false));
     *    > }; f
     *  net.cscott.sdr.calls.TaggedFormation@1db3e20[
     *    location={<phantom@7f>=0,0,n, <phantom@7e>=0,0,s}
     *    selected=[<phantom@7f>, <phantom@7e>]
     *    tags={<phantom@7f>=TRAILER, <phantom@7e>=TRAILER}
     *  ]
     *  js> // EXPECT FAIL
     *  js> FormationMapper.breathe(f).toStringDiagram()
     *  ^    v
     */
    // XXX: add test case for left-hand collisions
    public static Formation breathe(Formation f) {
        List<FormationPiece> fpl = new ArrayList<FormationPiece>
            (f.dancers().size());
        for (Dancer d: f.dancers()) {
            Formation nf = f.select(Collections.singleton(d)).onlySelected();
            fpl.add(new FormationPiece(nf, d, f.location(d).facing));
        }
        return breathe(fpl);
    }
    /** Create canonical formation by compressing or expanding components of a given
     * formation.  (The map giving the correspondence between dancers in
     * the new formation and the input formations is given by the
     * individual {@link FormationPiece} objects.)  We also resolve
     * collisions to right or left hands, depending on whether the
     * pass-left flag is set for the {@link Position}s involved.
     */
    // XXX: compress() doesn't work for expansion yet; if you let the
    //      beaus extend from facing couples, this function won't (yet)
    //      expand to a proper single quarter tag.  needs a separate pass
    //      to compute/adjust original dancer bounds so that they are
    //      non-overlapping?  Maybe this could be the same routine that
    //      handles "crashing to right hands" for dancers who end up on
    //      the same spot...
    public static Formation breathe(List<FormationPiece> pieces) {
        // Find 'inner boundaries' of component formations.
        Set<Fraction> xiB = new HashSet<Fraction>();
        Set<Fraction> yiB = new HashSet<Fraction>();
        for (FormationPiece fp : pieces)
            addInner(fp.f.bounds(), xiB, yiB);
        xiB.add(Fraction.ZERO); yiB.add(Fraction.ZERO);
        // sort boundaries.
        List<Fraction> xBound = new ArrayList<Fraction>(xiB);
        List<Fraction> yBound = new ArrayList<Fraction>(yiB);
        Collections.sort(xBound); Collections.sort(yBound);
        
        // initalize 'expansion' list so that there is 0 space between
        // bounds.
        // Note that expansion occurs *between* elements of the original
        // boundary list, so it is 1 element shorter than the boundary list.
        // expansion[0] is the expansion between boundary[0] and boundary[1].
        List<Fraction> xExpand = new ArrayList<Fraction>(xBound.size()-1);
        List<Fraction> yExpand = new ArrayList<Fraction>(yBound.size()-1);
        for (int i=0; i<xBound.size()-1; i++)
            xExpand.add(xBound.get(i+1).subtract(xBound.get(i)).negate());
        for (int i=0; i<yBound.size()-1; i++)
            yExpand.add(yBound.get(i+1).subtract(yBound.get(i)).negate());

        // now expand bounds so that they are just big enough for a single
        // dancer.  Work from center out.
        List<FormationPiece> fpSorted = new ArrayList<FormationPiece>(pieces);
        // sort dancers by absolute x
        Collections.sort(fpSorted, new FormationPieceComparator(true,true)); 
        for (FormationPiece fp : fpSorted) {
            Box b = fp.f.bounds();
            expand(xExpand, xBound, b.ll.x, b.ur.x, Fraction.TWO);
        }
        // sort dancers by absolute y
        Collections.sort(fpSorted, new FormationPieceComparator(false,true)); 
        for (FormationPiece fp : fpSorted) {
            Box b = fp.f.bounds();
            expand(yExpand, yBound, b.ll.y, b.ur.y, Fraction.TWO);
        }
        // assemble meta formation.
        Map<Dancer,Position> nf = new HashMap<Dancer,Position>();
        Box dancerSize = new Box(new Point(Fraction.mONE,Fraction.mONE),
                new Point(Fraction.ONE,Fraction.ONE));
        for (FormationPiece fp : pieces) {
            // find the boundary this piece is going to hang off
            Box b = fp.f.bounds();
            Integer[] xb = findNearest(xBound, b.ll.x, b.ur.x);
            Integer[] yb = findNearest(yBound, b.ll.y, b.ur.y);
            Point dancerLoc = computeCenter(dancerSize,
                    warpPair(xExpand,xBound,xb[0],xb[1]),
                    warpPair(yExpand,yBound,yb[0],yb[1]));
            nf.put(fp.d, new Position(dancerLoc.x,dancerLoc.y,fp.r));
        }
        Formation result = new Formation(nf);
        return result.recenter();
    }
    private static class FormationPieceComparator implements Comparator<FormationPiece> {
        final boolean isX, isAbs; 
        public FormationPieceComparator(boolean isX, boolean isAbs) {
            this.isX = isX; this.isAbs = isAbs;
        }
        public int compare(FormationPiece fp1, FormationPiece fp2) {
            Point p1 = fp1.f.bounds().center(), p2=fp2.f.bounds().center();
            Fraction xy1, xy2;
            if (isX) { xy1=p1.x; xy2=p2.x; }
            else { xy1=p1.y; xy2=p2.y; }
            if (isAbs) { xy1=xy1.abs(); xy2=xy2.abs(); }
            return xy1.compareTo(xy2);
        }
    }
}
