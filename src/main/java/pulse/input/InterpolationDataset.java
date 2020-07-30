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
	private static Map<StandardType, InterpolationDataset> standartDatasets = new HashMap<StandardType, InterpolationDataset>();
	private UnivariateFunction interpolation;

	/**
	 * Creates an empty {@code InterpolationDataset}.
	 */

	public InterpolationDataset() {
		dataset = new ArrayList<>();
	}

	/**
	 * Provides an interpolated value at {@code key} based on the available data in
	 * the {@code DataEntry List}. The interpolation is done using natural cubic
	 * splines, hence it is important that the input noise is minimal.
	 * 
	 * @param key the argument, at which interpolation needs to be done (e.g.
	 *            temperature)
	 * @return a double, representing the interpolated value
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

	/**
	 * Constructs a new spline interpolator and uses the available dataset to
	 * produce a {@code SplineInterpolation}.
	 */

	public void doInterpolation() {
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

	/**
	 * Retrieves a standard dataset previously loaded by the respective reader.
	 * 
	 * @param type the standard dataset type
	 * @return an {@code InterpolationDataset} corresponding to {@code type}
	 */

	public static InterpolationDataset getDataset(StandardType type) {
		return standartDatasets.get(type);
	}

	/**
	 * Puts a datset specified by {@code type} into the static hash map of this
	 * class, using {@code type} as key
	 * 
	 * @param dataset a dataset to be appended to the static hash map
	 * @param type    the dataset type
	 */

	public static void setDataset(InterpolationDataset dataset, StandardType type) {
		standartDatasets.put(type, dataset);
	}

	public enum StandardType {

		/**
		 * A keyword for the heat capacity dataset (in J/kg/K).
		 */

		HEAT_CAPACITY,

		/**
		 * A keyword for the density dataset (in kg/m<sup>3</sup>).
		 */

		DENSITY;

	}

}