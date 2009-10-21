// $Id: ClVariable.java,v 1.1 2008/08/12 22:32:42 larrymelia Exp $
//
// Cassowary Incremental Constraint Solver
// Original Smalltalk Implementation by Alan Borning
// This Java Implementation by Greg J. Badros, <gjb@cs.washington.edu>
// http://www.cs.washington.edu/homes/gjb
// (C) 1998, 1999 Greg J. Badros and Alan Borning
// See ../LICENSE for legal details regarding this software
//
// ClVariable

package EDU.Washington.grad.gjb.cassowary;

import java.util.*;

import net.cscott.sdr.util.Fraction;

public class ClVariable extends ClAbstractVariable {

    public ClVariable(String name, Fraction value) {
        super(name);
        _value = value;
        if (_ourVarMap != null) {
            _ourVarMap.put(name, this);
        }
    }

    public ClVariable(String name) {
        super(name);
        _value = Fraction.ZERO;
        if (_ourVarMap != null) {
            _ourVarMap.put(name, this);
        }
    }

    public ClVariable(Fraction value) {
        _value = value;
    }

    public ClVariable() {
        _value = Fraction.ZERO;
    }

    public ClVariable(long number, String prefix, Fraction value) {
        super(number, prefix);
        _value = value;
    }

    public ClVariable(long number, String prefix) {
        super(number, prefix);
        _value = Fraction.ZERO;
    }

    public boolean isDummy() {
        return false;
    }

    public boolean isExternal() {
        return true;
    }

    public boolean isPivotable() {
        return false;
    }

    public boolean isRestricted() {
        return false;
    }

    public String toString() {
        return "[" + name() + ":" + _value + "]";
    }

    // change the value held -- should *not* use this if the variable is
    // in a solver -- instead use addEditVar() and suggestValue() interface
    public final Fraction value() {
        return _value;
    }

    public final void set_value(Fraction value) {
        _value = value;
    }

    // permit overriding in subclasses in case something needs to be
    // done when the value is changed by the solver
    // may be called when the value hasn't actually changed -- just
    // means the solver is setting the external variable
    public void change_value(Fraction value) {
        _value = value;
    }

    public void setAttachedObject(Object o) {
        _attachedObject = o;
    }

    public Object getAttachedObject() {
        return _attachedObject;
    }

    public static void setVarMap(Hashtable<String, ClVariable> map) {
        _ourVarMap = map;
    }

    public static Hashtable<String, ClVariable> getVarMap() {
        return _ourVarMap;
    }

    private static Hashtable<String, ClVariable> _ourVarMap;

    private Fraction _value;

    private Object _attachedObject;

}