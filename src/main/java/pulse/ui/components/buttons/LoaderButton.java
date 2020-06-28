package pulse.ui.components.buttons;

import static pulse.input.InterpolationDataset.StandartType.DENSITY;
import static pulse.input.InterpolationDataset.StandartType.SPECIFIC_HEAT;

import java.awt.Component;
import java.awt.Font;
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

import pulse.input.InterpolationDataset;
import pulse.io.readers.DataLoader;
import pulse.io.readers.ReaderManager;
import pulse.ui.Messages;

@SuppressWarnings("serial")
public class LoaderButton extends JButton {

	private InterpolationDataset.StandartType dataType;
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
		
		setFont(getFont().deriveFont(Font.BOLD, 14f));
		
		addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setCurrentDirectory(dir);

				List<String> extensions = ReaderManager.getDatasetExtensions();
				String[] extArray = extensions.toArray(new String[extensions.size()]);
				fileChooser.setFileFilter(new FileNameExtensionFilter(
						Messages.getString("LoaderButton.SupportedExtensionsDescriptor"), extArray)); //$NON-NLS-1$

				boolean approve = fileChooser.showOpenDialog(
						SwingUtilities.getWindowAncestor((Component) arg0.getSource())) == JFileChooser.APPROVE_OPTION;

				dir = fileChooser.getCurrentDirectory();

				if (!approve)
					return;

				try {
					switch (dataType) {
					case SPECIFIC_HEAT:
						DataLoader.load(SPECIFIC_HEAT, fileChooser.getSelectedFile());
						break;
					case DENSITY:
						DataLoader.load(DENSITY, fileChooser.getSelectedFile());
						break;
					default:
						throw new IllegalStateException("Unrecognised type: " + dataType);
					}
				} catch (IOException e) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) arg0.getSource()),
							Messages.getString("LoaderButton.ReadError"), Messages.getString("LoaderButton.IOError"), //$NON-NLS-1$ //$NON-NLS-2$
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}

				int size = InterpolationDataset.getDataset(dataType).getData().size();

				String label = "";

				switch (dataType) {
				case SPECIFIC_HEAT:
					label = Messages.getString("LoaderButton.5"); //$NON-NLS-1$
					break;
				case DENSITY:
					label = Messages.getString("LoaderButton.6"); //$NON-NLS-1$
					break;
				default:
					throw new IllegalStateException("Unknown data type: " + dataType);
				}

				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((Component) arg0.getSource()),
						"<html>" + label + " data loaded! A total of " + size + " data points loaded.</html>",
						"Data loaded", JOptionPane.INFORMATION_MESSAGE);

			}
		});
	}

	public void setDataType(InterpolationDataset.StandartType dataType) {
		this.dataType = dataType;
	}

}