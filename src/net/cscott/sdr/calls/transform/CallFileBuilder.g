/**
 * Post-process the ANTLR AST generated by {@link CallFileParser} to create
 * a proper parse tree of {@link net.cscott.sdr.calls.ast.AstNode}.
 * @doc.test Trivial example:
 *  js> CallFileBuilder.parseCalllist("program:basic")
 *  []
 * @doc.test An actual call definition:
 *  js> CallFileBuilder.parseCalllist("program:basic\ndef: foo\n call: bar,bat")
 *  [foo[basic]]
 * @doc.test Parsing spoken-language grammar rules:
 *  js> CallFileBuilder.parseGrm("foo bar|bat? baz")
 *  foo bar|bat? baz
 *  js> CallFileBuilder.parseGrm("square thru <number> (hands (around|round)?)?")  
 *  square thru <number> (hands (around|round)?)?
 * @doc.test Call with long example clause:
 *  js> CallFileBuilder.parseCalllist('program: basic\n'+
 *    >    'def: ferris wheel\n'+
 *    >    '  call: stretch(wheel and deal)\n'+
 *    >    '  example: ferris wheel\n'+
 *    >    '    before:\n'+
 *    >    '    !  ^ ^\n'+
 *    >    '    !  A a c C\n'+
 *    >    '    !  ^ ^ v v\n'+
 *    >    '    !  B b d D\n'+
 *    >    '    !      v v\n'+
 *    >    '    after:\n'+
 *    >    '    !  a A\n'+
 *    >    '    !  v v\n'+
 *    >    '    !  b B\n'+
 *    >    '    !  v v\n'+
 *    >    '    !  ^ ^\n'+
 *    >    '    !  C c\n'+
 *    >    '    !  ^ ^\n'+
 *    >    '    !  D d\n')
 *  [ferris wheel[basic]]
 */
tree grammar CallFileBuilder;
options {
    tokenVocab = CallFile;
    ASTLabelType = CommonTree;
}
@header {
package net.cscott.sdr.calls.transform;

import static net.cscott.sdr.calls.transform.BuilderHelper.*;
import net.cscott.sdr.calls.*;
import net.cscott.sdr.calls.ast.*;
import net.cscott.sdr.calls.grm.Grm;
import net.cscott.sdr.calls.grm.Rule;
import net.cscott.sdr.calls.grm.SimplifyGrm;
import net.cscott.sdr.util.*;
import java.io.Reader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
}
@members {
	private final Set<String> names = new HashSet<String>();
	private final List<Call> db = new ArrayList<Call>();
	public List<Call> getList() { return Collections.unmodifiableList(db); }
	Program currentProgram = null;
	// quick helper
	public <T> T ifNull(T t, T otherwise) { return (t==null)?otherwise:t; }
	public BDirection d(BDirection d) { return ifNull(d, BDirection.ASIS); }
	private Map<String,Integer> scope = new HashMap<String,Integer>();
	private void semex(Tree a, String s) throws SemanticException {
		throw new SemanticException(s, a.getLine(), a.getCharPositionInLine());
	}
    class SemanticException extends RuntimeException {
        SemanticException(String msg, int line, int column) {
            super("line "+line+":"+column+" "+msg);
            emitErrorMessage(getMessage());
        }
    }
    public CallFileBuilder(Tree t) { this(new CommonTreeNodeStream(t)); }
    public static List<Call> parseCalllist(Reader r)
        throws java.io.IOException, RecognitionException {
        CallFileLexer lexer = new CallFileLexer(new ANTLRReaderStream(r));
        CallFileParser cfp = new CallFileParser(new CommonTokenStream(lexer));
        return parseCalllist(cfp);
    }
    public static List<Call> parseCalllist(String input)
        throws RecognitionException {
        return parseCalllist(new CallFileParser(input));
    }
    private static List<Call> parseCalllist(CallFileParser cfp)
        throws RecognitionException {
        Tree tree = (Tree) cfp.calllist().getTree();
        CallFileBuilder cfb=new CallFileBuilder(tree);
        cfb.calllist(); // throw away result
        // note that we are silently recovering from syntax errors here.
        return cfb.getList();
    }
    public static Grm parseGrm(String rule)
        throws RecognitionException {
        CallFileLexer lexer = new CallFileLexer(rule);
        // don't need an indent processor, but do need to setup lexer
        lexer.setToRuleStart();
        // Create a parser that reads from the scanner
        CallFileParser parser= new CallFileParser(new CommonTokenStream(lexer));
        // start parsing at the grammar_start rule
        Tree tree = (Tree) parser.grammar_start().getTree();
        // check for errors
        if (parser.getNumberOfSyntaxErrors() != 0)
            throw new IllegalArgumentException("Syntax errors in "+rule);
        // now build a proper AST.
        CallFileBuilder builder = new CallFileBuilder(tree);
        return builder.grammar_start();
    }
}
// exit immediately on error; don't try to recover
@rulecatch {
    catch (RecognitionException e) {
        throw e;
    }
}
    
