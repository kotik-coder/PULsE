package pulse.input;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import pulse.util.FunctionSerializer;
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
public class InterpolationDataset implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7439474910490135034L;
    private transient UnivariateFunction interpolation;
    private final List<ImmutableDataEntry<Double, Double>> dataset;

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
     * Constructs a new Akima spline interpolator and uses the available dataset to
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

    /*
     * Serialization
     */
    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        // default serialization 
        oos.defaultWriteObject();
        // write the object
        FunctionSerializer.writeSplineFunction((PolynomialSplineFunction) interpolation, oos);
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();
        this.interpolation = FunctionSerializer.readSplineFunction(ois);
    }

}