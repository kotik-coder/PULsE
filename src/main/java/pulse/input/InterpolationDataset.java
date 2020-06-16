package pulse.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import pulse.util.ImmutableDataEntry;

/**
 * An {@code InterpolationDataset} stores data in a {@code List} of
 * {@code DataEntry<Double,Double>} objects (each containing a 'key' and a
 * 'value') and provides means to interpolate between the 'values' using the
 * 'keys'. This is used mainly to interpolate between available data for thermal
 * properties loaded in tabular representation, e.g. the density and specific
 * heat tables.
 */

public class InterpolationDataset {

	private List<ImmutableDataEntry<Double, Double>> dataset;
	private static Map<StandartType, InterpolationDataset> standartDatasets = new HashMap<StandartType, InterpolationDataset>();
	private UnivariateFunction interpolation;

	/**
	 * Creates an empty {@code InterpolationDataset}.
	 */

	public InterpolationDataset() {
		dataset = new ArrayList<ImmutableDataEntry<Double, Double>>();
	}

	/**
	 * Iterates over the {@code List} of {@code DataEntry} objects to find one that
	 * has the closest {@code getKey() < key} value to the argument {@code key}.
	 * 
	 * @param key the key, which is the upper bound for the search.
	 * @return a {@code DataEntry} object, satisfying the conditions above.
	 */

	public ImmutableDataEntry<Double, Double> previousTo(double key) {
		return dataset.stream().filter(element -> element.getKey() < key).reduce((a, b) -> b).get();
	}

	/**
	 * Provides an interpolated value at {@code key} based on the available data in
	 * the {@code DataEntry List}. The interpolation is done using natural cubic
	 * splines, hence it is important that the input dataset has minimal noise.
	 * @param key the argument, at which interpolation needs to be done (e.g.
	 *            temperature)
	 * @return a double, representing the interpolated value
	 * @see previousTo
	 */

	public double interpolateAt(double key) {
		return interpolation.value(key);

	}

	/**
	 * Adds {@code entry} to this {@code InterpolationDataset}.
	 * 
	 * @param entry the entry to be added
	 */

	public void add(ImmutableDataEntry<Double, Double> entry) {
		dataset.add(entry);
	}

	public void finalize() {
		var interpolator = new SplineInterpolator();
		interpolation = interpolator.interpolate(dataset.stream().map(a -> a.getKey()).mapToDouble(d -> d).toArray(),
								 dataset.stream().map(a -> a.getValue()).mapToDouble(d -> d).toArray()); 
	}
	
	/**
	 * Extracts all data available in this {@code InterpolationDataset}.
	 * 
	 * @return the {@code List} of data.
	 */

	public List<ImmutableDataEntry<Double, Double>> getData() {
		return dataset;
	}

	public static InterpolationDataset getDataset(StandartType type) {
		return standartDatasets.get(type);
	}

	public static void setDataset(InterpolationDataset dataset, StandartType type) {
		standartDatasets.put(type, dataset);
	}

	public enum StandartType {
		SPECIFIC_HEAT, DENSITY;
	}

}