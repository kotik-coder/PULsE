package pulse.util;

import java.io.Serializable;

/**
 * An hierarchy listener, which listens to any changes happening with the
 * children of an {@code UpwardsNavigable}.
 *
 * @see pulse.util.UpwardsNavigable
 *
 */
public interface HierarchyListener extends Serializable {

    /**
     * This is invoked by the {@code UpwardsNavigable} when an event resulting
     * in a change of the child's property has occurred.
     *
     * @param property the event data.
     */
    public void onChildPropertyChanged(PropertyEvent property);

}
