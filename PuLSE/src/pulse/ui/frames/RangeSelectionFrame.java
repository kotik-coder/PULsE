package pulse.ui.frames;

import javax.swing.JFrame;
import java.awt.Font;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import pulse.input.ExperimentalData;
import pulse.tasks.TaskManager;
import pulse.ui.charts.PreviewPlot;
import pulse.ui.Messages;
import pulse.ui.charts.Chart;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class RangeSelectionFrame extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8888698379087886325L;
	private JTextField maxRangeTextField;
	private JTextField minRangeTextField;
	
	private final static Font LABEL_FONT 		= new Font(Messages.getString("RangeSelectionFrame.LabelFont"), Font.PLAIN, 18);
	private final static Font BUTTON_FONT		= new Font(Messages.getString("RangeSelectionFrame.ButtonFont"), Font.BOLD, 14);
	private final static Font BIG_LABEL_FONT	= new Font(Messages.getString("RangeSelectionFrame.BigLabelFont"), Font.BOLD, 20);
	private final static Font TEXT_FIELD_FONT	= new Font(Messages.getString("RangeSelectionFrame.TextFieldFont"), Font.PLAIN, 26);
	
	public RangeSelectionFrame(Chart chartPanel) {
		this.setSize(new Dimension(400, 200));
		
		setUndecorated(true);
		
		getRootPane().setBorder(BorderFactory.createMatteBorder(4, 4, 4, 4, Color.RED));
		
		setType(Type.UTILITY);
		this.setAlwaysOnTop(true);
		setResizable(false);
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		JLabel lblRangeSelected = new JLabel(Messages.getString("RangeSelectionFrame.ConfirmSelection")); //$NON-NLS-1$
		lblRangeSelected.setFont(LABEL_FONT); //$NON-NLS-1$
		lblRangeSelected.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(lblRangeSelected, BorderLayout.NORTH);
		
		JPanel btnNewButton = new JPanel();
		getContentPane().add(btnNewButton, BorderLayout.SOUTH);
		btnNewButton.setLayout(new GridLayout(0, 2, 0, 0));
		
		JButton btnNewButton_1 = new JButton(Messages.getString("RangeSelectionFrame.Apply")); //$NON-NLS-1$
		
		RangeSelectionFrame reference = this;
		
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ExperimentalData dat = TaskManager.getSelectedTask().getExperimentalCurve();
				
				int factor = chartPanel.getTimeAxisSpecs().getFactor();
				
				double a = Double.parseDouble(minRangeTextField.getText())/factor;
				double b = Double.parseDouble(maxRangeTextField.getText())/factor;
				
				StringBuilder sb = new StringBuilder();
				
				sb.append("<html><p>");
				sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage1"));
				sb.append("</p><br>");
				sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage2"));
				sb.append(String.format("%3.4f", dat.timeAt(dat.getFittingStartIndex())));
				sb.append(" to ");
				sb.append(String.format("%3.4f", dat.timeAt(dat.getFittingEndIndex())));
				sb.append("<br><br>");
				sb.append(Messages.getString("RangeSelectionFrame.ConfirmationMessage3"));
				sb.append(String.format("%3.4f", a) + " to " + String.format("%3.4f", b));
				sb.append("</html>");
				
				int dialogResult = JOptionPane.showConfirmDialog 
						(reference, 
								sb.toString(), 
								"Confirm chocie", JOptionPane.YES_NO_OPTION);

				if(dialogResult != JOptionPane.YES_OPTION)
					return;
				
				dat.setFittingRange(a, b);
				reference.setVisible(false);
			}
		});
		
		btnNewButton_1.setFont(BUTTON_FONT); //$NON-NLS-1$
		btnNewButton.add(btnNewButton_1);
		
		JButton btnNewButton_2 = new JButton(Messages.getString("RangeSelectionFrame.Cancel")); //$NON-NLS-1$
		
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chartPanel.undoHighlight();
				reference.setVisible(false);
			}
		});
		
		btnNewButton_2.setFont(BUTTON_FONT); //$NON-NLS-1$
		btnNewButton.add(btnNewButton_2);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblNewLabel = new JLabel(Messages.getString("RangeSelectionFrame.TimeLimitMin")); //$NON-NLS-1$
		lblNewLabel.setForeground(Color.BLUE);
		lblNewLabel.setFont(BIG_LABEL_FONT); //$NON-NLS-1$
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lblNewLabel);
		
		JLabel lblNewLabel_1 = new JLabel(Messages.getString("RangeSelectionFrame.TimeLimitMax")); //$NON-NLS-1$
		lblNewLabel_1.setForeground(Color.BLUE);
		lblNewLabel_1.setFont(BIG_LABEL_FONT); //$NON-NLS-1$
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lblNewLabel_1);
		
		minRangeTextField = new JTextField();
		minRangeTextField.setForeground(Color.RED);
		minRangeTextField.setFont(TEXT_FIELD_FONT); //$NON-NLS-1$
		minRangeTextField.setHorizontalAlignment(SwingConstants.CENTER);
		minRangeTextField.setEditable(false);
		panel.add(minRangeTextField);
		minRangeTextField.setColumns(10);
		
		maxRangeTextField = new JTextField();
		maxRangeTextField.setForeground(Color.RED);
		maxRangeTextField.setFont(TEXT_FIELD_FONT); //$NON-NLS-1$
		maxRangeTextField.setHorizontalAlignment(SwingConstants.CENTER);
		maxRangeTextField.setEditable(false);
		panel.add(maxRangeTextField);
		maxRangeTextField.setColumns(10);
		setTitle(Messages.getString("RangeSelectionFrame.RangeSelectorTitle")); //$NON-NLS-1$
	}
	
	public void setRangeFields(double min, double max) {
		minRangeTextField.setText(String.format(Messages.getString("RangeSelectionFrame.NumberFormat"), min)); //$NON-NLS-1$
		maxRangeTextField.setText(String.format("%3.3f", max)); //$NON-NLS-1$
	}

}
