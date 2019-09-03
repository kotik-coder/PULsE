package pulse.ui.frames;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pulse.tasks.ResultFormat;
import pulse.ui.charts.PreviewPlot;

public class PlotFrame extends JFrame {

	private static final long serialVersionUID = 8378780367826090756L;	

	private final static int FRAME_WIDTH = 480;
	private final static int FRAME_HEIGHT = 180;
	
	private double data[][][];
	private List<String> propertyNames;
	
	private JComboBox<String> selectXBox, selectYBox;
		
	public PlotFrame(ResultFormat fmt, double[][][] data) {
		super();
		
		this.data = data;
		this.propertyNames = fmt.abbreviations();				
		
		init();							
	}
	
	public void update(ResultFormat fmt, double[][][] data) {
		this.data = data;
		this.propertyNames = fmt.abbreviations();
		selectXBox.removeAllItems();
		
		for(String s : propertyNames)
			selectXBox.addItem(s);
		
		selectXBox.setSelectedIndex(0);
		
		selectYBox.removeAllItems();
		
		for(String s : propertyNames)
			selectYBox.addItem(s);
		
		selectYBox.setSelectedIndex(1);	
	}
	
	private void init() {
		setSize(FRAME_WIDTH, FRAME_HEIGHT);		
		setTitle("Axis Selection for Preview Plot");
		
		JPanel mainPanel = new JPanel();		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(new GridLayout(1,2));
		
		JPanel topPanel = new JPanel();
		mainPanel.add(topPanel);
		topPanel.setLayout(new GridLayout(2,1));
		
		JLabel selectX = new JLabel("Select Horizontal Axis:");
		topPanel.add(selectX);
		
		selectXBox = new JComboBox<String>( propertyNames.toArray(new String[propertyNames.size()]) );
		selectXBox.setSelectedIndex(0);
		topPanel.add(selectXBox);
		
		JPanel bottomPanel = new JPanel();
		mainPanel.add(bottomPanel);
		bottomPanel.setLayout(new GridLayout(2,1));
		
		JLabel selectY = new JLabel("Select Vertical Axis:");
		bottomPanel.add(selectY);
		
		selectYBox = new JComboBox<String>( propertyNames.toArray(new String[propertyNames.size()]) );
		selectYBox.setSelectedIndex(1);
		bottomPanel.add(selectYBox);	
		
		JButton plotButton = new JButton("Plot");
		getContentPane().add(plotButton, BorderLayout.SOUTH);
		
		plotButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selectedX = selectXBox.getSelectedIndex();
				int selectedY = selectYBox.getSelectedIndex();
				
				String selectedXText = (String) selectXBox.getSelectedItem();
				String selectedYText = (String) selectYBox.getSelectedItem();
				
				PreviewPlot.preview(selectedXText, 
							selectedYText, 
							data[selectedX][0], data[selectedY][0],
							data[selectedX][1], data[selectedY][1]);	
				
				dispose();
				
			}
			
		});
		
	}	

}