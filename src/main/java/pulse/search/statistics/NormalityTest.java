package pulse.search.statistics;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;

/**
 * A normality test is invoked after a task finishes, to validate its result. It
 * may be used as an acceptance criterion for tasks.
 *
 * For the test to pass, the model residuals need be distributed according to a
 * (0, &sigma;) normal distribution, where &sigma; is the variance of the model
 * residuals. As this is the pre-requisite for optimisers based on the ordinary
 * least-square statistic, the normality test can also be used to estimate if a
 * fit 'failed' or 'succeeded' in describing the data.
 * 
 * The test consists in testing the relation <math>statistic < critValue</math>,
 * where the critical value is determined based on a given level of significance.
 *
 */
public abstract class NormalityTest extends ResidualStatistic {

    private double statistic;
    protected static double significance = (double) def(SIGNIFICANCE).getValue();

    private static String selectedTestDescriptor;

    protected NormalityTest() {
        statistic = (double) def(TEST_STATISTIC).getValue();
    }

    public static NumericProperty getStatisticalSignifiance() {
        return derive(SIGNIFICANCE, significance);
    }

    public static void setStatisticalSignificance(NumericProperty alpha) {
        requireType(alpha, SIGNIFICANCE);
        NormalityTest.significance = (double) alpha.getValue();
    }

    public abstract boolean test(SearchTask task);

    @Override
    public NumericProperty getStatistic() {
        return derive(TEST_STATISTIC, statistic);
    }

    @Override
    public void setStatistic(NumericProperty statistic) {
        requireType(statistic, TEST_STATISTIC);
        this.statistic = (double) statistic.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == TEST_STATISTIC) {
            statistic = (double) property.getValue();
        }
    }

    public static String getSelectedTestDescriptor() {
        return selectedTestDescriptor;
    }

    public static void setSelectedTestDescriptor(String selectedTestDescriptor) {
        NormalityTest.selectedTestDescriptor = selectedTestDescriptor;
    }

}
