package net.cscott.sdr.calls;

/** <code>NoMatchException</code> is thrown when a necessary formation
 * can't be found in the given setup.
 * @author C. Scott Ananian
 * @version $Id: NoMatchException.java,v 1.2 2006-10-17 20:03:41 cananian Exp $
 */
public class NoMatchException extends BadCallException {
    public final String target, reason;
    public NoMatchException(String target, String reason) {
        super("No match for "+target+(reason.length()==0 ? "" : (": "+reason)));
        this.target=target;
        this.reason=reason;
    }
}