// start production for parsing call file.
calllist
	: ^(CALLLIST (program)* )
	;
// start production for parsing grammar rules
grammar_start returns [Grm g]
	: grm_rule { $g=$grm_rule.g; }
	;


program
	: ^(PROGRAM id=IDENT { currentProgram=Program.valueOf(id.getText().toUpperCase()); } (def)* )
	;

def
@init {
  String n=null; Apply a=null;
  Set<String> optional = new HashSet<String>();
}
	: ^(d=DEF cb=call_body
    { if (!cb.isConstant()) semex(d, "Bad call definition");
	  a = cb.build(null);
	  n = a.callName;
	  // if there are arguments, add them to our scope.
	  int i=0;
	  for (Apply arg : a.args) {
	    if (arg.args.size()!=0) semex(d, "Arguments can't have arguments");
	    scope.put(arg.callName, i++);
	  }
	}
       ( ^(OPTIONAL (id=IDENT {optional.add(id.getText().toUpperCase());})+ ) )?
       ( ^(SPOKEN (prec=number)? g=grm_rule ) )?
       example*
	   p=pieces)
	{ if (names.contains(n)) semex(d, "duplicate call: "+n);
      n = n.intern();
      names.add(n);

	  String ruleName = optional.contains("LEFT") ? "leftable_anything" :
	  	optional.contains("REVERSE") ? "reversable_anything" :
	  	"anything";
	  Rule rule = null;
	  if (g==null && !n.startsWith("_"))
	    g = Grm.mkGrm(n.split("\\s+"));
	  if (g!=null)
	    rule = new Rule(ruleName, SimplifyGrm.simplify(g),
						prec==null ? Fraction.ZERO : prec);

      Call call = makeCall(n, currentProgram, p, a.args.size(), rule);
	  db.add(call);

	  scope.clear();
	}
	;
	
example
	: ^(EXAMPLE call_body BEFORE FIGURE AFTER FIGURE)
        // XXX we currently throw these figures away.
        //     we should save them, and later check them.
	;

pieces returns [B<? extends Comp> r]
	: opt { $r=$opt.o; }
	| seq { $r=$seq.s; }
	| par { $r=$par.p; }
	| res { $r=$res.c; };
	
opt returns [B<Opt> o]
@init { List<B<OptCall>> l = new ArrayList<B<OptCall>>(); }
	: ^(OPT (oc=one_opt {l.add(oc);})+)
	{ $o = mkOpt(l); }
	;
one_opt returns [B<OptCall> oc]
	: ^(FROM f=simple_body co=pieces)
	{ $oc = mkOptCall(OptCall.parseFormations(f), co); }
	;
seq returns [B<Seq> s]
@init { List<B<? extends SeqCall>> l = new ArrayList<B<? extends SeqCall>>(); }
	: ^(SEQ (sc=one_seq {l.add(sc);})+)
	{ $s = mkSeq(l); }
	;
one_seq returns [B<? extends SeqCall> sc]
@init { EnumSet<BPrimAttrib> a=EnumSet.noneOf(BPrimAttrib.class); }
	: ^(PRIM (dx=direction)? x=number (dy=direction)? y=number (dr=direction | r=rotation) ^(ATTRIBS ( attribs[a] )* ) )
	{ $sc=mkPrim(d(dx), x, d(dy), y, d(dr), ifNull(r,ExactRotation.ONE_QUARTER), a); }
	| ^(CALL call_body) { $sc=$call_body.ast; }
	| ^(PART p=pieces)
	{ $sc = mkPart(true, p); /* divisible part */}
	| ^(IPART p=pieces)
	{ $sc = mkPart(false, p); /* indivisible part */}
	;

direction returns [BDirection d]
	: IN { $d=BDirection.IN; }
	| OUT { $d=BDirection.OUT; }
	;
rotation returns [ExactRotation r]
	: RIGHT { $r = ExactRotation.ONE_QUARTER; }
	| LEFT { $r = ExactRotation.mONE_QUARTER; }
	| NONE { $r = ExactRotation.ZERO; }
	;
attribs[EnumSet<BPrimAttrib> s]
	: ARC { $s.add(BPrimAttrib.FORCE_ARC); }
	| LEFT { $s.add(BPrimAttrib.PASS_LEFT); }
	;

par returns [B<Par> p]
@init {List<B<ParCall>> l=new ArrayList<B<ParCall>>();}
    : ^(PAR (pc=one_par {l.add(pc);})+)
	{ $p = mkPar(l); }
    ;

one_par returns [B<ParCall> pc]
    : ^(SELECT sl=simple_ref_body p=pieces)
	{ $pc = mkParCall(sl, p); }
	;
// restrictions/timing
res returns [B<? extends Comp> c]
    : ^(IN f=number p=pieces)
	{ $c = mkIn(f, p); }
    | ^(IF cd=cond_body p=pieces)
	{ $c = mkIf(cd, p); }
    ;
	
