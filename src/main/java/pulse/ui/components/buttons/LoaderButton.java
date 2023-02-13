package pulse.ui.components.buttons;

import static java.awt.Toolkit.getDefaultToolkit;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.io.readers.ReaderManager.getDatasetExtensions;
import static pulse.ui.Messages.getString;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.math3.exception.OutOfRangeException;

import pulse.input.InterpolationDataset;
import pulse.tasks.TaskManager;
import static pulse.ui.components.DataLoader.loadDensity;
import static pulse.ui.components.DataLoader.loadSpecificHeat;
import pulse.util.ImageUtils;

@SuppressWarnings("serial")
public class LoaderButton extends JButton {

    private final DataType dataType;
    private static File dir;

    private final static Color NOT_HIGHLIGHTED = UIManager.getColor("Button.background");
    private final static Color HIGHLIGHTED = ImageUtils.blend(NOT_HIGHLIGHTED, Color.red, 0.35f);

    public LoaderButton(DataType type) {
        super();
        this.dataType = type;
        init();
    }

    public LoaderButton(DataType type, String str) {
        super(str);
        this.dataType = type;
        init();
    }
  
    public final void init() {
        highlight(false);

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
            if (!approve) {
                return;
            }
            try {
                switch (dataType) {
                    case HEAT_CAPACITY:
                        loadSpecificHeat(fileChooser.getSelectedFile());
                        break;
                    case DENSITY:
                        loadDensity(fileChooser.getSelectedFile());
                        break;
                    default:
                        throw new IllegalStateException("Unrecognised type: " + dataType);
                }
            } catch (IOException e) {
                getDefaultToolkit().beep();
                showMessageDialog(getWindowAncestor((Component) arg0.getSource()), getString("LoaderButton.ReadError"), //$NON-NLS-1$
                        getString("LoaderButton.IOError"), //$NON-NLS-1$
                        ERROR_MESSAGE);
            } catch (OutOfRangeException ofre) {
                getDefaultToolkit().beep();
                StringBuilder sb = new StringBuilder(getString("TextWrap.0"));
                sb.append(getString("LoaderButton.OFRErrorDescriptor"));
                sb.append(ofre.getMessage());
                sb.append(getString("LoaderButton.OFRErrorDescriptor2"));
                sb.append(getString("TextWrap.1"));
                showMessageDialog(getWindowAncestor((Component) arg0.getSource()),
                        sb.toString(),
                        getString("LoaderButton.OFRError"), //$NON-NLS-1$
                        ERROR_MESSAGE);
            }
            int size = getDataset().getData().size();
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
            StringBuilder sb = new StringBuilder("<html>");
            sb.append(label).append(" data loaded! A total of ").append(size).append(" data points loaded.</html>");
            showMessageDialog(getWindowAncestor((Component) arg0.getSource()),
                    sb.toString(),
                    "Data loaded", INFORMATION_MESSAGE);
            highlight(false);
        });
    }
    
    public InterpolationDataset getDataset() {
        var i = TaskManager.getManagerInstance();
        return dataType == DataType.HEAT_CAPACITY ? 
                i.getSpecificHeatDataset() : i.getDensityDataset();
    }
    
    public void highlight(boolean highlighted) {
        setBackground(highlighted ? HIGHLIGHTED : NOT_HIGHLIGHTED);
    }

    public void highlightIfNeeded() {
        highlight(getDataset() == null);
    }
    
    public enum DataType {
        HEAT_CAPACITY, DENSITY;
    }

}