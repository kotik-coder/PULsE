package pulse.ui.components;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import pulse.io.readers.ReaderManager;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

@SuppressWarnings("serial")
public class LoaderButton extends JButton {

	private DataType dataType;
	private static File dir;
	
	public LoaderButton() {
		super();
		init();
	}
	
	public LoaderButton(String str) {
		super(str);
		init();
	}
	
	public void init() {
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				System.out.println("ok");
				fileChooser.setCurrentDirectory(dir);

				List<String> extensions = ReaderManager.getDatasetExtensions();							
				String[] extArray = extensions.toArray(new String[extensions.size()]);							
				fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("LoaderButton.SupportedExtensionsDescriptor"), extArray)); //$NON-NLS-1$

				boolean approve = fileChooser.showOpenDialog(
						SwingUtilities.getWindowAncestor((Component) arg0.getSource())) == JFileChooser.APPROVE_OPTION;
			
				dir = fileChooser.getCurrentDirectory();
				
				if(!approve) return;
				
				try {
					switch(dataType) {
					case SPECIFIC_HEAT : TaskManager.loadSpecificHeatData(fileChooser.getSelectedFile()); break;
					case DENSITY	   : TaskManager.loadDensityData(fileChooser.getSelectedFile()); break;
					}
				} catch (IOException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) arg0.getSource()),
							Messages.getString("LoaderButton.ReadError"), Messages.getString("LoaderButton.IOError"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
					e.printStackTrace();
				}

				int size = 0;
				
				switch(dataType) {
				case SPECIFIC_HEAT : size = TaskManager.getSpecificHeatCurve().getData().size(); break;
				case DENSITY : size = TaskManager.getDensityCurve().getData().size(); break;
				}
				
				String label = ""; //$NON-NLS-1$
				
				switch(dataType) {
					case SPECIFIC_HEAT : label = Messages.getString("LoaderButton.5"); break; //$NON-NLS-1$
					case DENSITY : label = Messages.getString("LoaderButton.6"); //$NON-NLS-1$
				}
				
				JOptionPane
						.showMessageDialog(SwingUtilities.getWindowAncestor((Component) arg0.getSource()),
								"<html>" + label + " data loaded! A total of " //$NON-NLS-1$ //$NON-NLS-2$
										+ size + " data points loaded.</html>", //$NON-NLS-1$
						"Data loaded", JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$

			}
		});
	}
	
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}


	public enum DataType {
		SPECIFIC_HEAT, DENSITY;
	}
	
}