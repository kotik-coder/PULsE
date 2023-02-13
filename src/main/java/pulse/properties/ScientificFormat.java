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
import java.text.ParsePosition;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public class ScientificFormat extends NumberFormat {

    private static final long serialVersionUID = -6509402151736747913L;
    private final int dimensionFactor;
    private final double dimensionDelta;

    public ScientificFormat(Number dimensionFactor, Number dimensionDelta) {
        super();
        this.dimensionFactor = dimensionFactor.intValue();
        this.dimensionDelta = dimensionDelta.doubleValue();
    }

    private static int getExponentForNumber(double number) {
        var nf = new DecimalFormat("0.000E000");
        String numberAsString = nf.format(number);
        try {
            var substring = numberAsString.substring(numberAsString.indexOf('E') + 1, numberAsString.length());
            return NumberFormat.getIntegerInstance().parse(substring).intValue();
        } catch (ParseException ex) {
            //no "E" found
            return 0;
        }
    }

    @Override
    public StringBuffer format(double arg0, StringBuffer arg1, FieldPosition arg2) {
        double adjusted = arg0 * dimensionFactor + dimensionDelta;

        int exponent = getExponentForNumber(adjusted);
        double mantissa = adjusted / Math.pow(10, exponent);

        return format(mantissa, exponent, true);
    }

    private static StringBuffer format(Number a, Number b, boolean decimal) {
        StringBuffer sb = new StringBuffer();
        var nf = new DecimalFormat();
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(decimal ? 2 : 0);

        sb.append(nf.format(a))
                .append(" &times; 10<sup>")
                .append(b)
                .append("</sup>");

        return sb;
    }

    @Override
    public StringBuffer format(long arg0, StringBuffer arg1, FieldPosition arg2) {
        long adjusted = arg0 * dimensionFactor + Double.doubleToLongBits(dimensionDelta);

        int exponent = Math.getExponent(adjusted);
        long mantissa = Double.doubleToLongBits(adjusted / Math.pow(2, exponent));

        return format(mantissa, exponent, false);
    }

    @Override
    public Number parse(String arg0, ParsePosition arg1) {
        var tokenizer = new StringTokenizer(arg0);
        Number a = null;
        Number b = null;
        try {
            a = NumberFormat.getInstance().parse(tokenizer.nextToken(" "));
            tokenizer.nextToken(); //ignore &times;
            b = NumberFormat.getInstance().parse(tokenizer.nextToken(" "));
        } catch (ParseException ex) {
            Logger.getLogger(ScientificFormat.class.getName()).log(Level.SEVERE, null, ex);
        }

        Number result;

        if (a instanceof Double) {
            result = a.doubleValue() * Math.pow(10, b.doubleValue());
        } else {
            result = a.longValue() * Math.pow(10, b.intValue());
        }

        return result;
    }

}
