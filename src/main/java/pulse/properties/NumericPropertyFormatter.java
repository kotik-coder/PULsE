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
package pulse.properties;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.NumberFormatter;
import pulse.math.Segment;
import pulse.ui.Messages;

/**
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public class NumericPropertyFormatter extends AbstractFormatter {

    private NumericPropertyKeyword key;
    private Segment bounds;
    private boolean convertDimension = true;
    private boolean addHtmlTags = true;

    private final static String PLUS_MINUS = Messages.getString("NumericProperty.PlusMinus");

    /**
     * Start using scientific notations for number whose absolute values are
     * larger than {@value UPPER_LIMIT}.
     */
    public final static double UPPER_LIMIT = 1e4;

    /**
     * Start using scientific notations for number whose absolute values are
     * lower than {@value LOWER_LIMIT}.
     */
    public final static double LOWER_LIMIT = 1e-2; // the lower limit, used for formatting

    private final static double ZERO = 1e-30;

    /**
     * @param convertDimension if {@code true}, the output will be the
     * {@code value * dimensionFactor}
     */
    public NumericPropertyFormatter(NumericProperty p, boolean convertDimension, boolean addHtmlTags) {
        this.key = p.getType();
        this.convertDimension = convertDimension;
        this.bounds = p.getBounds();
        this.addHtmlTags = addHtmlTags;
    }

    public NumberFormat numberFormat(NumericProperty p) {
        Number value = (Number) p.getValue();
        NumberFormat f;

        if (value instanceof Integer) {
            f = NumberFormat.getIntegerInstance();
        } else {

            double adjustedValue = convertDimension ? value.doubleValue() * p.getDimensionFactor().doubleValue()
                    : (double) value;
            double absAdjustedValue = Math.abs(adjustedValue);

            if (addHtmlTags && 
                    ( (absAdjustedValue > UPPER_LIMIT) 
                   || (absAdjustedValue < LOWER_LIMIT && absAdjustedValue > ZERO)) ) {
                //format with scientific notations
                f = new ScientificFormat(p.getDimensionFactor(), p.getDimensionDelta());
            } else {
                //format "standard" numbers
                f = new DecimalFormatImpl(p);
            }

        }
        
        return f;

    }

    @Override
    public Object stringToValue(String arg0) throws ParseException {
        var nf = new NumberFormatter();
        Number n = (Number) nf.stringToValue(arg0);
        this.setEditValid(
                bounds.contains(n.doubleValue()));
        return NumericProperties.derive(key, n);
    }

    /**
     * Used to print out a nice {@code value} for GUI applications and for
     * exporting.
     * <p>
     * Will use a {@code DecimalFormat} to reduce the number of digits, if
     * necessary. Automatically detects whether it is dealing with {@code int}
     * or {@code double} values, and adjust formatting accordingly. If
     * {@code error != null}, will use the latter as the error value, which is
     * separated from the main value by a plus-minus sign.
     * </p>
     *
     * @return a nice {@code String} representing the {@code value} of this
     * {@code NumericProperty} and its {@code error}
     */
    @Override
    public String valueToString(Object o) throws ParseException {
        if (o == null) {
            return "";
        }

        if (!(o instanceof NumericProperty)) {
            throw new IllegalArgumentException("Cannot format. Not a property: "
                    + o.getClass());
        }

        var p = (NumericProperty) o;
        String result;

        if (Double.isInfinite(
                ((Number) p.getValue()).doubleValue())) {
            result = "&infin;";
        } else if (Double.isNaN(
                ((Number) p.getValue()).doubleValue())) {
            result = "unknown";
        } else {

            if (p.getError() != null) {
                result = formatValueAndError(p);
            } else {
                result = formatValueOnly(p);
            }

        }

        return addHtmlTags ? encloseInHtmlTags(result) : result;

    }

    private String encloseInHtmlTags(String s) {
        return new StringBuffer("<html>").append(s).append("</html>").toString();
    }

    private String formatValueOnly(NumericProperty p) {
        return numberFormat(p).format(p.getValue());
    }

    private String formatValueAndError(NumericProperty p) {
        Number adjustedValue = ((Number) p.getValue());
        var selectedFormat = numberFormat(p);
        String value = selectedFormat.format(adjustedValue);
        String errorString = selectedFormat.format(
                adjustedValue instanceof Double
                        ? (p.getError().doubleValue() - p.getDimensionDelta().doubleValue())
                        : p.getError().intValue() - p.getDimensionDelta().intValue());
        return selectedFormat.format(adjustedValue) + PLUS_MINUS + errorString;
    }

    public boolean isDimensionConverted() {
        return convertDimension;
    }

    public boolean areHtmlTagsAdded() {
        return addHtmlTags;
    }

    public Segment getBounds() {
        return bounds;
    }

    private class DecimalFormatImpl extends DecimalFormat {

        private final long dimensionDelta;

        public DecimalFormatImpl(NumericProperty p) {
            super();
            dimensionDelta = p.getDimensionDelta().longValue();
            final int digits = p.getType() == NumericPropertyKeyword.TEST_TEMPERATURE ? 1 : 4;
            setMinimumFractionDigits(digits);
            setMaximumFractionDigits(digits);

            if (convertDimension) {
                setMultiplier(p.getDimensionFactor().intValue());
            }
        }

        @Override
        public StringBuffer format(long arg0, StringBuffer arg1, FieldPosition arg2) {
            return super.format(
                    //add delta (e.g. -273.15)
                    arg0 + dimensionDelta,
                    arg1, arg2);
        }

        @Override
        public StringBuffer format(double arg0, StringBuffer arg1, FieldPosition arg2) {
            return super.format(
                    //add delta (e.g. -237.15)
                    arg0 + dimensionDelta,
                    arg1, arg2);
        }

        //parse not needed for temperature since this is not changed
    }

}
