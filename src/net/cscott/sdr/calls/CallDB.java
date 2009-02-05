package net.cscott.sdr.calls;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.cscott.sdr.calls.ast.Apply;
import net.cscott.sdr.calls.lists.BasicList;
import net.cscott.sdr.calls.lists.MainstreamList;
import net.cscott.sdr.calls.transform.CallFileLoader;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.Parser;
import org.antlr.runtime.TokenStream;

/** CallDB holds all the calls and concepts we know about.
 * It is a singleton class; its static constructor loads
 * all the call definitions from files and other classes.
 * @author C. Scott Ananian
 */
public class CallDB {
    public static final CallDB INSTANCE = new CallDB();
    private Map<String, Call> db = new HashMap<String,Call>();
    /** Lookup a call in the database.
     * @throws IllegalArgumentException if the call name is
     * unknown.
     * @doc.test
     *  Basic lookup test:
     *  js> db = CallDB.INSTANCE
     *  net.cscott.sdr.calls.CallDB@1d66e22
     *  js> db.lookup("square thru")
     *  square thru[basic]
     * @doc.test
     *  Check that exceptions are properly thrown for bogus calls:
     *  js> try {
     *    >   CallDB.INSTANCE.lookup("foobar bat")
     *    > } catch (e) {
     *    >   print(e.javaException)
     *    > }
     *  java.lang.IllegalArgumentException: Unknown call: foobar bat
     */
    public Call lookup(String name) {
        if (!db.containsKey(name))
            throw new IllegalArgumentException("Unknown call: "+name);
        return db.get(name);
    }
    public final Collection<Call> allCalls = 
        Collections.unmodifiableCollection(db.values());

    private CallDB() {
        // okay, first load the call definition lists.
        CallFileLoader.load(resource("basic"), db);
        CallFileLoader.load(resource("mainstream"), db);
        CallFileLoader.load(resource("plus"), db);
        CallFileLoader.load(resource("a1"), db);
        CallFileLoader.load(resource("a2"), db);
        CallFileLoader.load(resource("c3b"), db);
        // now load complex calls and concepts.
        loadFromClass(BasicList.class);
        loadFromClass(MainstreamList.class);
    }
    private static URL resource(String name) {
        return CallDB.class.getClassLoader().getResource("net/cscott/sdr/calls/lists/"+name+".calls");
    }
    private void loadFromClass(Class c) {
        // iterate through all fields in class, and add fields of type 'Call'
        for (Field f : c.getFields()) {
            if (Call.class.isAssignableFrom(f.getType()) &&
                    Modifier.isStatic(f.getModifiers())) {
                try {
                    Call call = (Call) f.get(null);
                    db.put(call.getName(), call);
                } catch (IllegalAccessException e) {
                    assert false : e;
                }
            }
        }
    }
    ///////////////////////////////////////////////
    /** Parse a natural-language string of calls.
     *
     * @doc.test
     * js> db = CallDB.INSTANCE
     * net.cscott.sdr.calls.CallDB@1d66e22
     * js> db.parse(Program.BASIC, "double pass thru")
     * (Apply double pass thru)
     * js> db.parse(Program.BASIC, "square thru three and a half")
     * (Apply square thru (Apply 3 1/2))
     */
    public Apply parse(Program program, String s) {
        program = Program.C4; // xxx: force C4 for now.
        String pkgName = "net.cscott.sdr.calls.lists.";
        String baseName = program.toTitleCase()+"Grammar";
        String parserName = baseName+"Parser";
        String lexerName = baseName+"Lexer";

        try {
            Lexer lexer = (Lexer) Class.forName(pkgName+lexerName)
                .getConstructor(CharStream.class).newInstance
                (new ANTLRStringStream(s));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.discardOffChannelTokens(true);
            Parser parser = (Parser) Class.forName(pkgName+parserName)
                .getConstructor(TokenStream.class).newInstance(tokens);
            Method m = parser.getClass().getMethod("anything");
            return (Apply) m.invoke(parser);
        } catch (Exception e) {
            throw new BadCallException("Parsing error: "+e);
        }
    }
}
