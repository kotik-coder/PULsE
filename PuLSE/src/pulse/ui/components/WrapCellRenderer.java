package pulse.ui.components;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class WrapCellRenderer extends DefaultListCellRenderer {
	
	   /**
	 * 
	 */
	private static final long serialVersionUID = -1759387268751107278L;
	public static final String HTML_1 = "<html><body style='width: ";
	   public static final String HTML_2 = "px'>";
	   public static final String HTML_3 = "</html>";
	   private int width;

	   public WrapCellRenderer(int width) {
	      this.width = width;
	   }

	   @Override
	   public Component getListCellRendererComponent(JList<?> list, Object value,
	         int index, boolean isSelected, boolean cellHasFocus) {
	      String text = HTML_1 + String.valueOf(width) + HTML_2 + value.toString()
	            + HTML_3;
	      return super.getListCellRendererComponent(list, text, index, isSelected,
	            cellHasFocus);
	   }
	    
}
