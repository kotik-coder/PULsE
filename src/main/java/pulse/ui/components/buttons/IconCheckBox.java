package pulse.ui.components.buttons;

import static pulse.util.ImageUtils.loadIcon;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class IconCheckBox extends JCheckBox {

	/*
	 * Checkbox icons for the inner button editor class
	 */

	private final static int ICON_SIZE = 20;
	private final static ImageIcon ICON_ENABLED = loadIcon("checked.png", ICON_SIZE);
	private final static ImageIcon ICON_DISABLED = loadIcon("unchecked.png", ICON_SIZE);

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
		if (selected)
			this.setIcon(ICON_ENABLED);
		else
			this.setIcon(ICON_DISABLED);
	}

}