package pulse.ui.components;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

import pulse.ui.Launcher;

public class IconCheckBox extends JCheckBox {

	/*
	 * Checkbox icons for the inner button editor class
	 */
	
    private final static int ICON_SIZE = 32;
    private final static ImageIcon ICON_ENABLED		= Launcher.loadIcon("/checked.png", ICON_SIZE);
    private final static ImageIcon ICON_DISABLED	= Launcher.loadIcon("/unchecked.png", ICON_SIZE);
    
    public IconCheckBox() {
    	super();
    	setHorizontalAlignment(CENTER);
    }
    
    public IconCheckBox(boolean b) {
    	super("", b);
    	setSelected(b);
    }
    
    @Override
    public void setSelected(boolean selected) {
    	super.setSelected(selected);
    	if(selected)
    		this.setIcon(ICON_ENABLED);
    	else
    		this.setIcon(ICON_DISABLED);
    }
	
}