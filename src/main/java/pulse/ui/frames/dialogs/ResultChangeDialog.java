package pulse.ui.frames.dialogs;

import static javax.swing.SwingConstants.BOTTOM;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import static javax.swing.SwingConstants.SOUTH;
import static javax.swing.SwingConstants.TOP;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import pulse.tasks.processing.ResultFormat;
import pulse.ui.Messages;
import pulse.ui.components.models.ParameterTableModel;
import pulse.ui.components.models.SelectedKeysModel;
import pulse.ui.components.panels.DoubleTablePanel;

public class ResultChangeDialog extends JDialog {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final static int WIDTH = 1000;
    private final static int HEIGHT = 600;

    public ResultChangeDialog() {

        setTitle("Result output formatting");
        initComponents();
        setSize(WIDTH, HEIGHT);
        var model = (SelectedKeysModel) rightTbl.getModel();
        commitBtn.addActionListener(e -> ResultFormat.generateFormat(model.getData()));
        cancelBtn.addActionListener(e -> this.setVisible(false));
    }

    @Override
    public void setVisible(boolean value) {
        super.setVisible(value);
        ((SelectedKeysModel) rightTbl.getModel()).update();
        ((ParameterTableModel) leftTbl.getModel()).populateWithAllProperties();
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

        leftTbl = new javax.swing.JTable() {
            
            @Override
            public boolean isCellEditable(int row, int column) {                
                    return false;               
            };
     
        };        
        
        leftTbl.setModel(new ParameterTableModel(true));
        leftTbl.setTableHeader(null);

        rightTbl = new javax.swing.JTable() {
            
            @Override
            public boolean isCellEditable(int row, int column) {                
                    return false;               
            };
     
        };
        
        rightTbl.setModel(new SelectedKeysModel(
                ResultFormat.getInstance().getKeywords(),
                ResultFormat.getMinimalArray()));
        rightTbl.setTableHeader(null);
        
        MainContainer = new DoubleTablePanel(leftTbl, "All Parameters",
                rightTbl, "Output");
                
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
    private javax.swing.JTable leftTbl;
    private javax.swing.JTable rightTbl;

}
