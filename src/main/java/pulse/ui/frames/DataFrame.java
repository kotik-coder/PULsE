package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.Color.BLACK;
import static java.awt.Window.Type.UTILITY;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pulse.ui.components.PropertyHolderTable;
import pulse.util.PropertyHolder;

public class DataFrame extends JFrame {

    private JPanel contentPane;
    private PropertyHolderTable dataTable;
    private Component ancestorFrame;
    private PropertyHolder dataObject;
    private final static int ROW_HEIGHT = 70;

    @Override
    public void dispose() {
        if (ancestorFrame != null) {
            ancestorFrame.setEnabled(true);
            if (ancestorFrame.getParent() != null) {
                ancestorFrame.getParent().setEnabled(true);
            }
        }
        super.dispose();
    }

    /**
     * Create the frame.
     */
    public DataFrame(PropertyHolder dataObject, Component ancestor) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(ancestor);
        this.ancestorFrame = ancestor.getParent();
        this.dataObject = dataObject;
        if (ancestor != null) {
            ancestor.setEnabled(false);
            if (ancestorFrame != null) {
                ancestorFrame.setEnabled(false);
            }
        }
        setType(UTILITY);
        setResizable(false);
        setAlwaysOnTop(true);
        setTitle(dataObject.getClass().getSimpleName() + " properties");

        contentPane = new JPanel();
        contentPane.setForeground(BLACK);
        contentPane.setBorder(null);
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);
        var scrollPane = new JScrollPane();
        contentPane.add(scrollPane, CENTER);

        dataTable = new PropertyHolderTable(dataObject);
        dataTable.setRowHeight(ROW_HEIGHT);

        setBounds(100, 100, 600, 450);

        scrollPane.setViewportView(dataTable);
    }

    public PropertyHolder getDataObject() {
        return dataObject;
    }

}
