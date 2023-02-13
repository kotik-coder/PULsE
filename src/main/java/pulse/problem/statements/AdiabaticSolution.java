package pulse.problem.statements;

import java.io.Serializable;
import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import pulse.HeatingCurve;
import pulse.problem.statements.model.ThermalProperties;

public class AdiabaticSolution implements Serializable {

    private static final long serialVersionUID = 4240406501288696621L;
    public final static int DEFAULT_CLASSIC_PRECISION = 200;
    public final static int DEFAULT_POINTS = 100;

    private AdiabaticSolution() {
        //do nothing
    }

    /**
     * A static factory method for calculating a heating curve based on the
     * analytical solution of Parker et al.
     * <p>
     * The math itself is done separately in the {@code Problem} class. This
     * method creates a {@code HeatingCurve} with the number of points equal to
     * that of the {@code p.getHeatingCurve()}, and with the same baseline. The
     * solution is calculated for the time range {@code 0 <= t <= timeLimit}.
     * </p>
     *
     * @param p The problem statement, providing access to the
     * {@code classicSolutionAt} method and to the {@code HeatingCurve} object
     * it owns.
     * @param timeLimit The upper time limit (in seconds)
     * @param precision The second argument passed to the
     * {@code classicSolutionAt}
     * @return a {@code HeatingCurve} representing the analytical solution.
     * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i>
     * Journal of Applied Physics <b>32</b> (1961) 1679</a>
     * @see Problem.classicSolutionAt(double,int)
     */
    public static HeatingCurve classicSolution(Problem p, double timeLimit, int precision) {
        final int points = DEFAULT_POINTS;
        var classicCurve = new HeatingCurve(derive(NUMPOINTS, points));

        final double step = timeLimit / (points - 1.0);
        var prop = p.getProperties();

        for (int i = 1; i < points; i++) {
            classicCurve.addPoint(i * step, solutionAt(prop, i * step, precision));
        }

        classicCurve.apply(p.getBaseline());
        classicCurve.setName("Adiabatic Solution");

        return classicCurve;
    }

    /**
     * <p>
     * Calculates the classic analytical solution
     * <math><i>T(x=l</i>,<code>time</code>)</math> of Parker et al. at the
     * specified {@code time} using the first {@code n = precision} terms of the
     * solution series. The results is then scaled by a factor of
     * {@code signalHeight} and returned.
     * </p>
     *
     * @param time The calculation time
     * @param precision The number of terms in the approximated solution
     * @return a double, representing
     * <math><i>T(x=l</i>,<code>time</code>)</math>
     * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i>
     * Journal of Applied Physics <b>32</b> (1961) 1679</a>
     */
    private final static double solutionAt(ThermalProperties p, double time, int precision) {

        final double EPS = 1E-8;
        final double Fo = time / p.characteristicTime();

        if (time < EPS) {
            return 0;
        }

        double sum = 0;

        for (int i = 1; i <= precision; i++) {
            sum += pow(-1, i) * exp(-pow(i * PI, 2) * Fo);
        }

        return (1. + 2. * sum) * (double) p.getMaximumTemperature().getValue();

    }

    /**
     * Calculates the classic solution, using the default value of the
     * {@code precision} and the time limit specified by the
     * {@code HeatingCurve} of {@code p}.
     *
     * @param p the problem statement
     * @return a {@code HeatinCurve}, representing the classic solution.
     * @see classicSolution
     */
    public static HeatingCurve classicSolution(Problem p) {
        return classicSolution(p, p.getHeatingCurve().timeLimit(), DEFAULT_CLASSIC_PRECISION);
    }

    public static HeatingCurve classicSolution(Problem p, double timeLimit) {
        return classicSolution(p, timeLimit, DEFAULT_CLASSIC_PRECISION);
    }

}
