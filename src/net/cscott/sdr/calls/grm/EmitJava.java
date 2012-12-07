package net.cscott.sdr.calls.grm;

import static net.cscott.sdr.util.StringEscapeUtils.escapeJava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.cscott.jutil.MultiMap;
import net.cscott.sdr.DevSettings;
import net.cscott.sdr.calls.Program;
import net.cscott.sdr.calls.grm.Grm.Alt;
import net.cscott.sdr.calls.grm.Grm.Concat;
import net.cscott.sdr.calls.grm.Grm.Mult;
import net.cscott.sdr.calls.grm.Grm.Nonterminal;
import net.cscott.sdr.calls.grm.Grm.Terminal;
import net.cscott.sdr.util.Tools;

/** Emit Java representation of post-processed natural language grammar
 *  for calls. */
public class EmitJava extends AbstractEmit {
    public final static EmitJava INSTANCE = new EmitJava();
    Map<Program,Map<String,Grm>> grmTable =
        new TreeMap<Program,Map<String,Grm>>();
    public void collect(Program program, List<RuleAndAction> l) {
        // collect all the rules with the same LHS
        MultiMap<String,RuleAndAction> mm = collectLHS(l);
        // join all the alternatives
        Map<String,Grm> m = new LinkedHashMap<String,Grm>();
        for (String lhs : sorted(mm.keySet())) {
            List<Grm> alts = new ArrayList<Grm>();
            for (RuleAndAction ra : mm.getValues(lhs))
                alts.add(ra.rule.rhs);
            m.put(lhs, new Grm.Alt(alts));
        }
        // add rules for <anyone>, etc.
        m.put("parenthesized_anything", new Grm.Concat
                (Tools.<Grm>l(new Grm.Terminal("("),
                              new Grm.Nonterminal("anything", -1),
                              new Grm.Terminal(")"))));
        m.put("people",
              Grm.parse("<genders> | <heads_or_sides> | <all> | <none>"));
        m.put("heads_or_sides", Grm.parse("heads | sides"));
        m.put("two_select",
              Grm.parse("(head|side) <genders> | (center|end) <genders> | very centers | (center|outside) two"));
        m.put("six_select", Grm.parse("(center|outside) six"));
        m.put("genders", Grm.parse("<boys> | <girls>"));
        m.put("boys", Grm.parse("boys | men"));
        m.put("girls", Grm.parse("girls | ladies"));
        m.put("all", Grm.parse("all | every (one|body) | everybody | everyone"));
        m.put("none", Grm.parse("none | no (one|body) | nobody"));
        m.put("wave_select", Grm.parse("centers | ends"));
        m.put("anyone", Grm.parse("<people> | <wave_select> | <two_select> | <six_select>"));
        m.put("number", Grm.parse("<digit> and <fraction>|<digit>|<fraction>|<NUMBER>"));
        m.put("digit", Grm.parse("one | two | <digit_greater_than_two>"));
        m.put("digit_greater_than_two", Grm.parse("three | four | five | six | seven | eight | nine"));
        m.put("fraction", Grm.parse("(a|one) (half|third|quarter) | two (thirds|quarters) | three quarters"));
        m.put("times", Grm.parse("once and <fraction> | twice (and <fraction>)? | <digit_greater_than_two> (and <fraction>)? times | <NUMBER> times"));
        m.put("start", Grm.parse("<anything> <EOF>"));
        EmitJava.INSTANCE.grmTable.put(program, m);
    }
    final String NL = System.getProperty("line.separator");
    final String INDENT = "        ";
    final StringBuilder sb = new StringBuilder();
    Map<Grm,Integer> numbering = new HashMap<Grm,Integer>();
    final List<String> buildLines = new ArrayList<String>();
    public int num(Grm g) {
        if (!numbering.containsKey(g)) {
            String repr = g.accept(numberVisitor);
            StringBuilder sb = new StringBuilder();
            sb.append(INDENT);
            sb.append("l.add(");
            sb.append(repr);
            sb.append("); // "+numbering.size());
            sb.append(NL);
            buildLines.add(sb.toString());
            numbering.put(g, numbering.size());
        }
        return numbering.get(g);
    }
    final GrmVisitor<String> numberVisitor = new GrmVisitor<String>() {
        @Override
        public String visit(Alt alt) {
            StringBuilder sb = new StringBuilder("new Grm.Alt(Tools.l(");
            for (Iterator<Grm> it=alt.alternates.iterator(); it.hasNext(); ) {
                sb.append("l.get(");
                sb.append(""+num(it.next()));
                sb.append(')');
                if (it.hasNext()) sb.append(',');
            }
            sb.append("))");
            return sb.toString();
        }
        @Override
        public String visit(Concat concat) {
            StringBuilder sb = new StringBuilder("new Grm.Concat(Tools.l(");
            for (Iterator<Grm> it=concat.sequence.iterator(); it.hasNext(); ) {
                sb.append("l.get(");
                sb.append(""+num(it.next()));
                sb.append(')');
                if (it.hasNext()) sb.append(',');
            }
            sb.append("))");
            return sb.toString();
        }
        @Override
        public String visit(Mult mult) {
            StringBuilder sb=new StringBuilder("new Grm.Mult(");
            sb.append("l.get(");
            sb.append(""+num(mult.operand));
            sb.append("),Grm.Mult.Type.");
            sb.append(mult.type.name());
            sb.append(")");
            return sb.toString();
        }
        @Override
        public String visit(Nonterminal nonterm) { return nonterm.repr(); }
        @Override
        public String visit(Terminal term) { return term.repr(); }
    };
    public String emit() {
        // emit all the grammars
	Collection<Program> pp = grmTable.keySet();
	if (DevSettings.ONLY_C4_GRAMMAR)
	    pp = Arrays.asList(Program.values());

        for (Program p: pp) {
            sb.append("    /** Map from nonterminal names to grammar ");
            sb.append("productions for the ");
            sb.append(p.name());
            sb.append(NL);
            sb.append("     *  dance program. */");
            sb.append(NL);
            sb.append("    public static final Map<String,Grm> ");
            sb.append(p.name());
            sb.append(";");
            sb.append(NL);
        }
        sb.append("    static {"+NL);
        sb.append(INDENT);
        sb.append("List<Grm> l = _build();");
        sb.append(NL);
        for (Program p: grmTable.keySet()) {
            Map<String,Grm> m = grmTable.get(p);
            sb.append(INDENT);
            sb.append("Map<String,Grm> _");
            sb.append(p.name());
            sb.append(" = new HashMap<String,Grm>();");
            sb.append(NL);
            for (String nonterm : m.keySet()) {
                Grm g = SimplifyGrm.simplify(m.get(nonterm)).intern();
                int n = num(g);
                sb.append(INDENT);
                sb.append("_");
                sb.append(p.name());
                sb.append(".put(\"");
                sb.append(escapeJava(nonterm));
                sb.append("\",l.get("+n+"));"+NL);
            }
            sb.append(INDENT);
            sb.append(p.name());
            sb.append(" = Collections.unmodifiableMap(_");
            sb.append(p.name());
            sb.append(");");
            sb.append(NL);
        }
	if (DevSettings.ONLY_C4_GRAMMAR) {
	    for (Program p : Program.values()) {
		if (p==Program.C4) continue;
		sb.append(INDENT);
		sb.append(p.name());
		sb.append(" = _C4;");
		sb.append(NL);
	    }
	}
        sb.append("    }"+NL);

        // create the _build function, broken up into subfunctions to avoid
        // exceeding the maximum method size limit.
        sb.append("    private static List<Grm> _build() {"+NL);
        sb.append(INDENT);
        sb.append("List<Grm> l = new ArrayList<Grm>("+numbering.size()+");");
        sb.append(NL);
        sb.append(INDENT);
        sb.append("/* Break construction into multiple functions to avoid");
        sb.append(NL);
        sb.append(INDENT);
        sb.append(" * exceeding maximum method bytecode size limit. */");
        sb.append(NL);
        for (int i=0; i*500 < buildLines.size(); i++) {
            sb.append(INDENT);
            sb.append("_build"+i+"(l);");
            sb.append(NL);
        }
        sb.append(INDENT);
        sb.append("return l;");
        sb.append(NL);
        sb.append("    }"+NL);
        // _buildN subfunctions
        for (int i=0; i*500 < buildLines.size(); i++) {
            sb.append("    private static void _build"+i+"(List<Grm> l) {"+NL);
            int start = i*500;
            int end = Math.min(start+500, buildLines.size());
            for (String line: buildLines.subList(start, end)) {
                sb.append(line);
            }
            sb.append("    }"+NL);
        }

        // substitute the rules & the classname into the skeleton
        String result = subst("java.skel", sb.toString(), "All");
        // done.
        return result;
    }
}
