package net.cscott.sdr.calls;

import net.cscott.sdr.util.Fraction;
import org.apache.commons.lang.builder.*;

/** Position objects represent the position and orientation of a dancer.
 *  The (0,0) coordinate represents the center of the square (or formation),
 *  and dancers
 *  are nominally at least two units away from each other (although breathing
 *  may change this).  A zero rotation for 'facing direction' means
 *  "facing away from the caller".  Positive y is "away from the caller".  Positive
 *  x is "toward the caller's right".  The boy in couple number one
 *  starts out at <code>(-1, -3)</code> facing <code>0</code>.
 *  The <code>facing</code> field may not be <code>null</code>;
 *  to indicate "rotation unspecified" (for example, for phantoms
 *  or when specifying "general lines") use a {@link Rotation} with a
 *  modulus of 0.
 */
public class Position implements Comparable<Position> {
    /** Location. Always non-null. */
    public final Fraction x, y;
    /** Facing direction. Note that {@code facing} should always be an
     * {@link ExactRotation} for real (non-phantom) dancers. */
    public final Rotation facing;
    /** Create a Position object from the given x and y coordinates
     * and {@link Rotation}. */
    public Position(Fraction x, Fraction y, Rotation facing) {
	assert x!=null; assert y!=null; assert facing!=null;
	this.x = x; this.y = y; this.facing = facing;
    }
    /** Create a Position object with integer-valued x and y coordinates. */
    public Position(int x, int y, Rotation facing) {
        this(Fraction.valueOf(x),Fraction.valueOf(y),facing);
    }
    /**
     * Move the given distance in the facing direction.  Requires that
     * the {@code facing} direction be an {@link ExactRotation}.  If
     * {@code stepIn} is true, the distance is negated if the result
     * would end up closer to the origin for positive distance
     * (stepping "in" towards the center of the formation, or further
     * to the origin for negative distance (stepping "out" away from
     * the center).
     *
     * @doc.test Move the couple #1 boy forward two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"n").forwardStep(Fraction.TWO, false)
     *  -1,-1,n
     * @doc.test Move the couple #1 boy backward two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"n").forwardStep(Fraction.TWO.negate(), false)
     *  -1,-5,n
     * @doc.test Move the couple #1 boy "in" two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"n").forwardStep(Fraction.TWO, true)
     *  -1,-1,n
     * @doc.test Move the couple #1 boy "out" two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"n").forwardStep(Fraction.TWO.negate(), true)
     *  -1,-5,n
     * @doc.test Couple #2 boy facing out; move forward two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(3,-1,"e").forwardStep(Fraction.TWO, false)
     *  5,-1,e
     * @doc.test Couple #2 boy facing out; move backward two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(3,-1,"e").forwardStep(Fraction.TWO.negate(), false)
     *  1,-1,e
     * @doc.test Couple #2 boy facing out; move "in" two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(3,-1,"e").forwardStep(Fraction.TWO, true)
     *  1,-1,e
     * @doc.test Couple #2 boy facing out; move "out" two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(3,-1,"e").forwardStep(Fraction.TWO.negate(), true)
     *  5,-1,e
     */
    public Position forwardStep(Fraction distance, boolean stepIn) {
        if (distance.equals(Fraction.ZERO)) return this; // no op.
	assert facing!=null : "rotation unspecified!";
	Fraction dx = ((ExactRotation)facing).toX().multiply(distance);
	Fraction dy = ((ExactRotation)facing).toY().multiply(distance);
        Position p1 = new Position(x.add(dx), y.add(dy), facing);
        if (!stepIn) return p1; // simple case!
        Position p2 = new Position(x.subtract(dx), y.subtract(dy), facing);
        Fraction d1 = p1.x.multiply(p1.x).add(p1.y.multiply(p1.y));
        Fraction d2 = p2.x.multiply(p2.x).add(p2.y.multiply(p2.y));
        int c = d1.compareTo(d2);
        if (c==0) throw new BadCallException("no clear 'in' direction");
        if (distance.compareTo(Fraction.ZERO) > 0)
            return (c>0) ? p2 : p1;
        else
            return (c>0) ? p1 : p2;
    }
    /**
     * Move the given distance perpendicular to the facing direction.
     * Requires that the {@code facing} direction be an {@link
     * ExactRotation}.  If {@code stepIn} is true, the distance is
     * negated if the result would end up closer to the origin for
     * positive distance (stepping "in" towards the center of the
     * formation, or further to the origin for negative distance
     * (stepping "out" away from the center).
     *
     * @doc.test Couple #2 girl move "right" two steps (truck):
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(3,1,"w").sideStep(Fraction.TWO, false)
     *  3,3,w
     * @doc.test Couple #1 boy move "left" two steps (truck):
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"n").sideStep(Fraction.TWO.negate(), false)
     *  -3,-3,n
     * @doc.test Couple #1 boy facing west move "in" two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"w").sideStep(Fraction.TWO, true)
     *  -1,-1,w
     * @doc.test Couple #1 boy facing west move "out" two steps:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> Position.getGrid(-1,-3,"w").sideStep(Fraction.TWO.negate(), true)
     *  -1,-5,w
     */
    public Position sideStep(Fraction distance, boolean stepIn) {
        if (distance.equals(Fraction.ZERO)) return this; // no op.
        assert facing!=null : "rotation unspecified!";
        ExactRotation f = (ExactRotation) facing.add(Fraction.ONE_QUARTER);
        Fraction dx = f.toX().multiply(distance);
        Fraction dy = f.toY().multiply(distance);
        Position p1 = new Position(x.add(dx), y.add(dy), facing);
        if (!stepIn) return p1; // simple case!
        Position p2 = new Position(x.subtract(dx), y.subtract(dy), facing);
        Fraction d1 = p1.x.multiply(p1.x).add(p1.y.multiply(p1.y));
        Fraction d2 = p2.x.multiply(p2.x).add(p2.y.multiply(p2.y));
        int c = d1.compareTo(d2);
        if (c==0) throw new BadCallException("no clear 'in' direction");
        if (distance.compareTo(Fraction.ZERO) > 0)
            return (c>0) ? p2 : p1;
        else
            return (c>0) ? p1 : p2;
    }
    /**
     * Turn in place the given amount.  If {@code faceIn} is true, a positive
     * amount will turn towards the origin; otherwise a positive amount turns
     * clockwise.
     * @doc.test Exercise the turn method; amounts aren't normalized in order
     *  to preserve proper roll/sweep directions:
     *  js> ONE_HALF = net.cscott.sdr.util.Fraction.ONE_HALF
     *  1/2
     *  js> p = Position.getGrid(0,0,"n").turn(ONE_HALF, false)
     *  0,0,s
     *  js> p = p.turn(ONE_HALF, false)
     *  0,0,n
     *  js> p.facing.amount.toProperString()
     *  1
     * @doc.test Turning "in" when in is clockwise:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> p = Position.getGrid(1,1,"s").turn(Fraction.ONE_QUARTER, true)
     *  1,1,w
     * @doc.test Turning "out" when in is clockwise:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> p = Position.getGrid(1,1,"s").turn(Fraction.ONE_QUARTER.negate(), true)
     *  1,1,e
     * @doc.test Turning "in" when in is counter-clockwise:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> p = Position.getGrid(1,1,"n").turn(Fraction.ONE_QUARTER, true)
     *  1,1,w
     * @doc.test Turning "out" when in is counter-clockwise:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> p = Position.getGrid(1,1,"n").turn(Fraction.ONE_QUARTER.negate(), true)
     *  1,1,e
     */
    public Position turn(Fraction amount, boolean faceIn) {
	return this.turn(amount, faceIn, this);
    }
    /**
     * Turn in place the given amount.  If {@code faceIn} is true, a positive
     * amount will turn towards the origin; otherwise a positive amount turns
     * clockwise.<p>
     * This version of the method takes an additional argument specifying
     * a point at which to evaluate the "in/out" direction.
     */
    public Position turn(Fraction amount, boolean faceIn, Position reference) {
        if (amount.equals(Fraction.ZERO)) return this;
	assert facing!=null : "rotation unspecified!";
	Position p1 = new Position(x, y, facing.add(amount));
        if (!faceIn) return p1; // simple case!
        if (!facing.isExact())
            throw new BadCallException
            ("face in from inexact directions not implemented");
        Position p2 = new Position(x, y, facing.subtract(amount));
        // don't allow in/out if facing direction toward the center
        // direction from dancer to center point
        ExactRotation awayCenter =
	    ExactRotation.fromXY(reference.x, reference.y);
        Fraction f = reference.facing
	    .subtract(awayCenter.amount).normalize().amount;
        int czero = f.compareTo(Fraction.ZERO);
        int chalf = f.compareTo(Fraction.ONE_HALF);
        if (czero==0 || chalf==0)
            throw new BadCallException
            ("Can't face in/out when already facing exactly" +
             " toward/away from the center");
        assert czero > 0;
        if (chalf > 0)
            return p2; // "in" is ccw here.
        else
            return p1; // "in" is cw.
    }
    /** Rotate this position around the origin by the given amount.
     * @doc.test Rotating the #1 boy by 1/4 gives the #4 boy position:
     *  js> p = Position.getGrid(-1,-3,0)
     *  -1,-3,n
     *  js> p.rotateAroundOrigin(ExactRotation.ONE_QUARTER)
     *  -3,1,e
     */
    public Position rotateAroundOrigin(ExactRotation rot) {
        // x' =  x*cos(rot) + y*sin(rot)
        // y' = -x*sin(rot) + y*cos(rot)
        // where sin(rot) = rot.toX() and cos(rot) = rot.toY()
        Fraction cos = rot.toY(), sin = rot.toX();
        Fraction nx = this.x.multiply(cos).add(this.y.multiply(sin));
        Fraction ny = this.y.multiply(cos).subtract(this.x.multiply(sin));
        return new Position(nx, ny, facing.add(rot.amount));
    }
    /** Normalize (restrict to 0-modulus) the rotation of the given position.
     * @doc.test Show normalization after two 180-degree turns:
     *  js> importPackage(net.cscott.sdr.util)
     *  js> p = Position.getGrid(0,0,"e").turn(Fraction.ONE_HALF, false)
     *  0,0,w
     *  js> p = p.turn(Fraction.ONE_HALF, false)
     *  0,0,e
     *  js> p.facing.amount.toProperString()
     *  1 1/4
     *  js> p = p.normalize()
     *  0,0,e
     *  js> p.facing.amount.toProperString()
     *  1/4
     */
    public Position normalize() {
        return new Position(x, y, facing.normalize());
    }

