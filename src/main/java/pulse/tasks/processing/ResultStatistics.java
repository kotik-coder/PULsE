package pulse.tasks.processing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import pulse.properties.NumericPropertyKeyword;

import pulse.util.ImmutablePair;

/**
 * A simple collection of mean values and errors associated with an
 * {@code AverageResult}, and a method for calculating those statistics.
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
class ResultStatistics {

    private double[] av;
    private double[] err;

    /**
     * Confidence level of {@value CONFIDENCE_LEVEL} for error calculation using
     * <i>t</i>-distribution quantiles. 
     */
    public final static double CONFIDENCE_LEVEL = 0.95;

    public ResultStatistics() {
        //intentionaly blank
    }

    /**
     * This will calculate the statistics for each type of
     * {@code NumericProperty} stored in the {@code results}. It is assumed that
     * the order in which these properties appear in each element of
     * {@code results} is consistent, and it will be maintained as the order in
     * which these statistic are stored. The calculated statistics include the
     * arithmetic mean and the statistical error. The latter is calculated
     * assuming a {@value CONFIDENCE_LEVEL} confidence level, by calculating a
     * standard deviation for each {@code NumericPropertyKeyword} and
     * multiplying the result by the quantile value of the
     * <i>t</i>-distribution. The inverse cumulative distribution function of 
     * Student distribution is calculated using {@code ApacheCommonsMath} library.
     *
     * @param results a list of individual (or average) results to be processed
     */
    public void process(List<AbstractResult> results) {

        //group all properties contained in the list of results by their keyword
        Map<NumericPropertyKeyword, List<Double>> map = results.stream()
                .flatMap(r -> r.getProperties().stream())
                .collect(Collectors.groupingBy(t -> t.getType(),
                        Collectors.mapping(p
                                -> ((Number) p.getValue()).doubleValue(),
                                Collectors.toList())));

        /*
        The number of elements in the parameter list. This ASSUMES that the input
        list contains results with the same number of output parameters!
         */
        
        StandardDeviation sd = new StandardDeviation(true); //bias-corrected sd
        double sqrtn = Math.sqrt(results.size());

        //calculate average values
        
        var stats = ResultFormat.getInstance().getKeywords().stream()
            .map(key -> map.get(key))    //preserve the original order of keywods
            .map(c -> {
            double mean = openStream(c).average().orElse(0.0); //fail-safe, in case if avg is undefined
            return new ImmutablePair<Double>(
                    mean, //the mean value
                    sd.evaluate(openStream(c).toArray(), mean) //that would be the sample standard deviation
                    / sqrtn //however, since we are calculating the std of the MEAN, 
                            //we need to divide the result by sqrtn
            );
        }).collect(Collectors.toList());

        av = stats.stream().mapToDouble(pair -> pair.getFirst()).toArray(); //store mean values

        //Student t-distribution with degrees of freedom equal to number of individual results
        TDistribution student = new TDistribution(av.length);
        //CDF value
        double t = student.inverseCumulativeProbability(CONFIDENCE_LEVEL);   //right tail

        err = stats.stream().mapToDouble(pair
                -> t * pair.getSecond() //the error is equal to half of the span of the confidence interval
        ).toArray(); //store errors

    }
    
    private DoubleStream openStream(List<Double> input) {
        return input.stream().mapToDouble(d -> d);
    }

    /**
     * Retrieves the mean values of properties.
     *
     * @return the mean values
     * @see process()
     */
    public double[] getAverages() {
        return av;
    }

    /**
     * Retrieves the statistical errors associated with the property values.
     *
     * @return the values of the statistical error
     * @see process()
     */
    public double[] getErrors() {
        return err;
    }

}
