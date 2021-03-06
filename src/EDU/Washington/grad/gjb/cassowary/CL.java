// $Id: CL.java,v 1.2 2008/09/05 05:02:11 larrymelia Exp $
//
// Cassowary Incremental Constraint Solver
// Original Smalltalk Implementation by Alan Borning
// This Java Implementation by Greg J. Badros, <gjb@cs.washington.edu>
// http://www.cs.washington.edu/homes/gjb
// (C) 1998, 1999 Greg J. Badros and Alan Borning
// See ../LICENSE for legal details regarding this software
//
// CL.java
// The enumerations from ClLinearInequality,
// and `global' functions that we want easy to access

package EDU.Washington.grad.gjb.cassowary;

import net.cscott.sdr.util.Fraction;

public class CL {
    protected final static boolean fDebugOn = false;
    protected final static boolean fTraceOn = false;
    protected final static boolean fGC = false;

    protected static void debugprint(String s) {
        System.err.println(s);
    }

    protected static void traceprint(String s) {
        System.err.println(s);
    }

    protected static void fnenterprint(String s) {
        System.err.println("* " + s);
    }

    protected static void fnexitprint(String s) {
        System.err.println("- " + s);
    }

    protected void assertion(boolean f, String description)
            throws ExCLInternalError {
        if (!f) {
            throw new ExCLInternalError("Assertion failed:" + description);
        }
    }

    protected void assertion(boolean f) throws ExCLInternalError {
        if (!f) {
            throw new ExCLInternalError("Assertion failed");
        }
    }

    // public static final byte GEQ = 1;
    // public static final byte LEQ = 2;
    public enum Op {
        GEQ, // >=
        LEQ  // <=
    }

    public static ClLinearExpression Plus(ClLinearExpression e1,
            ClLinearExpression e2) {
        return e1.plus(e2);
    }

    public static ClLinearExpression Plus(ClLinearExpression e1, Fraction e2) {
        return e1.plus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Plus(Fraction e1, ClLinearExpression e2) {
        return (new ClLinearExpression(e1)).plus(e2);
    }

    public static ClLinearExpression Plus(ClVariable e1, ClLinearExpression e2) {
        return (new ClLinearExpression(e1)).plus(e2);
    }

    public static ClLinearExpression Plus(ClLinearExpression e1, ClVariable e2) {
        return e1.plus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Plus(ClVariable e1, ClVariable e2) {
        return new ClLinearExpression(e1).plus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Plus(ClVariable e1, Fraction e2) {
        return (new ClLinearExpression(e1)).plus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Plus(Fraction e1, ClVariable e2) {
        return (new ClLinearExpression(e1)).plus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Minus(ClVariable e1, ClVariable e2) {
        return new ClLinearExpression(e1).minus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Minus(ClVariable e1, Fraction e2) {
        return new ClLinearExpression(e1).minus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Minus(ClLinearExpression e1,
            ClLinearExpression e2) {
        return e1.minus(e2);
    }

    public static ClLinearExpression Minus(Fraction e1, ClLinearExpression e2) {
        return (new ClLinearExpression(e1)).minus(e2);
    }

    public static ClLinearExpression Minus(Fraction e1, ClVariable e2) {
        return (new ClLinearExpression(e1)).minus(e2);
    }

    public static ClLinearExpression Minus(ClLinearExpression e1, Fraction e2) {
        return e1.minus(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Times(ClLinearExpression e1,
            ClLinearExpression e2) throws ExCLNonlinearExpression {
        return e1.times(e2);
    }

    public static ClLinearExpression Times(ClLinearExpression e1, ClVariable e2)
            throws ExCLNonlinearExpression {
        return e1.times(new ClLinearExpression(e2));
    }

    public static ClLinearExpression Times(ClVariable e1, ClLinearExpression e2)
            throws ExCLNonlinearExpression {
        return (new ClLinearExpression(e1)).times(e2);
    }

    public static ClLinearExpression Times(ClLinearExpression e1, Fraction e2) {
        return e1.times(e2);
    }

    public static ClLinearExpression Times(Fraction e1, ClLinearExpression e2) {
        return e2.times(e1);
    }

    public static ClLinearExpression Times(Fraction n, ClVariable clv) {
        return (new ClLinearExpression(clv, n));
    }

    public static ClLinearExpression Times(ClVariable clv, Fraction n) {
        return (new ClLinearExpression(clv, n));
    }

    public static ClLinearExpression Divide(ClLinearExpression e1,
            ClLinearExpression e2) throws ExCLNonlinearExpression {
        return e1.divide(e2);
    }
}
