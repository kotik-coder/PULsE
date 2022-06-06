package pulse.problem.schemes.rte.dom;

import java.util.List;

import pulse.io.readers.ButcherTableauReader;
import pulse.math.linear.Vector;
import pulse.properties.Property;
import pulse.util.DiscreteSelector;

/**
 * Explicit Runge-Kutta integrator with Hermite interpolation for the solution
 * of one-dimensional radiative transfer problems.
 *
 * @author Artem Lunev, Vadim Zborovskii
 *
 */
public class ExplicitRungeKutta extends AdaptiveIntegrator {

    private ButcherTableau tableau;
    private DiscreteSelector<ButcherTableau> tableauSelector;

    public ExplicitRungeKutta(Discretisation intensities) {
        super(intensities);

        tableauSelector = new DiscreteSelector<>(ButcherTableauReader.getInstance(), "/solvers/",
                "Solvers.list");
        tableauSelector.setDefaultSelection(ButcherTableau.DEFAULT_TABLEAU);
        tableau = tableauSelector.getDefaultSelection();
        tableauSelector.addListener(() -> setButcherTableau((ButcherTableau) tableauSelector.getValue()));
    }

    @Override
    public Vector[] step(final int j, final double sign) {

        var quantities = getDiscretisation().getQuantities();

        final var grid = getDiscretisation().getGrid();
        final var ordinates = getDiscretisation().getOrdinates();

        final double h = grid.step(j, sign);
        final double hSigned = h * sign;
        final double t = grid.getNode(j);

        final int nPositiveStart = ordinates.getFirstPositiveNode();
        final int nNegativeStart = ordinates.getFirstNegativeNode();

        var hermite = getHermiteInterpolator();

        hermite.a = t;
        hermite.bMinusA = hSigned;

        /*
		 * Indices of outward (n1 to n2) and inward (> n3) intensities
         */
        final int n1 = sign > 0 ? nPositiveStart : nNegativeStart; // either first positive index (e.g. 0) or first
        // negative (n/2)
        final int n2 = sign > 0 ? nNegativeStart : ordinates.getTotalNodes(); // either first negative index (n/2) or
        // n
        final int n3 = ordinates.getTotalNodes() - n2; // either nNegativeStart or 0
        final int nH = n2 - n1;

        var error = new double[nH];
        var iOutward = new double[nH];
        var iInward = new double[nH];

        int stages = tableau.numberOfStages();

        var q = new double[nH][stages]; // first index - cosine node, second index - stage

        double bDotQ;
        double sum;

        int increment = (int) (1 * sign);

        /*
		 * RK Explicit (Embedded)
         */

 /*
		 * First stage
         */
        if (tableau.isFSAL() && !isFirstRun()) { // if FSAL

            for (int l = n1; l < n2; l++) {
                q[l - n1][0] = quantities.getQLast(l - n1); // assume first stage is the last stage of last step
            }

        } else { // if not FSAL or on first run

            for (int l = n1; l < n2; l++) {
                q[l - n1][0] = derivative(l, j, t, quantities.getIntensity(j, l));
            }

            setFirstRun(false);

        }

        // in any case
        for (int l = n1; l < n2; l++) {
            quantities.setDerivative(j, l, q[l - n1][0]); // store derivative for inward intensities
            error[l - n1] = (tableau.getInterpolator().get(0) - tableau.getEstimator().get(0)) * q[l - n1][0] * hSigned;
        }

        /*
		 * Next stages
         */
        for (int m = 1; m < stages; m++) { // <------- STAGES (1...s)

            /*
			 * Calculate interpolated (OUTWARD and INWARD) intensities at each stage from m
			 * = 1 onwards
             */
            double tm = t + hSigned * tableau.getC().get(m); // interpolation point for stage m

            for (int l = n1; l < n2; l++) { // find unknown intensities (sum over the outward intensities)

                /*
		 * OUTWARD
                 */
                sum = tableau.getMatrix().get(m, 0) * q[l - n1][0];
                for (int k = 1; k < m; k++) {
                    sum += tableau.getMatrix().get(m, k) * q[l - n1][k];
                }

                iOutward[l - n1] = quantities.getIntensity(j, l) + hSigned * sum; // outward intensities are simply
                // found from the
                // RK explicit expressions

                /*
				 * INWARD
                 */
                hermite.y0 = quantities.getIntensity(j, l + n3);
                hermite.y1 = quantities.getIntensity(j + increment, l + n3);
                hermite.d0 = quantities.getDerivative(j, l + n3);
                hermite.d1 = quantities.getDerivative(j + increment, l + n3);

                iInward[l - n1] = hermite.interpolate(tm); // inward intensities are interpolated with
                // Hermite polynomials

            }

            /*
			 * Derivatives and associated errors at stage m
             */
            for (int l = n1; l < n2; l++) {
                q[l - n1][m] = derivative(l, tm, iOutward, iInward, n1, n2);
                quantities.setQLast(l - n1, q[l - n1][m]);
                error[l - n1] += (tableau.getInterpolator().get(m) - tableau.getEstimator().get(m)) * q[l - n1][m]
                        * hSigned;
            }

        }

        double[] Is = new double[nH];

        /*
		 * Value at next step
         */
        for (int l = 0; l < nH; l++) {
            bDotQ = tableau.getInterpolator().dot(new Vector(q[l]));
            Is[l] = quantities.getIntensity(j, l + n1) + bDotQ * hSigned;
        }

        return new Vector[]{new Vector(Is), new Vector(error)};

    }

    public ButcherTableau getButcherTableau() {
        return tableau;
    }

    public void setButcherTableau(ButcherTableau coef) {
        this.tableau = coef;
        this.firePropertyChanged(this, tableauSelector);
    }

    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(tableauSelector);
        return list;
    }

    @Override
    public String toString() {
        return super.toString() + " ; " + tableau;
    }

    public DiscreteSelector<ButcherTableau> getTableauSelector() {
        return tableauSelector;
    }

}
