package pulse.search.statistics;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.CORRELATION_THRESHOLD;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class CorrelationTest extends PropertyHolder implements Reflexive {

    private static double threshold = (double) def(CORRELATION_THRESHOLD).getValue();

    private static final InstanceDescriptor<CorrelationTest> instanceDescriptor
            = new InstanceDescriptor<>(
                    "Correlation Test Selector", CorrelationTest.class);

    static {
        instanceDescriptor.setSelectedDescriptor(EmptyCorrelationTest.class.getSimpleName());
    }

    public CorrelationTest() {
        //intentionally blank
    }

    public static CorrelationTest init() {
        return instanceDescriptor.newInstance(CorrelationTest.class);
    }

    public final static InstanceDescriptor<CorrelationTest> getTestDescriptor() {
        return instanceDescriptor;
    }

    public abstract double evaluate(double[] x, double[] y);

    public boolean compareToThreshold(double value) {
        return Math.abs(value) > threshold;
    }

    public static NumericProperty getThreshold() {
        return derive(CORRELATION_THRESHOLD, threshold);
    }

    public static void setThreshold(NumericProperty p) {
        requireType(p, CORRELATION_THRESHOLD);
        threshold = (double) p.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == NumericPropertyKeyword.CORRELATION_THRESHOLD) {
            threshold = (double) property.getValue();
        }
    }

}
