package pulse.tasks.processing;

import static pulse.properties.NumericProperties.derive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import pulse.properties.NumericProperties;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * An {@code AverageResult} is obtained by averaging a list of
 * {@code AbstractResult}s and calculating the associated errors of averaging.
 *
 */
public class AverageResult extends AbstractResult {

    private final List<AbstractResult> results;

    public final static int SIGNIFICANT_FIGURES = 2;

    /**
     * This will create an {@code AverageResult} based on the
     * {@code AbstractResult} in {@code res}.
     * <p>
     * It will also use the {@code resultFormat}. A method will be invoked to:
     * (a) calculate the mean values of the list of {@code NumericProperty}
     * according to the {@code resultFormat}; (b) calculate the statistical error; 
     * (c) create a {@code BigDecimal} representation
     * of the values and the errors, so that only {@value SIGNIFICANT_FIGURES}
     * significant figures are left for consistency between the {@code value}
     * and the {@code error}.
     * </p>
     *
     * @param res a list of {@code AbstractResult}s that are going to be
     * averaged (not necessarily instances of {@code Result}).
     * @param resultFormat the {@code ResultFormat}, which will be used for this
     * {@code AveragedResult}.
     */
    public AverageResult(List<AbstractResult> res, ResultFormat resultFormat) {
        super(resultFormat);

        results = new ArrayList<>();
        results.addAll(res);

        calculate();
    }

    private void calculate() {

        var rs = new ResultStatistics();

        rs.process(results);

        /*
		 * Calculate average and statistical error for each column
         */
        double[] av = rs.getAverages();
        double[] err = rs.getErrors();

        /*
		 * Round up
         */
        NumericProperty p;
        NumericPropertyKeyword key;

        for (int j = 0; j < av.length; j++) {
            key = getFormat().getKeywords().get(j);

            if (!Double.isFinite(err[j])) {
                p = derive(key, av[j]); // ignore error as the value is not finite
            } else if(NumericProperties.def(key).getValue() instanceof Double) {
                var stdBig  = new BigDecimal(err[j]);
                var avBig   = new BigDecimal(av[j]);

                var error = stdBig.setScale(
                        SIGNIFICANT_FIGURES - stdBig.precision() + stdBig.scale(),
                        RoundingMode.HALF_UP);

                var mean = stdBig.precision() > 1 ? 
                        avBig.setScale(error.scale(), RoundingMode.CEILING) 
                        : avBig;

                p = derive(key, mean.doubleValue());
                p.setError(error.doubleValue());

            } else {
                //if integer
                p = derive(key, (int)Math.round( av[j] ) );
                p.setError((int)Math.round( err[j] ) );
            }

            addProperty(p);

        }

    }

    /**
     * This will analyse the list of {@code AbstractResult}s used for
     * calculation of the mean and find all associated individual results.
     * <p>
     * If it is established that some instances of {@code AverageResult} were
     * used in the calculation, this will invoke this method recursively to get
     * a full list of {@code AbstractResult}s that are not
     * {@code AverageResult}s
     *
     * @return a list of {@code AbstractResult}s that are guaranteed not to be
     * {@code AveragedResult}s.
     */
    public List<AbstractResult> getIndividualResults() {
        List<AbstractResult> indResults = new ArrayList<>();

        for (AbstractResult r : results) {
            if (r instanceof AverageResult) {
                var ar = (AverageResult) r;
                indResults.addAll(ar.getIndividualResults());
            } else {
                indResults.add(r);
            }
        }

        return indResults;
    }

}
