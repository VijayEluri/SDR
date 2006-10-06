package net.cscott.sdr.calls;

import static net.cscott.sdr.calls.StandardDancer.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/** A Formation is a set of dancers and positions for them.
 * Certain dancers in the formation can be selected. */
public class Formation {
    private final Map<Dancer,Position> location;
    private final Set<Dancer> selected;

    public boolean isSelected(Dancer d) {
	return selected.contains(d);
    }
    public Set<Dancer> selectedDancers() {
	return selected;
    }
    public Set<Dancer> dancers() {
	return location.keySet();
    }
    public Position location(Dancer d) {
	return location.get(d);
    }
    // utility functions.
    public boolean equals(Object o) {
	if (!(o instanceof Formation)) return false;
	Formation f = (Formation) o;
	return new EqualsBuilder()
	    .append(location, f.location)
	    .append(selected, f.selected)
	    .isEquals();
    }
    public int hashCode() {
	return new HashCodeBuilder()
	    .append(location).append(selected)
	    .toHashCode();
    }
    public String toString() {
	return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
	    .append("location", location)
	    .append("selected", selected)
	    .toString();
    }
    public static final Formation SQUARED_SET = new Formation
	(new DancerInfo(COUPLE_1_BOY,
			Position.getGrid(-1,-3,Rotation.ZERO)),
	 new DancerInfo(COUPLE_1_GIRL,
			Position.getGrid(+1,-3,Rotation.ZERO)),
	 new DancerInfo(COUPLE_2_BOY,
			Position.getGrid(+3,-1,Rotation.THREE_QUARTERS)),
	 new DancerInfo(COUPLE_2_GIRL,
			Position.getGrid(+3,+1,Rotation.THREE_QUARTERS)),
	 new DancerInfo(COUPLE_3_BOY,
			Position.getGrid(+1,+3,Rotation.ONE_HALF)),
	 new DancerInfo(COUPLE_3_GIRL,
			Position.getGrid(-1,+3,Rotation.ONE_HALF)),
	 new DancerInfo(COUPLE_4_BOY,
			Position.getGrid(-3,+1,Rotation.ONE_QUARTER)),
	 new DancerInfo(COUPLE_4_GIRL,
			Position.getGrid(-3,-1,Rotation.ONE_QUARTER))
	 );

    Formation(DancerInfo... dis) {
	Map<Dancer,Position> m = new HashMap<Dancer,Position>();
	Set<Dancer> s = new HashSet<Dancer>();
	for (DancerInfo di : dis) {
	    m.put(di.dancer, di.position);
	    if (di.isSelected)
		s.add(di.dancer);
	}
	this.location = Collections.unmodifiableMap(m);
	this.selected = Collections.unmodifiableSet(s);
    }
    static class DancerInfo {
	final Dancer dancer;
	final Position position;
	final boolean isSelected;
	DancerInfo(Dancer d, Position p, boolean s) {
	    this.dancer = d; this.position = p; this.isSelected = s;
	}
	DancerInfo(Dancer d, Position p) { this(d,p,true); }
    }
}
