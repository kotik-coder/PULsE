package pulse.ui.components.controllers;

import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.text.NumberFormat.getIntegerInstance;
import static javax.swing.JFormattedTextField.PERSIST;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.JOptionPane.showOptionDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.ui.Messages.getString;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import pulse.properties.NumericProperty;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/

public class NumberEditor extends DefaultCellEditor {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;
	JFormattedTextField ftf;
	NumberFormat numberFormat;
	private boolean DEBUG = false;
	private NumericProperty property;

	public NumberEditor(NumericProperty property) {
		super(new JFormattedTextField());

		this.property = property;
		ftf = (JFormattedTextField) getComponent();

		// Set up the editor for the integer cells.

		var numFormatter = new NumberFormatter(numberFormat);
		numFormatter.setFormat(numberFormat);

		Number value;

		if (property.getValue() instanceof Integer) {
			numberFormat = getIntegerInstance();
			value = (int) property.getValue() * (int) property.getDimensionFactor();
			numFormatter.setMinimum(property.getMinimum().intValue() * property.getDimensionFactor().intValue());
			numFormatter.setMaximum(property.getMaximum().intValue() * property.getDimensionFactor().intValue());
		} else {
			numberFormat = new DecimalFormat(getString("NumberEditor.NumberFormat")); //$NON-NLS-1$
			value = ((Number) property.getValue()).doubleValue() * property.getDimensionFactor().doubleValue();
			numFormatter.setMinimum(property.getMinimum().doubleValue() * property.getDimensionFactor().doubleValue());
			numFormatter.setMaximum(property.getMaximum().doubleValue() * property.getDimensionFactor().doubleValue());
		}

		ftf.setFormatterFactory(new DefaultFormatterFactory(numFormatter));
		ftf.setValue(value);
		ftf.setHorizontalAlignment(CENTER);
		ftf.setFocusLostBehavior(PERSIST);

		// React when the user presses Enter while the editor is
		// active. (Tab is handled as specified by
		// JFormattedTextField's focusLostBehavior property.)
		ftf.getInputMap().put(getKeyStroke(VK_ENTER, 0), "check");
		ftf.getActionMap().put("check", new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ftf.isEditValid()) { // The text is invalid.
					if (userSaysRevert()) { // reverted
						ftf.postActionEvent(); // inform the editor
					}
				} else
					try { // The text is valid,
						ftf.commitEdit(); // so use it.
						ftf.postActionEvent(); // stop editing
					} catch (java.text.ParseException exc) {
					}
			}
		});
	}

	// Override to invoke setValue on the formatted text field.
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		var ftf = (JFormattedTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);

		Number num;
		var prop = (NumericProperty) value;

		if ((prop.getValue() instanceof Integer))
			num = (int) prop.getValue() * (int) prop.getDimensionFactor();
		else
			num = ((Number) prop.getValue()).doubleValue() * prop.getDimensionFactor().doubleValue();

		ftf.setValue(num);
		return ftf;
	}

	@Override
	public Object getCellEditorValue() {
		var ftf = (JFormattedTextField) getComponent();
		var o = ftf.getValue();
		if (o instanceof Number) {

			try {
				if (o instanceof Integer) {
					if (property.getValue() instanceof Integer)
						property.setValue((int) o / (int) property.getDimensionFactor());
					else
						property.setValue(((Number) o).doubleValue() / (double) (property.getDimensionFactor()));
				} else {
					if (property.getValue() instanceof Integer)
						property.setValue(((Number) o).intValue() / (int) property.getDimensionFactor());
					else
						property.setValue(((Number) o).doubleValue() / (double) (property.getDimensionFactor()));
				}
			} catch (IllegalArgumentException e) {
				getDefaultToolkit().beep();
				showMessageDialog(getWindowAncestor(ftf), e.getMessage(), getString("NumberEditor.IllegalTableEntry"), //$NON-NLS-1$
						ERROR_MESSAGE);
				property.setValue(property.getMinimum());
			}
			return property;
		} else {
			if (DEBUG) {
				out.println(getString("NumberEditor.NotANumberError")); //$NON-NLS-1$
			}
			try {
				return numberFormat.parseObject(o.toString());
			} catch (ParseException exc) {
				err.println(getString("NumberEditor.ParseError") + o); //$NON-NLS-1$
				return null;
			}
		}
	}

	// Override to check whether the edit is valid,
	// setting the value if it is and complaining if
	// it isn't. If it's OK for the editor to go
	// away, we need to invoke the superclass's version
	// of this method so that everything gets cleaned up.
	@Override
	public boolean stopCellEditing() {
		var ftf = (JFormattedTextField) getComponent();
		if (ftf.isEditValid()) {
			try {
				ftf.commitEdit();
			} catch (java.text.ParseException exc) {
			}

		} else { // text is invalid
			if (!userSaysRevert()) { // user wants to edit
				return false; // don't let the editor go away
			}
		}

		return super.stopCellEditing();
	}

	/**
	 * Lets the user know that the text they entered is bad. Returns true if the
	 * user elects to revert to the last good value. Otherwise, returns false,
	 * indicating that the user wants to continue editing.
	 */
	protected boolean userSaysRevert() {
		getDefaultToolkit().beep();
		ftf.selectAll();
		Object[] options = { getString("NumberEditor.EditText"), getString("NumberEditor.RevertText") };
		var answer = showOptionDialog(getWindowAncestor(ftf),
				"The value must be a " + property.getMinimum().getClass().getSimpleName() + " between "
						+ property.getMinimum().doubleValue() * property.getDimensionFactor().doubleValue() + " and "
						+ property.getMaximum().doubleValue() * property.getDimensionFactor().doubleValue() + ".\n"
						+ getString("NumberEditor.MessageLine1") //$NON-NLS-1$
						+ getString("NumberEditor.MessageLine2"), //$NON-NLS-1$
				getString("NumberEditor.InvalidText"), //$NON-NLS-1$
				YES_NO_OPTION, ERROR_MESSAGE, null, options, options[1]);

		if (answer == 1) { // Revert!
			ftf.setValue(ftf.getValue());
			return true;
		}
		return false;
	}

}
