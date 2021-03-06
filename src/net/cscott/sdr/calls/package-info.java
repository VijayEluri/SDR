/**
 * This package contains the square dance choreography engine, and the basic
 * types and interfaces required to communicate with it.
 *
 * @doc.test Test basic call database functionality:
 *  js> CallDB.INSTANCE.parse(Program.BASIC, "couples trade").
 *    >     evaluator(null).simpleExpansion()
 *  (In '6 (Seq (Apply (Expr as couples 'trade))))
 *
 * @doc.test Calls with arguments:
 *  js> importPackage(net.cscott.sdr.util) // for Fraction
 *  js> importPackage(net.cscott.sdr.calls.ast) // for Apply
 *  js> sqthr = CallDB.INSTANCE.lookup("square thru")
 *  square thru[basic]
 *  js> ds = new DanceState(new DanceProgram(Program.C4), Formation.SQUARED_SET); undefined;
 *  js> def = sqthr.getEvaluator(ds, java.util.Arrays.asList(Expr.literal("1 1/2"))).simpleExpansion()
 *  (Opt (From (Expr condition (Expr and (Expr greater '1 1/2 '0) (Expr not (Expr greater '1 1/2 '1)))) (Seq (Apply (Expr _fractional '1 1/2 (Expr _in '2 'pull by))))) (From (Expr condition (Expr greater '1 1/2 '1)) (Seq (Part 'DIVISIBLE '1 (Seq (Apply (Expr _in '2 '_square thru part)))) (Part 'DIVISIBLE (Expr _ceil (Expr _subtract num '1 1/2 '1)) (Seq (Apply (Expr left (Expr square thru (Expr _subtract num '1 1/2 '1)))))))))
 *
 * @doc.test Call fractionalization:
 *  js> importPackage(net.cscott.sdr.util) // for Fraction
 *  js> importPackage(net.cscott.sdr.calls.ast) // for Apply
 *  js> a = new Apply(new Expr("run", Expr.literal("boy")))
 *  (Apply (Expr run 'boy))
 *  js> a.evaluator(null).simpleExpansion()
 *  (In '4 (Opt (From (Expr or '1 x4 '2 x4 'BOX (Expr mixed 'COUPLE 'RH MINIWAVE 'LH MINIWAVE) '1 x2) (Seq (Apply (Expr _with designated 'boy '_designees run))))))
 *  js> a = new Apply(new Expr("_fractional", Expr.literal("1/2"), a.call))
 *  (Apply (Expr _fractional '1/2 (Expr run 'boy)))
 *  js> a.evaluator(null).simpleExpansion()
 *  (In (Expr _multiply num '1/2 '4) (Opt (From (Expr or '1 x4 '2 x4 'BOX (Expr mixed 'COUPLE 'RH MINIWAVE 'LH MINIWAVE) '1 x2) (Seq (Apply (Expr _with designated 'boy (Expr _fractional '1/2 '_designees run)))))))
 */
package net.cscott.sdr.calls;
