/**
 * This package contains the square dance choreography engine, and the basic
 * types and interfaces required to communicate with it.
 *
 * @doc.test Test basic call database functionality:
 *  js> CallDB.INSTANCE.parse(Program.BASIC, "double pass thru").expand()
 *  (In 4 (Seq (Apply tandem (Apply pass thru))))
 *
 * @doc.test Calls with arguments:
 *  js> importPackage(net.cscott.sdr.util) // for Fraction
 *  js> importPackage(net.cscott.sdr.calls.ast) // for Apply
 *  js> sqthr = CallDB.INSTANCE.lookup("square thru")
 *  square thru[basic]
 *  js> def = sqthr.apply(Apply.makeApply("square thru", Fraction.valueOf("1 1/2")))
 *  (Opt (From [FACING COUPLES] (If (Condition and (Condition greater (Condition literal (Condition 3/2)) (Condition literal (Condition 0))) (Condition not (Condition greater (Condition literal (Condition 3/2)) (Condition literal (Condition 1))))) (Seq (Apply _fractional (Apply 3/2) (Apply _in (Apply 2) (Apply pull by)))))) (From [FACING COUPLES] (If (Condition greater (Condition literal (Condition 3/2)) (Condition literal (Condition 1))) (Seq (Part false (Seq (Apply and (Apply _in (Apply 2) (Apply _sq_thru_part)) (Apply left (Apply square thru (Apply _subtract_num (Apply 3/2) (Apply 1)))))))))))
 *
 * @doc.test Call fractionalization:
 *  js> importPackage(net.cscott.sdr.util) // for Fraction
 *  js> importPackage(net.cscott.sdr.calls.ast) // for Apply
 *  js> a = Apply.makeApply("run", Apply.makeApply("boy"))
 *  (Apply run (Apply boy))
 *  js> a.expand()
 *  (In 4 (Opt (From [1x4, BOX, COUPLE, MINIWAVE, 1x2] (Seq (Apply _with designated (Apply boy) (Apply _designees run))))))
 *  js> a = Apply.makeApply("_fractional", Apply.makeApply("1/2"), a)
 *  (Apply _fractional (Apply 1/2) (Apply run (Apply boy)))
 *  js> a.expand()
 *  (In 2 (Opt (From [1x4, BOX, COUPLE, MINIWAVE, 1x2] (Seq (Apply _with designated (Apply boy) (Apply _fractional (Apply 1/2) (Apply _designees run)))))))
 */
package net.cscott.sdr.calls;
