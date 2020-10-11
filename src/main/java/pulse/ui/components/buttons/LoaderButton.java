package pulse.ui.components.buttons;

import static java.awt.Toolkit.getDefaultToolkit;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.input.InterpolationDataset.getDataset;
import static pulse.input.InterpolationDataset.StandartType.DENSITY;
import static pulse.input.InterpolationDataset.StandartType.HEAT_CAPACITY;
import static pulse.io.readers.ReaderManager.getDatasetExtensions;
import static pulse.ui.Messages.getString;
import static pulse.ui.components.DataLoader.load;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import pulse.input.InterpolationDataset;
import pulse.util.ImageUtils;

@SuppressWarnings("serial")
public class LoaderButton extends JButton {

	private InterpolationDataset.StandartType dataType;
	private static File dir;

	private final static Color NOT_HIGHLIGHTED = UIManager.getColor("Button.background");
	private final static Color HIGHLIGHTED = ImageUtils.blend(NOT_HIGHLIGHTED, Color.red, 0.75f);

	public LoaderButton() {
		super();
		init();
	}

	public LoaderButton(String str) {
		super(str);
		init();
	}

	public void init() {

		InterpolationDataset.addListener(e -> {
			if (dataType == e)
				highlight(false);
		});

		addActionListener((ActionEvent arg0) -> {
			var fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(dir);
			var extensions = getDatasetExtensions();
			var extArray = extensions.toArray(new String[extensions.size()]);
			fileChooser.setFileFilter(
					new FileNameExtensionFilter(getString("LoaderButton.SupportedExtensionsDescriptor"), extArray)); //$NON-NLS-1$
			// $NON-NLS-1$
			var approve = fileChooser.showOpenDialog(getWindowAncestor((Component) arg0.getSource())) == APPROVE_OPTION;
			dir = fileChooser.getCurrentDirectory();
			if (!approve)
				return;
			try {
				switch (dataType) {
				case HEAT_CAPACITY:
					load(HEAT_CAPACITY, fileChooser.getSelectedFile());
					break;
				case DENSITY:
					load(DENSITY, fileChooser.getSelectedFile());
					break;
				default:
					throw new IllegalStateException("Unrecognised type: " + dataType);
				}
			} catch (IOException e) {
				getDefaultToolkit().beep();
				showMessageDialog(getWindowAncestor((Component) arg0.getSource()), getString("LoaderButton.ReadError"), //$NON-NLS-1$
						getString("LoaderButton.IOError"), //$NON-NLS-1$
						ERROR_MESSAGE);
				e.printStackTrace();
			}
			var size = getDataset(dataType).getData().size();
			var label = "";
			switch (dataType) {
			case HEAT_CAPACITY:
				label = getString("LoaderButton.5"); //$NON-NLS-1$
				// $NON-NLS-1$
				break;
			case DENSITY:
				label = getString("LoaderButton.6"); //$NON-NLS-1$
				// $NON-NLS-1$
				break;
			default:
				throw new IllegalStateException("Unknown data type: " + dataType);
			}
			showMessageDialog(getWindowAncestor((Component) arg0.getSource()),
					"<html>" + label + " data loaded! A total of " + size + " data points loaded.</html>",
					"Data loaded", INFORMATION_MESSAGE);
		});
	}

	public void setDataType(InterpolationDataset.StandartType dataType) {
		this.dataType = dataType;
	}

	public void highlight(boolean highlighted) {
		setBorder(highlighted ? BorderFactory.createLineBorder(HIGHLIGHTED) : null );
	}
	
	public void highlightIfNeeded() {
		highlight(getDataset(dataType) == null); 
	}

}