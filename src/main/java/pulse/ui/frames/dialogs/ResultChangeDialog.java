package pulse.ui.frames.dialogs;

import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingConstants.BOTTOM;
import static javax.swing.SwingConstants.VERTICAL;
import static pulse.tasks.processing.ResultFormat.generateFormat;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

import javax.swing.JDialog;
import javax.swing.SwingConstants;

import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.processing.ResultFormat;
import pulse.ui.components.controllers.KeywordListRenderer;
import pulse.ui.components.models.ParameterListModel;
import pulse.ui.components.models.ActiveFlagsListModel;
import pulse.ui.components.panels.DoubleListPanel;

public class ResultChangeDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static int WIDTH = 500;
	private final static int HEIGHT = 500;

	public ResultChangeDialog() {

		setTitle("Result output formatting");

		setSize(WIDTH, HEIGHT);

		initComponents();
                var model = (ActiveFlagsListModel)rightList.getModel();
		commitBtn.addActionListener(e -> generateFormat(model.getData()));
		cancelBtn.addActionListener(e -> this.setVisible(false));
	}

	@Override
	public void setVisible(boolean value) {
		super.setVisible(value);
		((ActiveFlagsListModel) rightList.getModel()).update();
		((ParameterListModel) leftList.getModel()).update();
	}

	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		MainToolbar = new javax.swing.JToolBar();
		filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0),
				new java.awt.Dimension(32767, 0));
		cancelBtn = new javax.swing.JButton();
		filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(25, 0), new java.awt.Dimension(25, 0),
				new java.awt.Dimension(25, 32767));
		commitBtn = new javax.swing.JButton();
		filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0),
				new java.awt.Dimension(32767, 0));

		setDefaultCloseOperation(HIDE_ON_CLOSE);

                leftList = new javax.swing.JList<>();
                leftList.setModel(new ParameterListModel(true));
		leftList.setCellRenderer(new KeywordListRenderer());
		
                rightList = new javax.swing.JList<>();
		rightList.setModel(new ActiveFlagsListModel(
                        ResultFormat.getInstance().getKeywords(), 
                        ResultFormat.getMinimalArray()));
		rightList.setCellRenderer(new KeywordListRenderer());
		
                MainContainer = new DoubleListPanel
                        (leftList, "All Parameters", 
                        rightList, "Output");		

		getContentPane().add(MainContainer, BorderLayout.CENTER);

		MainToolbar.setFloatable(false);
		MainToolbar.setRollover(true);
		MainToolbar.add(filler1);

		cancelBtn.setText("Cancel");
		cancelBtn.setFocusable(false);
		cancelBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		cancelBtn.setVerticalTextPosition(BOTTOM);
		MainToolbar.add(cancelBtn);
		MainToolbar.add(filler3);

		commitBtn.setText("Commit");
		commitBtn.setFocusable(false);
		commitBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		commitBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		MainToolbar.add(commitBtn);
		MainToolbar.add(filler2);

		getContentPane().add(MainToolbar, BorderLayout.SOUTH);

		pack();
	}

	private javax.swing.JPanel MainContainer;
	private javax.swing.JToolBar MainToolbar;
	private javax.swing.JButton cancelBtn;
	private javax.swing.JButton commitBtn;
	private javax.swing.Box.Filler filler1;
	private javax.swing.Box.Filler filler2;
	private javax.swing.Box.Filler filler3;
	private javax.swing.JList<NumericPropertyKeyword> leftList;
	private javax.swing.JList<NumericPropertyKeyword> rightList;

}