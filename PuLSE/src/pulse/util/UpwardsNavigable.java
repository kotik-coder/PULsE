package pulse.util;

import java.util.ArrayList;
import java.util.List;

public abstract class UpwardsNavigable implements Describable {
	
	private UpwardsNavigable parent;
	private List<HierarchyListener> listeners = new ArrayList<HierarchyListener>();
	
	public void removeHierarchyListeners() {
		this.listeners.clear();
	}
	
	public void addHierarchyListener(HierarchyListener l) {
		this.listeners.add(l);
	}
	
	public List<HierarchyListener> getHierarchyListeners() {
		return listeners;
	}
	
	public void tellParent(PropertyEvent e) {
		if(parent == null)
			return;
		
		parent.listeners.forEach(l -> l.onChildPropertyChanged(e));
		parent.tellParent(e);		
	}


	public UpwardsNavigable getParent() {
		return parent;
	}
	
	public UpwardsNavigable specificAncestor(Class<? extends UpwardsNavigable> aClass) {
		UpwardsNavigable aParent = null;
		for(UpwardsNavigable navigable = this; navigable != null ; navigable = navigable.getParent()) {
			aParent = navigable.getParent();
			if(aParent.getClass().equals(aClass))
				return aParent;
		}
		return null;
	}

	public void setParent(UpwardsNavigable parent) {
		this.parent = parent;
	}
	
}
