package pulse.tasks;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * An {@code AverageResult} is obtained by averaging a list of
 * {@code AbstractResult}s and calculating the associated errors of averaging.
 *
 */

public class AverageResult extends AbstractResult {

	private List<AbstractResult> results;

	public final static int SIGNIFICANT_FIGURES = 2;

	/**
	 * This will create an {@code AverageResult} based on the {@code AbstractResult}
	 * in {@code res}.
	 * <p>
	 * It will also use the {@code resultFormat}. A method will be invoked to: (a)
	 * calculate the average values of the list of {@code NumericProperty} according
	 * to the {@code resultFormat}; (b) calculate the standard error associated with
	 * averaging; (c) create a {@code BigDecimal} representation of the values and
	 * the errors, so that only {@value SIGNIFICANT_FIGURES} significant figures are
	 * left for consistency between the {@code value} and the {@code error}.
	 * </p>
	 * 
	 * @param res          a list of {@code AbstractResult}s that are going to be
	 *                     averaged (not necessarily instances of {@code Result}).
	 * @param resultFormat the {@code ResultFormat}, which will be used for this
	 *                     {@code AveragedResult}.
	 */

	public AverageResult(List<AbstractResult> res, ResultFormat resultFormat) {
		super(resultFormat);

		results = new ArrayList<>();

		for (AbstractResult r : res) {
			results.add(r);
		}

		average();

	}

	private void average() {

		/*
		 * Calculate average
		 */

		double[] av = new double[getFormat().abbreviations().size()];

		int size = results.size();

		for (int i = 0; i < av.length; i++) {
			for (AbstractResult r : results) {
				av[i] += ((Number) r.getProperty(i).getValue()).doubleValue();
			}
			av[i] /= size;
		}

		/*
		 * Calculate standard error std[j] for each column
		 */

		double[] std = new double[av.length];

		for (int j = 0; j < av.length; j++) {
			for (AbstractResult r : results) {
				std[j] += Math.pow(((Number) r.getProperty(j).getValue()).doubleValue() - av[j], 2);
			}
			std[j] /= size;
		}

		/*
		 * Round up
		 */

		BigDecimal resultAv, resultStd, stdBig, avBig;

		NumericProperty p;
		NumericPropertyKeyword key;

		for (int j = 0; j < av.length; j++) {
			key = getFormat().getKeywords().get(j);

			if (!Double.isFinite(std[j]))
				p = NumericProperty.derive(key, av[j]); // ignore error as the value is not finite
			else {
				stdBig = (new BigDecimal(std[j])).sqrt(MathContext.DECIMAL64);
				avBig = new BigDecimal(av[j]);

				resultStd = stdBig.setScale(SIGNIFICANT_FIGURES - stdBig.precision() + stdBig.scale(),
						RoundingMode.HALF_UP);

				if (stdBig.precision() > 1)
					resultAv = avBig.setScale(resultStd.scale(), RoundingMode.CEILING);
				else
					resultAv = avBig;

				p = NumericProperty.derive(key, resultAv.doubleValue());
				p.setError(resultStd.doubleValue());

			}

			addProperty(p);

		}

	}

	/**
	 * This will analyse the list of {@code AbstractResult}s used for calculation of
	 * the average and find all associated individual results.
	 * <p>
	 * If it is established that some instances of {@code AverageResult} were used
	 * in the calculation, this will invoke this method recursively to get a full
	 * list of {@code AbstractResult}s that are not {@code AverageResult}s
	 * 
	 * @return a list of {@code AbstractResult}s that are guaranteed not to be
	 *         {@code AveragedResult}s.
	 */

	public List<AbstractResult> getIndividualResults() {
		List<AbstractResult> indResults = new ArrayList<>();

		for (AbstractResult r : results) {
			if (r instanceof AverageResult) {
				AverageResult ar = (AverageResult) r;
				indResults.addAll(ar.getIndividualResults());
			} else
				indResults.add(r);
		}

		return indResults;
	}

}