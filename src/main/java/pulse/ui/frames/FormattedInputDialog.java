package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import pulse.properties.NumericProperty;
import pulse.ui.Messages;
import pulse.ui.components.controllers.ConfirmAction;

@SuppressWarnings("serial")
public class FormattedInputDialog extends JDialog {
	
	private final static int FONT_SIZE = 14;
	private final static int WIDTH = 550;
	private final static int HEIGHT = 130;
	private JFormattedTextField ftf;
	private ConfirmAction confirmAction;
	private NumberFormatter numFormatter;
	
	public FormattedInputDialog(NumericProperty p) {
		this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		this.setMinimumSize(new Dimension(WIDTH, HEIGHT));		
		setLocationRelativeTo(null);
		
		setTitle("Choose " + p.getAbbreviation(false));
		
		JPanel northPanel = new JPanel();
		
		northPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));		
		
		northPanel.setLayout(new GridLayout());
		
		northPanel.add(new JLabel(p.getDescriptor(true)));
		northPanel.add(new JSeparator());
		northPanel.add(ftf = initFormattedTextField(p));
		add(northPanel, BorderLayout.CENTER);
		//
	    JPanel btnPanel = new JPanel();
	    JButton okBtn = new JButton("OK");
	    JButton cancelBtn = new JButton("Cancel");
	    btnPanel.add(okBtn);
	    btnPanel.add(cancelBtn);
	    add(btnPanel, BorderLayout.SOUTH);
	    //
	    cancelBtn.addActionListener(event -> 
	    {
	    	close();
	    });
	    okBtn.addActionListener(event -> 
	    	{	    		
	    		if (!ftf.isEditValid()) { //The text is invalid.
                    if (userSaysRevert(ftf, numFormatter, p))  //reverted
                    	ftf.postActionEvent(); //inform the editor
	    		}
                else {
                	try {
						ftf.commitEdit();
					} catch (ParseException e) {
						System.out.println("Could not parse merge value");
						e.printStackTrace();
					}
			    	confirmAction.onConfirm();
			    	close();
                }
	    	});
	}
	
	private void close() {
		this.setVisible(false);
	}
	
	public Number value() {
		return (Number) ftf.getValue();
	}
	
	private JFormattedTextField initFormattedTextField(NumericProperty p) {
		JFormattedTextField inputTextField = new JFormattedTextField(p.getValue());
		
		 //Set up the editor for the integer cells.
               
        NumberFormat numberFormat = p.numberFormat(true);
        numFormatter = new NumberFormatter(numberFormat);
        
        numFormatter.setMinimum(p.getMinimum().doubleValue());
	    numFormatter.setMaximum(p.getMaximum().doubleValue());
        
        numFormatter.setFormat(numberFormat);		        
 
        inputTextField.setFormatterFactory(
                new DefaultFormatterFactory(numFormatter));
        inputTextField.setHorizontalAlignment(SwingConstants.CENTER);
        inputTextField.setFocusLostBehavior(JFormattedTextField.PERSIST);        
        
        //React when the user presses Enter while the editor is
        //active.  (Tab is handled as specified by
        //JFormattedTextField's focusLostBehavior property.)
        inputTextField.getInputMap().put(KeyStroke.getKeyStroke(
                                        KeyEvent.VK_ENTER, 0),
                                        "check"); //$NON-NLS-1$
        inputTextField.getActionMap().put("check", new AbstractAction() { //$NON-NLS-1$

		@Override
		public void actionPerformed(ActionEvent e) {
        if (!inputTextField.isEditValid()) { //The text is invalid.
                    if (userSaysRevert(inputTextField, numFormatter, p)) { //reverted
                    	inputTextField.postActionEvent(); //inform the editor
            }
                } else try {              //The text is valid,
                	inputTextField.commitEdit();     //so use it.
                	inputTextField.postActionEvent(); //stop editing
                } catch (java.text.ParseException exc) { }
            }
        });		
		
		inputTextField.setFont(inputTextField.getFont().deriveFont(FONT_SIZE));
		inputTextField.setColumns(10);
		return inputTextField;
	}
	
	public void setConfirmAction(ConfirmAction confirmAction) {
		this.confirmAction = confirmAction;
	}
	
	public ConfirmAction getConfirmAction() {
		return confirmAction;
	}

    private static boolean userSaysRevert(JFormattedTextField inputTextField, NumberFormatter numFormatter, NumericProperty p) {
        Toolkit.getDefaultToolkit().beep();
        inputTextField.selectAll();
        Object[] options = {Messages.getString("SimpleInputFrame.Edit"), //$NON-NLS-1$
                            Messages.getString("SimpleInputFrame.Revert")}; //$NON-NLS-1$
        int answer = JOptionPane.showOptionDialog(
            SwingUtilities.getWindowAncestor(inputTextField),
            "The value must be a " + p.getValue().getClass().getSimpleName() + " between " //$NON-NLS-1$
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