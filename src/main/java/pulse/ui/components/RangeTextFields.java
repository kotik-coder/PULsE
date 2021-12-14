/*
 * Copyright 2021 Artem Lunev <artem.v.lunev@gmail.com>.
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
package pulse.ui.components;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;
import pulse.input.Range;
import pulse.input.listeners.DataEvent;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.ui.components.panels.ChartToolbar;

/**
 * Two JFormattedTextFields used to display the range of the currently
 * selected task.
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public final class RangeTextFields {

    private JFormattedTextField lowerLimitField;
    private JFormattedTextField upperLimitField;

    /**
     * Creates textfield objects, which may be accessed with getters from this instance.
     * Additionally, binds listeners to all current and future tasks in order to observe
     * and reflect upon the changes with the textfield.
     */
    
    public RangeTextFields() {
        initTextFields();

        var instance = TaskManager.getManagerInstance();

        //for each new task created in the repo
        instance.addTaskRepositoryListener((TaskRepositoryEvent e) -> {
            if (e.getState() == TaskRepositoryEvent.State.TASK_ADDED) {

                var newTask = instance.getTask(e.getId());
                //when the range of the selected data is changed and the task is the selected one,
                //update the textfields values
                updateTextfieldsFromTask(newTask);

            }
        });

        //when a new task is selected
        instance.addSelectionListener((TaskSelectionEvent e) -> {
            var task = instance.getSelectedTask();
            var segment = task.getExperimentalCurve().getRange().getSegment();
            //update the textfield values
            lowerLimitField.setValue(segment.getMinimum());
            upperLimitField.setValue(segment.getMaximum());
        });

    }
    
    /*
    Creates a formatter for the textfields
    */

    private NumberFormatter initFormatter() {
        var format = new DecimalFormat();
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        format.setMinimumIntegerDigits(1);
        format.setMaximumIntegerDigits(6);
        format.setMultiplier(1000); //ms to seconds
        format.setPositiveSuffix(" ms");
        format.setGroupingUsed(false);

        /*
         * A custom formatter for the time range 
         */
        var formatter = new NumberFormatter(format);
        formatter.setAllowsInvalid(false);
        formatter.setOverwriteMode(true);
        return formatter;
    }
    
    /**
     * Checks if the candidate value produced by the formatter is sensible, i.e.
     * if it lies within the bounds defined in the Range class. 
     * @param jtf the textfield containing the candidate value as text
     * @param upperBound whether the upper bound is checked ({@code false} if the lower bound is checked)
     * @return {@code true} if the edit may proceed
     */

    private static boolean isEditValid(JFormattedTextField jtf, boolean upperBound) {
        Range range = TaskManager.getManagerInstance().getSelectedTask()
                .getExperimentalCurve().getRange();

        double candidateValue = 0.0;
        try {
            candidateValue = ((Number) jtf.getFormatter().stringToValue(jtf.getText())).doubleValue();
        } catch (ParseException ex) {
            Logger.getLogger(ChartToolbar.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean b = range.boundLimits(upperBound).contains(candidateValue);

        return range.boundLimits(upperBound).contains(candidateValue);
    }
    
    /**
     * Creates a formatter and initialised the textfields, setting up rules
     * for edit validation. 
     */

    private void initTextFields() {
        var instance = TaskManager.getManagerInstance();

        var formatter = initFormatter();

        lowerLimitField = new JFormattedTextField(formatter) {

            @Override
            public boolean isEditValid() {
                return super.isEditValid()
                        ? RangeTextFields.isEditValid(this, false)
                        : false;
            }

            @Override
            public void commitEdit() throws ParseException {
                if (isEditValid()) {
                    super.commitEdit();
                }
            }

        };

        upperLimitField = new JFormattedTextField(formatter) {

            @Override
            public boolean isEditValid() {
                return super.isEditValid()
                        ? RangeTextFields.isEditValid(this, true)
                        : false;
            }

            @Override
            public void commitEdit() throws ParseException {
                if (isEditValid()) {
                    super.commitEdit();
                }
            }

        };
        
        var fl = new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
                arg0.getComponent().setForeground(Color.WHITE);
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                arg0.getComponent().setForeground(Color.lightGray);
            }

        };

        lowerLimitField.addFocusListener(fl);
        upperLimitField.addFocusListener(fl);

        lowerLimitField.setColumns(10);
        upperLimitField.setColumns(10);

        lowerLimitField.setForeground(Color.lightGray);
        upperLimitField.setForeground(Color.lightGray);

    }

    private void updateTextfieldsFromTask(SearchTask newTask) {
        //add data listeners in case when the range of the selected task is changed
        newTask.getExperimentalCurve().addDataListener((DataEvent e1) -> {
            if (TaskManager.getManagerInstance().getSelectedTask() == newTask) {
                var segment = newTask.getExperimentalCurve().getRange().getSegment();
                lowerLimitField.setValue(segment.getMinimum());
                upperLimitField.setValue(segment.getMaximum());
            }
        });
    }
    
    public JFormattedTextField getLowerLimitField() {
        return lowerLimitField;
    }
    
    public JFormattedTextField getUpperLimitField() {
        return upperLimitField;
    }

}
