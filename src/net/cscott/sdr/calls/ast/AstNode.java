package net.cscott.sdr.calls.ast;

import org.junit.runner.RunWith;

import net.cscott.jdoctest.JDoctestRunner;
import net.cscott.sdr.calls.transform.TransformVisitor;
import net.cscott.sdr.calls.transform.ValueVisitor;

/**
 * <code>AstNode</code> is the abstract base class for abstract syntax
 * tree nodes representing parsed call definitions and components.
 * @author C. Scott Ananian
 * @version $Id: AstNode.java,v 1.1 2006-10-17 16:29:05 cananian Exp $
 */
@RunWith(value=JDoctestRunner.class)
public abstract class AstNode {
    private final String name;
    public final int type;
    protected AstNode(int type) {
        this(type, null);
    }
    protected AstNode(int type, String name) {
        if (name==null)
            name = getClass().getName().replaceAll("[^.]+[.]","");
        this.type = type;
        this.name = name.intern();
    }
    /**
     * Visitor pattern implementation for transformations.
     * Each {@link AstNode} accepts a {@link TransformVisitor} and a
     * value, and returns an instance of its own type (ie,
     * <code>Seq.accept(tv, "A")</code> returns a <code>Seq</code>).
     * The 't' parameter is a closure.
     * @doc.test Create a no-op transform visitor and apply it:
     *  js> s = AstNode.valueOf("(Seq (Prim -1, 1, none, 1 1/2) (Prim 1, 1, none, 1 1/2))")
     *  (Seq (Prim -1, 1, none, 1 1/2) (Prim 1, 1, none, 1 1/2))
     *  js> s.accept(new net.cscott.sdr.calls.transform.TransformVisitor({}), "A")
     *  (Seq (Prim -1, 1, none, 1 1/2) (Prim 1, 1, none, 1 1/2))
     */
    public abstract <T> AstNode accept(TransformVisitor<T> v, T t);
    /**
     * Visitor pattern implementation for computations.
     * Each {@link AstNode} accepts a {@link ValueVisitor} and a closure,
     * and returns an object of the appropriate result type.
     */
    public abstract <RESULT,CLOSURE>
    RESULT accept (ValueVisitor<RESULT,CLOSURE> v, CLOSURE cl);

    /** Output AST tree in lisp-like notation. */
    @Override
    public String toString() { return ("("+name+" "+argsToString()+")").replaceAll(" \\)",")"); }
    protected String argsToString() { return ""; }
    /**
     * Parse AST tree from string; inverse of {@link #toString}.
     * @throws IllegalArgumentException if the given string value doesn't parse
     */
    public static AstNode valueOf(String s) throws IllegalArgumentException {
	AstNode result = null;
	try {
	    result = new AstParser(s).start();
	} catch (org.antlr.runtime.RecognitionException e) {
	    throw new IllegalArgumentException("Bad AST: "+e);
	}
	if (result==null)
	    throw new IllegalArgumentException("Bad AST: "+s);
	return result;
    }
}
