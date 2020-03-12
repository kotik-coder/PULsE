package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import pulse.ui.Messages;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.PropertyHolder;

public class DataFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private PropertyHolderTable dataTable;
	private Component ancestorFrame;
	private PropertyHolder dataObject;
	private final static Font TABLE_FONT = new Font(Messages.getString("DataFrame.FontName"), Font.PLAIN, 16);
	private	final static int ROW_HEIGHT = 50;

	@Override 
	public void dispose() {
		if(ancestorFrame != null) {
			ancestorFrame.setEnabled(true);
			if(ancestorFrame.getParent() != null)
				ancestorFrame.getParent().setEnabled(true);
		}		
		super.dispose();
	}

	/**
	 * Create the frame.
	 */
	public DataFrame(PropertyHolder dataObject, Component ancestor) {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.ancestorFrame = ancestor.getParent();
		this.dataObject = dataObject;
		if(ancestor != null) {
			ancestor.setEnabled(false);
			if(ancestorFrame != null)
				ancestorFrame.setEnabled(false);
		}
		setType(Type.UTILITY);
		setResizable(false);
		setAlwaysOnTop(true);
		setTitle(dataObject.getClass().getSimpleName() + " properties"); //$NON-NLS-1$
		setBounds(100, 100, 450, 300);

		contentPane = new JPanel();
		contentPane.setForeground(Color.BLACK);
		contentPane.setBorder(null);
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		dataTable = new PropertyHolderTable(dataObject);
		dataTable.setFont(TABLE_FONT); 
		dataTable.setRowHeight(ROW_HEIGHT);
		
		scrollPane.setViewportView(dataTable);
	}
	
	public PropertyHolder getDataObject() {
		return dataObject;
	}

}
