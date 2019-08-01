package pulse.ui.frames;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JScrollPane;

import pulse.ui.components.PropertyHolderTable;
import pulse.util.PropertyHolder;

import java.awt.Font;

public class DataFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private PropertyHolderTable dataTable;
	private Component ancestorFrame;
	private PropertyHolder dataObject;
	private final static Font TABLE_FONT = new Font(Messages.getString("DataFrame.FontName"), Font.PLAIN, 12);

	@Override 
	public void dispose() {;
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
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
		dataTable.setRowHeight(30);
		dataTable.setFont(TABLE_FONT); //$NON-NLS-1$
	
		scrollPane.setViewportView(dataTable);
	}
	
	public PropertyHolder getDataObject() {
		return dataObject;
	}

}