    // positions in the standard 4x4 grid.
    /** Returns a position corresponding to the standard square
     *  dance grid.  0,0 is the center of the set, and odd coordinates
     *  between -3 and 3 correspond to the standard 4x4 grid.
     * @doc.test Some sample grid locations:
     *  js> Position.getGrid(0,0,ExactRotation.ZERO)
     *  0,0,n
     *  js> Position.getGrid(-3,3,ExactRotation.WEST)
     *  -3,3,w
     */
    public static Position getGrid(int x, int y, ExactRotation r) {
        assert r != null;
	return new Position
	    (Fraction.valueOf(x), Fraction.valueOf(y), r);
    }
    /** Returns a position corresponding to the standard square
     *  dance grid.  0,0 is the center of the set, and odd coordinates
     *  between -3 and 3 correspond to the standard 4x4 grid.
     *  For convenience, the direction is specified as a string
     *  valid for <code>ExactRotation.valueOf(String)</code>.
     * @doc.test Some sample grid locations:
     *  js> Position.getGrid(0,0,"n")
     *  0,0,n
     *  js> Position.getGrid(1,2,"e")
     *  1,2,e
     */
    public static Position getGrid(int x, int y, String direction) {
	return getGrid(x,y,ExactRotation.fromAbsoluteString(direction));
    }

