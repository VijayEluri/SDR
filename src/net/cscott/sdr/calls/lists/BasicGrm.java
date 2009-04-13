package net.cscott.sdr.calls.lists;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.cscott.sdr.calls.grm.Grm;
import net.cscott.sdr.calls.grm.GrmDB;
import net.cscott.sdr.util.Tools;

/** Post-processed grammar for Basic. */
public class BasicGrm extends GrmDB {
    /** Constructor. */
    public BasicGrm() { }
    /** Map from nonterminal name to Grm representing its production. */
    public static final Map<String,Grm> NONTERMINALS;
    static {
	Map<String,Grm> m = new HashMap<String,Grm>();
        m.put("anything",new Grm.Nonterminal("anything_0",null,0));
        m.put("anything_0",new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("do"),new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("half"),new Grm.Terminal("of"),new Grm.Mult(new Grm.Terminal("a"),Grm.Mult.Type.QUESTION),new Grm.Nonterminal("anything_1","anything",0),new Grm.Mult(new Grm.Nonterminal("anything_0_suffix",null,-1),Grm.Mult.Type.STAR))),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("fraction",0),new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("of"),new Grm.Mult(new Grm.Terminal("a"),Grm.Mult.Type.QUESTION))),Grm.Mult.Type.QUESTION),new Grm.Nonterminal("anything_1","anything",1),new Grm.Mult(new Grm.Nonterminal("anything_0_suffix",null,-1),Grm.Mult.Type.STAR))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("anything_1",null,0),new Grm.Mult(new Grm.Nonterminal("anything_0_suffix",null,-1),Grm.Mult.Type.STAR))))));
        m.put("anything_0_suffix",new Grm.Nonterminal("cardinal",0));
        m.put("anything_1",new Grm.Alt(Tools.<Grm>l(new Grm.Nonterminal("anything_2",null,0),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("anyone",0),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("run"),new Grm.Terminal("fold"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("cross"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("run"),new Grm.Terminal("fold"))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("walk"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("others"),new Grm.Nonterminal("anyone",-1))),new Grm.Terminal("dodge"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("u"),new Grm.Terminal("turn"),new Grm.Terminal("back"))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("touch"),new Grm.Nonterminal("fraction",0))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("two"),new Grm.Nonterminal("genders",0),new Grm.Terminal("chain"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("courtesy"),new Grm.Terminal("turn"),new Grm.Nonterminal("fraction",0))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("circle"),new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("left"),new Grm.Nonterminal("fraction",0))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("right"),new Grm.Nonterminal("fraction",0))))))))));
        m.put("anything_2",new Grm.Alt(Tools.<Grm>l(new Grm.Nonterminal("anything_3",null,0),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("left"),new Grm.Nonterminal("leftable_anything",0))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("reverse"),new Grm.Nonterminal("reversable_anything",0))))));
        m.put("anything_3",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("zoom"),new Grm.Terminal("recycle"),new Grm.Terminal("nothing"),new Grm.Terminal("hinge"),new Grm.Terminal("balance"),new Grm.Terminal("trade"),new Grm.Concat(Tools.<Grm>l(new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("all"),new Grm.Terminal("eight"))),Grm.Mult.Type.QUESTION),new Grm.Terminal("circulate"))),new Grm.Nonterminal("leftable_anything",null,0),new Grm.Nonterminal("reversable_anything",null,0),new Grm.Nonterminal("parenthesized_anything",null,0),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("bend"),new Grm.Terminal("the"),new Grm.Terminal("line"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("pass"),new Grm.Terminal("the"),new Grm.Terminal("ocean"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("walk"),new Grm.Terminal("and"),new Grm.Terminal("dodge"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("lead"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("right"),new Grm.Terminal("left"))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("face"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("right"),new Grm.Terminal("left"))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("forward"),new Grm.Terminal("and"),new Grm.Terminal("back"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("slide"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("veer"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("left"),new Grm.Terminal("right"))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("right"),new Grm.Terminal("and"),new Grm.Terminal("left"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("column"),new Grm.Terminal("circulate"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("star"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("box"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("circulate"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("the"),new Grm.Terminal("gnat"))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("chain"),new Grm.Terminal("down"),new Grm.Terminal("the"),new Grm.Terminal("line"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("step"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("thru"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("to"),new Grm.Terminal("a"),new Grm.Terminal("wave"))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("couples"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("trade"),new Grm.Terminal("circulate"),new Grm.Terminal("hinge"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("left"),new Grm.Terminal("hinge"))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("sides"),new Grm.Terminal("start"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("dixie"),new Grm.Terminal("style"),new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("to"),new Grm.Terminal("a"),new Grm.Terminal("wave"))),Grm.Mult.Type.QUESTION))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("heads"),new Grm.Terminal("start"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("double"),new Grm.Terminal("pass"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("california"),new Grm.Terminal("twirl"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("left"),new Grm.Terminal("and"),new Grm.Terminal("right"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("u"),new Grm.Terminal("turn"),new Grm.Terminal("back"))))));
        m.put("leftable_anything",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("touch"),new Grm.Terminal("dosado"),new Grm.Terminal("extend"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("turn"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("scoot"),new Grm.Terminal("back"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("square"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("thru"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("thru"),new Grm.Nonterminal("number",0),new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("hands"),new Grm.Mult(new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("around"),new Grm.Terminal("round"))),Grm.Mult.Type.QUESTION))),Grm.Mult.Type.QUESTION))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("swing"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("tag"),new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("the"),new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("line"),new Grm.Alt(Tools.<Grm>l(new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("all"),new Grm.Terminal("the"),new Grm.Terminal("way"),new Grm.Mult(new Grm.Terminal("through"),Grm.Mult.Type.QUESTION))),Grm.Mult.Type.QUESTION),new Grm.Nonterminal("fraction",0))))))))))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("spin"),new Grm.Terminal("the"),new Grm.Terminal("top"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("pass"),new Grm.Terminal("thru"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("pull"),new Grm.Terminal("by"))))));
        m.put("reversable_anything",new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("flutter"),new Grm.Terminal("wheel"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("half"),new Grm.Terminal("sashay"))),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("anyone",0),new Grm.Terminal("roll"),new Grm.Terminal("away"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("roll"),new Grm.Terminal("away"))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("wheel"),new Grm.Terminal("around"))))));
        m.put("parenthesized_anything",new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("\050"),new Grm.Nonterminal("anything",-1),new Grm.Terminal("\051"))));
        m.put("people",new Grm.Alt(Tools.<Grm>l(new Grm.Nonterminal("genders",-1),new Grm.Nonterminal("all",-1))));
        m.put("genders",new Grm.Alt(Tools.<Grm>l(new Grm.Nonterminal("boys",-1),new Grm.Nonterminal("girls",-1))));
        m.put("boys",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("boys"),new Grm.Terminal("men"))));
        m.put("girls",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("girls"),new Grm.Terminal("ladies"))));
        m.put("all",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("all"),new Grm.Terminal("everyone"),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("every"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("one"),new Grm.Terminal("body"))))))));
        m.put("wave_select",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("centers"),new Grm.Terminal("ends"))));
        m.put("anyone",new Grm.Alt(Tools.<Grm>l(new Grm.Nonterminal("people",-1),new Grm.Nonterminal("wave_select",-1))));
        m.put("number",new Grm.Alt(Tools.<Grm>l(new Grm.Nonterminal("digit",-1),new Grm.Nonterminal("fraction",-1),new Grm.Nonterminal("NUMBER",-1),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("digit",-1),new Grm.Terminal("and"),new Grm.Nonterminal("fraction",-1))))));
        m.put("digit",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("one"),new Grm.Terminal("two"),new Grm.Nonterminal("digit_greater_than_two",-1))));
        m.put("digit_greater_than_two",new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("three"),new Grm.Terminal("four"),new Grm.Terminal("five"),new Grm.Terminal("six"),new Grm.Terminal("seven"),new Grm.Terminal("eight"),new Grm.Terminal("nine"))));
        m.put("fraction",new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("a"),new Grm.Terminal("one"))),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("half"),new Grm.Terminal("third"),new Grm.Terminal("quarter"))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("two"),new Grm.Alt(Tools.<Grm>l(new Grm.Terminal("thirds"),new Grm.Terminal("quarters"))))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("three"),new Grm.Terminal("quarters"))))));
        m.put("cardinal",new Grm.Alt(Tools.<Grm>l(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("once"),new Grm.Terminal("and"),new Grm.Nonterminal("fraction",-1))),new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("twice"),new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("and"),new Grm.Nonterminal("fraction",-1))),Grm.Mult.Type.QUESTION))),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("digit_greater_than_two",-1),new Grm.Mult(new Grm.Concat(Tools.<Grm>l(new Grm.Terminal("and"),new Grm.Nonterminal("fraction",-1))),Grm.Mult.Type.QUESTION),new Grm.Terminal("times"))),new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("NUMBER",-1),new Grm.Terminal("times"))))));
        m.put("start",new Grm.Concat(Tools.<Grm>l(new Grm.Nonterminal("anything",-1),new Grm.Nonterminal("EOF",-1))));

	NONTERMINALS = Collections.unmodifiableMap(m);
    }
    /** Returns the static {@link #NONTERMINALS} field. */
    public Map<String,Grm> grammar() { return NONTERMINALS; }
}
