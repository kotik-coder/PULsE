package pulse.problem.schemes;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.properties.NumericProperty;

/**
 * An {@code ADIScheme} uses a {@code Grid2D} to provide numerical capabilities
 * needed to solve a {@code Problem}.
 *
 */
public abstract class ADIScheme extends DifferenceScheme {

    /**
     * Creates a new {@code ADIScheme} with default values of grid density and
     * time factor.
     */
    public ADIScheme() {
        this(derive(GRID_DENSITY, 30), derive(TAU_FACTOR, 0.5));
    }

    /**
     * Creates an {@code ADIScheme} with the specified arguments. This creates
     * an associated {@code Grid2D} object.
     *
     * @param N the grid density
     * @param timeFactor the time factor (&tau;<sub>F</sub>)
     */
    public ADIScheme(NumericProperty N, NumericProperty timeFactor) {
        super();
        setGrid(new Grid2D(N, timeFactor));
    }

    /**
     * Creates an {@code ADIScheme} with the specified arguments. This creates
     * an associated {@code Grid2D} object.
     *
     * @param N the grid density
     * @param timeFactor the time factor (&tau;<sub>F</sub>)
     * @param timeLimit a custom time limit (<i>t</i><sub>lim</sub>)
     */
    public ADIScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        setTimeLimit(timeLimit);
        setGrid(new Grid2D(N, timeFactor));
    }

    /**
     * Prints out the description of this problem type.
     *
     * @return a verbose description of the problem.
     */
    @Override
    public String toString() {
        return getString("ADIScheme.4");
    }

}
