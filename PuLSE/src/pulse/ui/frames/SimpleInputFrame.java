package pulse.ui.frames;

import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.awt.Font;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.SwingUtilities;

import javax.swing.AbstractAction;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import pulse.ui.Messages;
import pulse.ui.components.ResultTable;

import java.awt.event.ActionListener;

public class SimpleInputFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1877579145033684459L;
	private JFormattedTextField inputTextField;
	private NumberFormatter numFormatter;
	
	private final static int WIDTH = 450;
	private final static int HEIGHT = 130;
	
	private final static Font FONT = new Font(Messages.getString("SimpleInputFrame.Font"), Font.PLAIN, 16);
	
	public SimpleInputFrame(ResultTable linkedTable) {
		setType(Type.UTILITY);
		setTitle("Input required"); //$NON-NLS-1$
		
		this.setSize(WIDTH, HEIGHT);
		
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		JTextArea infoText = new JTextArea();
		infoText.setEditable(false);
		infoText.setFont(FONT); //$NON-NLS-1$
		infoText.setBackground(UIManager.getColor(Messages.getString("SimpleInputFrame.Color"))); //$NON-NLS-1$
		infoText.setLineWrap(true);
		infoText.setWrapStyleWord(true);
		infoText.setText(Messages.getString("SimpleInputFrame.GroupMessage")); //$NON-NLS-1$
		panel.add(infoText);
		
		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new GridLayout(0, 3, 0, 0));
		
		inputTextField = new JFormattedTextField(1.0);
		
		 //Set up the editor for the integer cells.
               
        NumberFormat numberFormat = new DecimalFormat(Messages.getString("SimpleInputFrame.SimpleNumberFormat")); //$NON-NLS-1$
        numFormatter = new NumberFormatter(numberFormat);

        numFormatter.setMinimum(0.01);
	    numFormatter.setMaximum(50.0);
        
        numFormatter.setFormat(numberFormat);		        
 
        inputTextField.setFormatterFactory(
                new DefaultFormatterFactory(numFormatter));
        inputTextField.setHorizontalAlignment(JTextField.CENTER);
        inputTextField.setFocusLostBehavior(JFormattedTextField.PERSIST);
 
        //React when the user presses Enter while the editor is
        //active.  (Tab is handled as specified by
        //JFormattedTextField's focusLostBehavior property.)
        inputTextField.getInputMap().put(KeyStroke.getKeyStroke(
                                        KeyEvent.VK_ENTER, 0),
                                        "check"); //$NON-NLS-1$
        inputTextField.getActionMap().put("check", new AbstractAction() { //$NON-NLS-1$
            /**
			 * 
			 */
			private static final long serialVersionUID = 3977051175084065931L;

			public void actionPerformed(ActionEvent e) {
        if (!inputTextField.isEditValid()) { //The text is invalid.
                    if (userSaysRevert()) { //reverted
                    	inputTextField.postActionEvent(); //inform the editor
            }
                } else try {              //The text is valid,
                	inputTextField.commitEdit();     //so use it.
                	inputTextField.postActionEvent(); //stop editing
                } catch (java.text.ParseException exc) { }
            }
        });
		
		
		inputTextField.setFont(FONT); //$NON-NLS-1$
		panel_1.add(inputTextField);
		inputTextField.setColumns(10);
		
		JButton groupButton = new JButton(Messages.getString("SimpleInputFrame.GroupButton")); //$NON-NLS-1$
		groupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				linkedTable.merge(Double.parseDouble(inputTextField.getText()) );
			}
		});
		groupButton.setFont(FONT); //$NON-NLS-1$
		panel_1.add(groupButton);
		
		JButton doNothingButton = new JButton(Messages.getString("SimpleInputFrame.DoNothing")); //$NON-NLS-1$
		doNothingButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		doNothingButton.setFont(FONT); //$NON-NLS-1$
		panel_1.add(doNothingButton);
	}

    protected boolean userSaysRevert() {
        Toolkit.getDefaultToolkit().beep();
        inputTextField.selectAll();
        Object[] options = {Messages.getString("SimpleInputFrame.Edit"), //$NON-NLS-1$
                            Messages.getString("SimpleInputFrame.Revert")}; //$NON-NLS-1$
        int answer = JOptionPane.showOptionDialog(
            SwingUtilities.getWindowAncestor(inputTextField),
            Messages.getString("SimpleInputFrame.IntegerMessage") //$NON-NLS-1$
            + numFormatter.getMinimum() + " and " //$NON-NLS-1$
            + numFormatter.getMaximum() + ".\n" //$NON-NLS-1$
            + Messages.getString("SimpleInputFrame.MessageLine1") //$NON-NLS-1$
            + Messages.getString("SimpleInputFrame.MessageLine2"), //$NON-NLS-1$
            "Invalid Text Entered", //$NON-NLS-1$
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[1]);
         
        if (answer == 1) { //Revert!
            inputTextField.setValue(inputTextField.getValue());
        return true;
        }
    return false;
    }	
	
}
