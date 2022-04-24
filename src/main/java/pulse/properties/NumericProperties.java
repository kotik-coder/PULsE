package pulse.properties;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import pulse.io.export.XMLConverter;
import pulse.ui.Messages;

/**
 * Default operations with NumericProperties
 *
 */
public class NumericProperties {

    /**
     * The list of default properties read that is created by reading the
     * default {@code .xml} file.
     */
    private final static List<NumericProperty> DEFAULT = XMLConverter.readDefaultXML();

    private NumericProperties() {
        //empty constructor
    }

    /**
     * Checks whether the {@code val} that is going to be passed to the
     * {@code property} (a) has the same type as the {@code property.getValue()}
     * object; (b) is confined within the definition domain:
     * {@code minimum <= value <= maximum}.
     *
     * @param property the {@code property} containing the definition domain
     * @param val a numeric value, the conformity of which to the definition
     * domain needs to be checked
     * @return {@code true} if {@code minimum <= val <= maximum} and if both
     * {@code val} and {@code value} are instances of the same {@code class};
     * {@code false} otherwise
     */
    public static boolean isValueSensible(NumericProperty property, Number val) {
        if (!property.getValue().getClass().equals(val.getClass())) {
            return false;
        }

        double v = val.doubleValue();
        final double EPS = 1E-12;
        boolean ok = true;
        
        if(        !Double.isFinite(v) 
                || v > property.getMaximum().doubleValue() + EPS
                || v < property.getMinimum().doubleValue() - EPS) {
            ok = false;
        }

        return ok;
    }

    public static String printRangeAndNumber(NumericProperty p, Number value) {
        StringBuilder msg = new StringBuilder();
        msg.append("Acceptable region for ");
        msg.append("parameter : ");
        msg.append(p.getValue().getClass().getSimpleName());
        msg.append(" [ " + p.getMinimum());
        msg.append(" : " + p.getMaximum() + " ]. ");
        msg.append("Value received: " + value);
        return msg.toString();
    }

    public static List<NumericProperty> defaultList() {
        return DEFAULT;
    }

    /**
     * Searches for the default {@code NumericProperty} corresponding to
     * {@code keyword} in the list of pre-defined properties loaded from the
     * respective {@code .xml} file.
     *
     * @param keyword one of the constant {@code NumericPropertyKeyword}s
     * @return a {@code NumericProperty} in the default list of properties
     * @see pulse.properties.NumericPropertyKeyword
     */
    public static NumericProperty def(NumericPropertyKeyword keyword) {
        return new NumericProperty(DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get());
    }

    /**
     * Compares the numeric values of this {@code NumericProperty} and
     * {@code arg0}
     *
     * @param a a {@code NumericProperty}
     * @param b another {@code NumericProperty}
     * @return {@code true} if the values are equals
     */
    public static int compare(NumericProperty a, NumericProperty b) {
        Double d1 = ((Number) a.getValue()).doubleValue();
        Double d2 = ((Number) b.getValue()).doubleValue();

        final double eps = 1E-8 * (d1 + d2) / 2.0;

        return Math.abs(d1 - d2) < eps ? 0 : d1.compareTo(d2);
    }

    /**
     * Searches for the default {@code NumericProperty} corresponding to
     * {@code keyword} in the list of pre-defined properties loaded from the
     * respective {@code .xml} file, and if found creates a new
     * {
     *
     * @NumericProperty} which will replicate all field of the latter, but will
     * set its value to {@code value}.
     *
     * @param keyword one of the constant {@code NumericPropertyKeyword}s
     * @param value the new value for the created {@code NumericProperty}
     * @return a new {@code NumericProperty} that is built according to the
     * default pattern specified by the {@code keyword}, but with a different
     * {@code value}
     * @see pulse.properties.NumericPropertyKeyword
     */
    public static NumericProperty derive(NumericPropertyKeyword keyword, Number value) {
        return new NumericProperty(value, DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get());
    }

    public static boolean isDiscrete(NumericPropertyKeyword key) {
        return def(key).isDiscrete();
    }

}
