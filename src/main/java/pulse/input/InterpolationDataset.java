package pulse.input;

import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import pulse.input.listeners.ExternalDatasetListener;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.ImmutableDataEntry;

/**
 * An {@code InterpolationDataset} stores data in a {@code List} of
 * {@code DataEntry<Double,Double>} objects (each containing a 'key' and a
 * 'value') and provides means to interpolate between the 'values' using the
 * 'keys'. This is used mainly to interpolate between available data for thermal
 * properties loaded in tabular representation, e.g. the density and specific
 * heat tables. Features a static list of {@code ExternalDatasetListener}s.
 *
 * @see pulse.input.listeners.ExternalDatasetListener
 */
public class InterpolationDataset {

    private UnivariateFunction interpolation;
    private List<ImmutableDataEntry<Double, Double>> dataset;
    private static Map<StandartType, InterpolationDataset> standartDatasets = new HashMap<StandartType, InterpolationDataset>();
    private static List<ExternalDatasetListener> listeners = new ArrayList<>();

	/**
	 * Creates an empty {@code InterpolationDataset}.
	 */

	public InterpolationDataset() {
        dataset = new ArrayList<>();
    }

    /**
     * Provides an interpolated value at {@code key} based on the available data
     * in the {@code DataEntry List}. The interpolation is done using natural
     * cubic splines, hence it is important that the input noise is minimal.
     *
     * @param key the argument, at which interpolation needs to be done (e.g.
     * temperature)
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
        var interpolator = new AkimaSplineInterpolator();
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
    public static InterpolationDataset getDataset(StandartType type) {
        return standartDatasets.get(type);
    }

    /**
     * Puts a datset specified by {@code type} into the static hash map of this
     * class, using {@code type} as key. Triggers {@code onDensityDataLoaded}
     *
     * @param dataset a dataset to be appended to the static hash map
     * @param type the dataset type
     */
    public static void setDataset(InterpolationDataset dataset, StandartType type) {
        standartDatasets.put(type, dataset);
        listeners.stream().forEach(l -> l.onDataLoaded(type));
    }

    /**
     * Creates a list of property keywords that can be derived with help of the
     * loaded data. For example, if heat capacity and density data is available,
     * the returned list will contain {@code CONDUCTIVITY}.
     *
     * @return
     */
    public static List<NumericPropertyKeyword> derivableProperties() {
        var list = new ArrayList<NumericPropertyKeyword>();
        if (standartDatasets.containsKey(StandartType.HEAT_CAPACITY)) {
            list.add(SPECIFIC_HEAT);
        }
        if (standartDatasets.containsKey(StandartType.DENSITY)) {
            list.add(DENSITY);
        }
        if (list.contains(SPECIFIC_HEAT) && list.contains(DENSITY)) {
            list.add(CONDUCTIVITY);
        }
        return list;
    }

    public static void addListener(ExternalDatasetListener l) {
        listeners.add(l);
    }

    public enum StandartType {

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
