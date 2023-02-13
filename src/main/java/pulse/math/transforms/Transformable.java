package pulse.math.transforms;

import java.io.Serializable;

/**
 * An interface for performing reversible one-to-one mapping of the model
 * parameters.
 *
 */
public interface Transformable extends Serializable {

    /**
     * Performs the selected transform with {@code value}
     *
     * @param value a double representing the parameter value
     * @return the results, such that
     * {@code inverse( transform(value) ) = value}
     */
    public double transform(double value);

    /**
     * Inverses the transform.
     */
    public double inverse(double t);

}
