package net.cscott.sdr.calls.transform;

import java.util.*;

import net.cscott.sdr.calls.*;
import net.cscott.sdr.calls.TaggedFormation.Tag;
import net.cscott.sdr.calls.ast.*;

/**
 * The {@link Evaluate} class does formation evaluation to eliminate
 * {@link Opt} elements in the call tree and identify the dancers in
 * {@link Par}s.  It inserts {@link Warp} elements as needed.
 * The result is a 'simplified tree'.
 * @author C. Scott Ananian
 * @version $Id: Evaluate.java,v 1.6 2009-02-06 06:27:55 cananian Exp $
 */
// XXX: should this be a TranformationVisitor<TaggedFormation> ?
public class Evaluate extends TransformVisitor<Formation> {
    /** Static dance state (like program, etc). */
    private final DanceProgram ds;

    private Evaluate(DanceProgram ds) {
        this.ds = ds;
    }
    /**
     * Evaluate "one part" of a call.
     * @param ds current static {@link DanceProgram} (program, etc)
     * @param f  current {@link Formation} (dynamic state)
     * @param c  the call component to elaborate
     * @param doFully if false, performs just one step of elaboration;
     *  otherwise, evaluate all the way down to a simplified tree.
     * @return the elaborated call component
     */
    public static Comp evaluate(DanceProgram ds, Formation f, Comp c, boolean doFully) {
        return c.accept(new Evaluate(ds), f);
    }
    private boolean doFully=false; // XXX REMOVE ME
    
    /** Expand any 'Apply' node we come to. */
    @Override
    public SeqCall visit(Apply a, Formation f) {
        // expand this call.
        Comp c = a.expand();
        if (doFully) // go off and elaborate the expansion.
            c = c.accept(this, f);
        return new Part(true, c);
    }
    /* Evaluate predicates, and either remove them or
     * throw a BadCallException. */
    @Override
    public Comp visit(If iff, Formation f) {
        // evaluate the predicate
        Predicate p = iff.condition.getPredicate();
        if (!p.evaluate(ds, f, iff.condition)) 
            throw new BadCallException("condition failed");
        Comp c = iff.child;
        if (doFully) c = c.accept(this, f);
        return c;
    }
    /** Go through list of possible formations, and pick
     * the first one which works.  Generate SELECT and
     * WARP nodes to pull out the subformations. */
    @Override
    public Comp visit(Opt opt, Formation f) {
        // first, reduce to one formation per OptCall
        List<ParCall> l = new ArrayList<ParCall>(opt.children.size());
        for (OptCall oc : opt.children) {
            for (Selector s : oc.selectors) {
                try {
                    l.addAll(doOptCall(oc.build
                            (Collections.singletonList(s), oc.child), f));
                    break; // only use first matching formation.
                } catch (BadCallException ex) {
                    // ignore, just don't add a parcall to the par.
                }
            }
        }
        if (l.isEmpty())
            throw new BadCallException("No matching formation");
        // optimize: (Par (Select [ALL] x)) -> x
        if (l.size()==1)
            return l.get(0).child;
        return new Par(l);
    }
    // (FROM [MINIWAVE] x) ->
    //   (SELECT (d1,d2,d3) (WARP w (FROM MINIWAVE x)))
    // with the FROM left out iff doFully.
    // XXX add a 'do nothing' call for the unselected dancers?
    public List<ParCall> doOptCall(OptCall oc, Formation f) {
        // this is the tricky part: perform the formation match
        assert oc.selectors.size()==1;
        Selector s = oc.selectors.get(0);
        System.err.println("TRYING TO MATCH "+s+" TO "+f);
        FormationMatch fm = s.match(f);
        System.err.println("NUMBER OF MATCHES: "+fm.matches.size());
        assert !fm.matches.isEmpty();
        int dancers = 0;
        List<ParCall> l = new ArrayList<ParCall>(fm.matches.size());
        for (Dancer md : fm.meta.dancers()) {
            TaggedFormation tf = fm.matches.get(md);
            Comp c = oc.child.accept(this, tf);
            if (!doFully) {
                c = new Opt(oc.build(oc.selectors, c));
            } else { // xxx broken
                assert false : "unimplemented?";
                c = null;
            }
            l.add(new ParCall(selectedTags(tf), c));
            dancers += tf.dancers().size();
        }
        // xxx are there leftover dancers?  we'll be safe...
        // add a call for them to do nothing.
        if (dancers < f.dancers().size())
            l.add(new ParCall(Collections.singleton(Tag.ALL),
                    new Seq(Prim.STAND_STILL)));
        return l;
    }
        
