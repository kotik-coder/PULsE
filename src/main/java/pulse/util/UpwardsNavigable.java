package pulse.util;

import java.util.ArrayList;
import java.util.List;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;

/**
 * <p>
 * An {@code UpwardsNavigable} provides a two-way connection with the other
 * {@code Describable} in an asymmetric (upwards-oriented) manner (hence its
 * name). The {@code UpwardsNavigable} stores information about its parent,
 * which stands higher in hierarchy than this object. The {@code parent} is
 * always informed if any changes happen with its child properties.
 * </p>
 *
 */
public abstract class UpwardsNavigable implements Descriptive {

    private UpwardsNavigable parent;
    private final List<HierarchyListener> listeners = new ArrayList<>();

    public final void removeHierarchyListeners() {
        this.listeners.clear();
    }

    public final void removeHierarchyListener(HierarchyListener l) {
        this.listeners.remove(l);
    }

    public final void addHierarchyListener(HierarchyListener l) {
        this.listeners.add(l);
    }

    public final List<HierarchyListener> getHierarchyListeners() {
        return listeners;
    }

    /**
     * Recursively informs the parent, the parent of its parent, etc. of this
     * {@code UpwardsNavigable} that an action has been taken on its child's
     * properties specified by {
     *
     * @e}.
     *
     * @param e the property event
     */
    public void tellParent(PropertyEvent e) {
        if (parent != null) {
            parent.listeners.forEach(l -> l.onChildPropertyChanged(e));
            parent.tellParent(e);
        }
    }

    /**
     * Return the parent of this {@code UpwardsNavigable} -- if is has been
     * previously explicitly set.
     *
     * @return the parent (which is also an {@code UpwardsNavigable}).
     */
    public UpwardsNavigable getParent() {
        return parent;
    }

    /**
     * Finds an ancestor that looks similar to {@code aClass} by recursively
     * calling {@code getParent()}.
     *
     * @param aClass a class which should be similar to an ancestor of this
     * {@code UpwardsNavigable}
     * @return the ancestor, which is a parent, or grand-parent, or
     * grand-grand-parent, etc. of this {@code UpwardsNavigable}.
     */
    public UpwardsNavigable specificAncestor(Class<? extends UpwardsNavigable> aClass) {
        if (aClass.equals(this.getClass())) {
            return this;
        }
        UpwardsNavigable result = null;
        if (parent != null) {
            result = parent.getClass().equals(aClass) ? parent : parent.specificAncestor(aClass);
        }
        return result;
    }

    /**
     * Explicitly sets the parent of this {@code UpwardsNavigable}.
     *
     * @param parent the new parent that will adopt this
     * {@code UpwardsNavigable}.
     */
    public final void setParent(UpwardsNavigable parent) {
        this.parent = parent;
    }

    /**
     * Retrieves the Identifier of the SearchTaks this UpwardsNavigable belongs
     * to.
     *
     * @return the identifier of the SearchTask
     */
    public Identifier identify() {
        var un = specificAncestor(SearchTask.class);
        return un == null ? null : ((SearchTask) un).getIdentifier();
    }

    /**
     * Uses the SearchTask id (if present) to describe this UpwardsNavigable.
     */
    @Override
    public String describe() {
        var id = identify();
        String name = getClass().getSimpleName();
        return id == null ? name : name + "_" + id.getValue();
    }

}
