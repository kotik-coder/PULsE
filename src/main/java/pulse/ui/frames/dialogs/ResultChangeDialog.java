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
import pulse.ui.components.controllers.KeywordListRenderer;
import pulse.ui.components.models.ParameterListModel;
import pulse.ui.components.models.ResultListModel;

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

		var model = (ResultListModel) rightList.getModel();

		moveRightBtn.addActionListener(e -> {

			var key = leftList.getSelectedValue();

			if (key != null)
				if (!model.contains(key))
					model.add(key);

		});

		moveLeftBtn.addActionListener(e -> {

			var key = rightList.getSelectedValue();

			if (key != null)
				model.remove(key);

		});

		commitBtn.addActionListener(e -> generateFormat(model.getData()));
		cancelBtn.addActionListener(e -> this.setVisible(false));

	}

	@Override
	public void setVisible(boolean value) {
		super.setVisible(value);
		((ResultListModel) rightList.getModel()).update();
		((ParameterListModel) leftList.getModel()).update();
	}

	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		MainContainer = new javax.swing.JPanel();
		leftScroller = new javax.swing.JScrollPane();
		leftList = new javax.swing.JList<>();
		rightScroller = new javax.swing.JScrollPane();
		rightList = new javax.swing.JList<>();
		moveToolbar = new javax.swing.JToolBar();
		moveRightBtn = new javax.swing.JButton();
		moveLeftBtn = new javax.swing.JButton();
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

		MainContainer.setPreferredSize(new java.awt.Dimension(650, 400));
		MainContainer.setLayout(new java.awt.GridBagLayout());

		leftScroller.setBorder(createTitledBorder("Available properties"));
		leftList.setModel(new ParameterListModel());
		leftList.setCellRenderer(new KeywordListRenderer());
		leftList.setFixedCellHeight(50);
		leftScroller.setViewportView(leftList);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(8, 5, 8, 5);
		MainContainer.add(leftScroller, gridBagConstraints);

		rightScroller.setBorder(createTitledBorder("Printed output"));

		rightList.setModel(new ResultListModel());
		rightList.setCellRenderer(new KeywordListRenderer());
		rightList.setFixedCellHeight(50);
		rightScroller.setViewportView(rightList);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(9, 5, 9, 5);
		MainContainer.add(rightScroller, gridBagConstraints);

		moveToolbar.setFloatable(false);
		moveToolbar.setOrientation(VERTICAL);
		moveToolbar.setRollover(true);

		moveRightBtn.setText(">>");
		moveRightBtn.setFocusable(false);
		moveRightBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		moveRightBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		moveToolbar.add(moveRightBtn);

		moveLeftBtn.setText("<<");
		moveLeftBtn.setFocusable(false);
		moveLeftBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		moveLeftBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		moveToolbar.add(moveLeftBtn);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		MainContainer.add(moveToolbar, gridBagConstraints);

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
	private javax.swing.JButton moveRightBtn;
	private javax.swing.JButton moveLeftBtn;
	private javax.swing.JToolBar moveToolbar;
	private javax.swing.JList<NumericPropertyKeyword> leftList;
	private javax.swing.JScrollPane leftScroller;
	private javax.swing.JList<NumericPropertyKeyword> rightList;
	private javax.swing.JScrollPane rightScroller;

}