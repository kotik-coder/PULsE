/*
 * Copyright 2021 kotik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pulse.ui.components.panels;

import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.BorderFactory.createTitledBorder;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JTable;
import pulse.properties.NumericProperties;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.components.models.ParameterTableModel;
import pulse.ui.components.models.SelectedKeysModel;

public class DoubleTablePanel extends JPanel {

    private javax.swing.JButton moveLeftBtn;
    private javax.swing.JButton moveRightBtn;

    public DoubleTablePanel(JTable leftTable, String titleLeft, JTable rightTable, String titleRight) {

        super();
        initComponents(leftTable, titleLeft, rightTable, titleRight);
        
        moveRightBtn.addActionListener(e -> {

            var model = (SelectedKeysModel) rightTable.getModel();
            NumericPropertyKeyword key = ( (ParameterTableModel) leftTable.getModel() )
                                            .getElementAt(leftTable
                                                    .convertRowIndexToModel(leftTable.getSelectedRow()));

            if (key != null) {
                if (!model.contains(key)) {
                    model.addElement((NumericPropertyKeyword) key);
                    var excluded = NumericProperties.def(
                            (NumericPropertyKeyword) key)
                            .getExcludeKeywords();

                    for (var aKey : excluded) {
                        if (model.contains(aKey)) {
                            model.removeElement(aKey);
                        }
                    }

                }
            }

        });

        moveLeftBtn.addActionListener(e -> {

            var model = (SelectedKeysModel) rightTable.getModel();
            NumericPropertyKeyword key = model.getElementAt(rightTable
                    .convertRowIndexToModel(rightTable.getSelectedRow()));

            if (key != null) {
                model.removeElement(key);
            }

        });
        
    }

    public void initComponents(JTable leftTable, String titleLeft, JTable rightTable, String titleRight) {
        var leftScroller = new javax.swing.JScrollPane();
        var rightScroller = new javax.swing.JScrollPane();
        var moveToolbar = new javax.swing.JToolBar();
        moveRightBtn = new javax.swing.JButton();
        moveLeftBtn = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(650, 400));
        setLayout(new java.awt.GridBagLayout());

        var borderLeft = createTitledBorder(titleLeft);
        leftScroller.setBorder(borderLeft);
        borderLeft.setTitleColor(java.awt.Color.WHITE);
        
        leftTable.setRowHeight(80);

        leftScroller.setViewportView(leftTable);

        var gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 5, 8, 5);
        add(leftScroller, gridBagConstraints);

        var borderRight = createTitledBorder(titleRight);
        rightScroller.setBorder(borderRight);
        borderRight.setTitleColor(java.awt.Color.WHITE);

        rightTable.setRowHeight(80);
        rightScroller.setViewportView(rightTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(9, 5, 9, 5);
        add(rightScroller, gridBagConstraints);

        moveToolbar.setFloatable(false);
        moveToolbar.setOrientation(SwingConstants.HORIZONTAL);
        moveToolbar.setRollover(true);

        moveRightBtn.setText("\u25BA");
        moveRightBtn.setFocusable(false);
        moveRightBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        moveRightBtn.setVerticalTextPosition(SwingConstants.BOTTOM);

        moveLeftBtn.setText("\u25C4");
        moveLeftBtn.setFocusable(false);
        moveLeftBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        moveLeftBtn.setVerticalTextPosition(SwingConstants.BOTTOM);

        moveToolbar.add(moveLeftBtn);
        moveToolbar.add(moveRightBtn);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        add(moveToolbar, gridBagConstraints);
    }

}