simple_words returns [String r]
@init {
  StringBuilder sb = new StringBuilder();
}
	: ^(ITEM s=simple_word {sb.append(s);}
	        (s=simple_word {sb.append(' ');sb.append(s);})* )
      { $r = sb.toString(); }
	;
simple_word returns [String r]
	: i=IDENT { $r = i.getText(); }
	| n=number { $r = n.toProperString(); }
	;

simple_body returns [List<String> l]
@init { $l = new ArrayList<String>(); }
	: ^(BODY (s=simple_words {$l.add(s);} )+)
	;

words_or_ref returns [B<String> b]
	: s=simple_words
	{ $b = mkConstant(s); }
	| r=ref
	{ final int param = r;
	  $b = new B<String>() {
	  	public String build(List<Apply> args) {
	  	  assert args.get(param).args.isEmpty();
          return args.get(param).callName;
	  	}
	  };
	}
	;

simple_ref_body returns [List<B<String>> l]
@init { $l = new ArrayList<B<String>>(); }
    : ^(BODY (s=words_or_ref {$l.add(s);} )+)
    ;

call_body returns [B<Apply> ast]
	// shorthand: 3/4 (foo) = fractional(3/4, foo)
	: ( ^(APPLY ^(ITEM number) (.)* ) ) =>
	  ^(APPLY ^(ITEM n=number) args=call_args )
	{   args.add(0, mkConstant(Apply.makeApply(n.toString().intern())));
		$ast = mkApply("_fractional", args); }
	// parameter reference
	| ( ^(APPLY REF (.)* ) ) =>
	  ^(APPLY r=ref args=call_args )
	{ final int param = r;
	  final List<B<Apply>> call_args = args;
	  if (call_args.isEmpty()) {
	  	// if no args, then substitute given Apply node wholesale.
	    $ast = new B<Apply>() {
	    	public Apply build(List<Apply> args) {
	    		return args.get(param);
	    	}
	    };
	  } else {
	  	// otherwise, just use the given parameter as a string.
	    $ast = new B<Apply>() {
	    	public Apply build(List<Apply> args) {
				assert args.get(param).args.isEmpty();
	    		String callName = args.get(param).callName;
	    		return new Apply(callName, reduce(call_args, args));
	    	}
	    };
	  }
	}
	// standard rule
	| ^(APPLY s=simple_words args=call_args )
	{ $ast = mkApply(s.intern(), args); }
	;
ref returns [int v]
	: r=REF
	{ if (!scope.containsKey(r.getText())) semex(r, "No argument named "+r.getText());
	  $v=scope.get(r.getText()); }
	;
call_args returns [List<B<Apply>> l]
@init { $l = new ArrayList<B<Apply>>(); }
	: (c=call_body {$l.add(c);} )*
	;
cond_body returns [B<Condition> c]
	// parameter reference
	: ( ^(CONDITION REF (.)* ) ) =>
	  ^(CONDITION r=ref args=cond_args )
	{ final int param = r;
	  final List<B<Condition>> cond_args = args;
	  // use the given parameter as a string.
	  $c = new B<Condition>() {
	    	public Condition build(List<Apply> args) {
				assert args.get(param).args.isEmpty();
	    		String predicate = args.get(param).callName;
	    		return new Condition(predicate, reduce(cond_args, args));
	    	}
	  };
	}
	| ^(CONDITION s=simple_words args=cond_args )
	{ $c = mkCondition(s.intern(), args); }
	;
cond_args returns [List<B<Condition>> l]
@init { $l = new ArrayList<B<Condition>>(); }
	: (c=cond_body {$l.add(c);} )*
	;

number returns [Fraction r]
	: n=NUMBER
	{ $r = Fraction.valueOf(n.getText()); }
	;
	
grm_rule returns [Grm g]
@init { List<Grm> l = new ArrayList<Grm>(); }
	: ^(VBAR (gg=grm_rule {l.add(gg);})+ )
	{ $g = new Grm.Alt(l); }
	| ^(ADJ (gg=grm_rule {l.add(gg);})+ )
	{ $g = new Grm.Concat(l); }
	| ^(PLUS gg=grm_rule )
	{ $g = new Grm.Mult(gg, Grm.Mult.Type.PLUS); }
	| ^(STAR gg=grm_rule )
	{ $g = new Grm.Mult(gg, Grm.Mult.Type.STAR); }
	| ^(QUESTION gg=grm_rule )
	{ $g = new Grm.Mult(gg, Grm.Mult.Type.QUESTION); }
	| i=IDENT
	{ $g = new Grm.Terminal(i.getText()); }
	| ^(REF r=IDENT (p=grm_ref_or_int)? )
	{ $g = new Grm.Nonterminal(r.getText(), p==null ? -1 : p); }
	;
grm_ref_or_int returns [Integer i]
	: p=IDENT
	{ if (!scope.containsKey(p.getText()))
        semex(p, "No argument named "+p.getText());
      $i=scope.get(p.getText()); }
	| n=INTEGER { $i=Integer.valueOf(n.getText()); }
	;