    // utility functions.
    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Position)) return false;
	Position p = (Position) o;
	return new EqualsBuilder()
	    .append(x, p.x)
	    .append(y, p.y)
	    .append(facing, p.facing)
	    .isEquals();
    }
    @Override
    public int hashCode() {
        if (hashCode==0)
            hashCode = new HashCodeBuilder()
            .append(x).append(y).append(facing)
            .toHashCode();
        return hashCode;
    }
    private transient int hashCode = 0;
    @Override
    public String toString() {
	return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
	    .append("x", x.toProperString())
	    .append("y", y.toProperString())
	    .append("facing", facing.toAbsoluteString())
	    .toString();
    }
    /**
     * Compare two {@link Position}s.  We use reading order: top to bottom,
     * then left to right.  Ties are broken by facing direction: first
     * the most specific rotation modulus, then by normalized direction.
     * @doc.test Top to bottom:
     *  js> Position.getGrid(0,0,"n").compareTo(Position.getGrid(1,1,"n")) > 0
     *  true
     * @doc.test Left to right:
     *  js> Position.getGrid(1,0,"n").compareTo(Position.getGrid(0,0,"n")) > 0
     *  true
     * @doc.test Most specific rotation modulus first:
     *  js> new Position["(int,int,net.cscott.sdr.calls.Rotation)"](
     *    >              0,0,Rotation.fromAbsoluteString("|")
     *    >              ).compareTo(Position.getGrid(0,0,"n")) > 0
     *  true
     * @doc.test Normalized direction:
     *  js> Position.getGrid(0,0,"e").compareTo(Position.getGrid(0,0,"n")) > 0
     *  true
     * @doc.test Equality:
     *  js> Position.getGrid(1,2,"w").compareTo(Position.getGrid(1,2,"w")) == 0
     *  true
     */
    public int compareTo(Position p) {
        int c = -this.y.compareTo(p.y);
        if (c!=0) return c;
        c = this.x.compareTo(p.x);
        if (c!=0) return c;
        c = -this.facing.modulus.compareTo(p.facing.modulus);
        if (c!=0) return c;
        c = this.facing.normalize().amount.compareTo
            (p.facing.normalize().amount);
        return c;
    }
}
