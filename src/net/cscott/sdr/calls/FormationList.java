package net.cscott.sdr.calls;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.cscott.jdoctest.JDoctestRunner;

import org.junit.runner.RunWith;

/**
 * A list of common formations, specified with phantoms.
 * The actual formation definitions are in the package-scope
 * {@link FormationListSlow}, which is post-processed into
 * {@link FormationListFast}.  This class is a public wrapper for those
 * definitions and contains doc tests on the formations.
 *
 * @doc.test Almost all standard formations are maximally "breathed in".
 *  Diamonds are a special case.  There should be "expanded" diamonds and
 *  "compressed" diamonds.  (Also the case for quarter tags?)
 *  js> FormationList = FormationListJS.initJS(this); undefined;
 *  js> for (f in Iterator(FormationList.all)) {
 *    >   if (!Breather.breathe(f).equals(f)) {
 *    >     print("Unbreathed formation: "+f.getName());
 *    >   }
 *    > }
 *  Unbreathed formation: RH DIAMOND
 *  Unbreathed formation: RH FACING DIAMOND
 *  Unbreathed formation: LH DIAMOND
 *  Unbreathed formation: LH FACING DIAMOND
 *  Unbreathed formation: RH TWIN DIAMONDS
 *  Unbreathed formation: LH TWIN DIAMONDS
 *  Unbreathed formation: RH POINT-TO-POINT DIAMONDS
 *  Unbreathed formation: RH POINT-TO-POINT FACING DIAMONDS
 *  Unbreathed formation: LH POINT-TO-POINT DIAMONDS
 *  Unbreathed formation: LH POINT-TO-POINT FACING DIAMONDS
 *  Unbreathed formation: RH TWIN FACING DIAMONDS
 *  Unbreathed formation: LH TWIN FACING DIAMONDS
 * @doc.test Canonical formations should be centered.
 *  js> FormationList = FormationListJS.initJS(this); undefined;
 *  js> for (f in Iterator(FormationList.all)) {
 *    >   if (!f.isCentered()) {
 *    >     print("Uncentered formation: "+f.getName());
 *    >   }
 *    > } ; undefined
 *  js> // note no output from the above.
 * @doc.test Canonical formations should be oriented so that "most"
 *  dancers are facing north or south.  This seems to match standard
 *  diagrams best.
 *  js> FormationList = FormationListJS.initJS(this); undefined;
 *  js> ns = Rotation.fromAbsoluteString("|");
 *  0 mod 1/2
 *  js> for (f in Iterator(FormationList.all)) {
 *    >   facing = [f.location(d).facing for (d in Iterator(f.dancers()))]
 *    >   l=[(ns.includes(dir) || dir.includes(ns)) for each (dir in facing)]
 *    >   if ([b for each (b in l) if (b)].length <
 *    >       [b for each (b in l) if (!b)].length) {
 *    >     print("Unexpected orientation: "+f.getName());
 *    >   }
 *    > } ; undefined
 *  js> // note no output from the above.
 * @doc.test Formations in the formation list should be named to match their
 *  field name, except that underscores become spaces.
 *  js> flc = java.lang.Class.forName('net.cscott.sdr.calls.FormationList')
 *  class net.cscott.sdr.calls.FormationList
 *  js> flds = [f for each (f in flc.getFields()) if
 *    >         (java.lang.reflect.Modifier.isPublic(f.getModifiers()) &&
 *    >          java.lang.reflect.Modifier.isStatic(f.getModifiers()) &&
 *    >          !f.getName().equals("all"))]; undefined
 *  js> FormationListJS = FormationListJS.initJS(this); undefined;
 *  js> flds.every(function(f) {
 *    >    name = f.getName();
 *    >    return (FormationListJS[name].getName()
 *    >            .replace(' ','_').replace('-','_')
 *    >            .replace("1/4","QUARTER")
 *    >            .replace("3/4","THREE_QUARTER")
 *    >            .replace("1x4", "_1x4")
 *    >            .replace("2x2", "_2x2")
 *    >            .equals(name));
 *    > })
 *  true
 * @doc.test The "slow" and "fast" versions of the formations should be
 *  identical (modulo the exact identity of the phantom dancers)
 *  js> FormationListSlow = FormationListJS.initJS(this, FormationListSlow)
 *  [object FormationListJS]
 *  js> FormationListFast = FormationListJS.initJS(this, FormationList)
 *  [object FormationListJS]
 *  js> len = FormationListSlow.all.size(); undefined
 *  js> [f for (f in FormationListSlow)].length == len
 *  true
 *  js> FormationListFast.all.size() == len
 *  true
 *  js> [f for (f in FormationListFast)].length == len
 *  true
 *  js> function compare(a, b) {
 *    >    if (!a.getName().equals(b.getName())) return false;
 *    >    d1=a.sortedDancers(); d2=b.sortedDancers();
 *    >    if (d1.size() != d2.size()) return false;
 *    >    // make all phantoms equivalent
 *    >    m=new java.util.HashMap();
 *    >    for (let i=0; i<d1.size(); i++) {
 *    >      if (!(d1.get(i) instanceof PhantomDancer)) return false;
 *    >      if (!(d2.get(i) instanceof PhantomDancer)) return false;
 *    >      m.put(d1.get(i), d2.get(i));
 *    >    }
 *    >    aa=a.map(m);
 *    >    return aa.equals(b);
 *    > }
 *  js> matches=0
 *  0
 *  js> for (f in FormationListFast) {
 *    >    if (compare(FormationListSlow[f], FormationListFast[f]))
 *    >      matches++;
 *    > }; matches == len;
 *  true
 */
// can use SelectorList to associate phantoms with real dancers.
@RunWith(value=JDoctestRunner.class)
public abstract class FormationList extends FormationListFast {
    // no implementation: all the interesting stuff is in FormationListSlow.
    
    /** Show all the defined formations. */
    public static void main(String[] args) throws Exception {
        for (Field f : FormationList.class.getFields()) {
            if (Modifier.isPublic(f.getModifiers()) &&
                Modifier.isStatic(f.getModifiers()) &&
                f.getName().toUpperCase().equals(f.getName())) {
                NamedTaggedFormation ff = (NamedTaggedFormation) f.get(null);
                System.out.println("FormationList."+f.getName());
                System.out.println(ff.toStringDiagram());
                System.out.println(ff.toString());
                System.out.println();
            }
        }
    }
}
