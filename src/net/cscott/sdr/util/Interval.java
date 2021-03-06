package net.cscott.sdr.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** An interval between two {@link Fraction}s.  It is open or closed
 * as you see fit.  Intervals are ordered first by their start,
 * and then (if two intervals have the same start) by their ends.
 * The start fraction must always be less than or equal to the end
 * fraction.
 * @author C. Scott Ananian
 * @version $Id: Interval.java,v 1.2 2006-10-23 16:54:12 cananian Exp $
 */
public class Interval implements Comparable<Interval> {
    public final Fraction start, end;
    /** Create an {@link Interval} from {@code start} to {@code end}.
     * The {@code start} parameter must be less than or equal to {@code end}.
     */
    public Interval(Fraction start, Fraction end) {
        this.start=start; this.end=end;
        assert this.start.compareTo(this.end) <= 0;
    }
    /** Compare two {@link Interval}s.  Intervals are ordered first by
     * start, and if the starts are equal, by their ends.
     */
    public int compareTo(Interval i) {
        int c = this.start.compareTo(i.start);
        if (c!=0) return c;
        return this.end.compareTo(i.end);
    }
    public boolean equals(Object o) {
        if (!(o instanceof Interval)) return false;
        Interval i = (Interval) o;
        return new EqualsBuilder()
            .append(this.start, i.start)
            .append(this.end, i.end)
            .isEquals();
    }
    public int hashCode() {
        return new HashCodeBuilder()
            .append(start).append(end)
            .toHashCode();
    }
    public String toString() {
        return "["+start.toProperString()+","+end.toProperString()+"]";
    }
}
