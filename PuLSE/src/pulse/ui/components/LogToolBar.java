package pulse.ui.components;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import pulse.tasks.Log;
import pulse.ui.Messages;

public class LogToolBar extends JToolBar {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7569969348003139935L;

	public LogToolBar(JFrame parentFrame, LogPane logPane) {
		super();
		setLayout(new GridLayout());
	
		JCheckBox verboseCheckBox = new JCheckBox(Messages.getString("LogToolBar.Verbose")); //$NON-NLS-1$
		verboseCheckBox.setSelected(Log.isVerbose());						
		add(verboseCheckBox);
		
		verboseCheckBox.addActionListener( event -> Log.setVerbose(verboseCheckBox.isSelected()) ) ;
		
		ToolBarButton btnLogSave = new ToolBarButton(Messages.getString("LogToolBar.SaveButton")); //$NON-NLS-1$
		btnLogSave.setEnabled(false);						
		add(btnLogSave);
		
		logPane.getDocument().addDocumentListener( new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				if(logPane.getDocument().getLength() < 1)
					btnLogSave.setEnabled(false);
				else
					btnLogSave.setEnabled(true);
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				if(logPane.getDocument().getLength() < 1)
					btnLogSave.setEnabled(false);
				else
					btnLogSave.setEnabled(true);
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				if(logPane.getDocument().getLength() < 1)
					btnLogSave.setEnabled(false);
				else
					btnLogSave.setEnabled(true);
			}
			
		});
		
		btnLogSave.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logPane.askToSave(parentFrame, Messages.getString("LogToolBar.FileFormatDescriptor")); //$NON-NLS-1$
			}
			
		});
		
		setFloatable(false);
		
	}
	
}