    @Override
    public OptCall visit(OptCall oc, Formation f) {
        assert false : "Can't elaborate an OptCall (need to use the parent)";
        return oc.build(oc.selectors, oc.child.accept(this, f));
    }
    /** For each select clause, rephrase using 'primitive' tags.
     * Ensure that every selected dancer matches one of the clauses. */
    @Override
    public Comp visit(Par p, Formation _f) {
        // XXX: Should I be dynamically applying tags here? (probably!)
        TaggedFormation tf = (TaggedFormation) _f;
        Set<Dancer> sel = new HashSet<Dancer>(tf.selectedDancers());
        List<ParCall> l = new ArrayList<ParCall>(p.children.size());
        for (ParCall pc : p.children) {
            try {
                l.add(pc.accept(this, tf));
            } catch (BadCallException ex) {
                // ignore the exception; just don't add it to the list
                System.err.println("BAD CALL IN PAR ELABORATION: "+ex);
            }
            // remove matched dancers from the formation.
            sel.removeAll(selected(pc,tf));
            tf = tf.select(sel);
        }
        if (l.isEmpty()) // all options have been exhausted
            throw new BadCallException("No dancers can be selected");
        if (!sel.isEmpty()) // some dancers not matched
            throw new BadCallException("Unmatched dancers: "+sel);
        // optimize: (Par (Select [ALL] x)) -> x
        if (l.size()==1)
            return l.get(0).child;
        return p.build(l);
    }
    /** Select the given dancers in the formation, and evaluate the child. */
    @Override
    public ParCall visit(ParCall pc, Formation f) {
        TaggedFormation tf = (TaggedFormation) f;
        // identify the dancers selected by this.
        Set<Dancer> nSel = selected(pc, tf);
        if (nSel.isEmpty()) throw new BadCallException("no dancers selected");
        // replace the tags with DANCER_x tags, corresponding to the
        // dancers in the given formation with that tag.
        Set<Tag> nTags;
        if (doFully) { // only if doFully
            nTags = EnumSet.noneOf(Tag.class);
            for (Dancer d : nSel)
                nTags.add(d.primitiveTag());
        } else nTags = pc.tags; // otherwise use old tags
        // create a new TaggedFormation with just these dancers selected.
        TaggedFormation nf=tf.select(nSel);
        // and evaluate the child in this formation.
        Comp c = pc.child.accept(this, nf);
        // build a primitive sel if doFully; otherwise just rebuild the select
        return pc.build(nTags, c);
    }
    /** Return the set of dancers named by this ParCall. */
    private Set<Dancer> selected(ParCall pc, TaggedFormation tf) {
        Set<Dancer> result = new HashSet<Dancer>(8);
        for (Tag tag : pc.tags)
            for (Dancer d : tf.tagged(tag))
                if (tf.isSelected(d))
                    result.add(d);
        return result;
    }
    /** Return a set of primitive tags which name all the selected
     * dancers in this formation.
     */
    private Set<Tag> selectedTags(Formation f) {
        Set<Tag> tags = EnumSet.noneOf(Tag.class);
        for (Dancer d : f.selectedDancers()) {
            assert d.primitiveTag() != null : "formation consists of phantoms";
            tags.add(d.primitiveTag());
        }
        return tags;
    }

    /** A little optimization: for pairs of Warps, compose
     * them into one node. */
    public Comp visit(Warped w, Formation f) {
        // little bit of optimization here.
        if (w.child.type == TokenTypes.WARP) {
            Warped ww = (Warped) w.child;
            w = w.build(Warp.compose(w.warp, ww.warp), ww.child);
        }
        return super.visit(w, f);
    }

    // XXX: need to handle Seq/SeqCall, generating new
    // formations as the dancers move.
}