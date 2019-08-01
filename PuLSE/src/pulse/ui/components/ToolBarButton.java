package pulse.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.border.EtchedBorder;

public class ToolBarButton extends JButton {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2981000660078137653L;

	public ToolBarButton() {
		super();
		init();
	}
	
	public ToolBarButton(String s) {
		super(s);
		init();			
	}
	
	public void init() {
		setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		setBackground(new Color(240, 248, 255));
		this.setPreferredSize(new Dimension(60, 30));
		setFont(new Font(Messages.getString("ToolBarButton.FontName"), Font.BOLD, 13)); //$NON-NLS-1$
	}
	
}