package pulse.problem.schemes.rte.dom;

import java.io.Serializable;
import pulse.math.linear.Matrices;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.util.Descriptive;

/**
 * The Butcher tableau coefficients used by the explicit Runge-Kutta solvers.
 * Variable names correspond to the standard notations.
 *
 */
public class ButcherTableau implements Descriptive, Serializable {

    private static final long serialVersionUID = -8856270519744473886L;
    private Vector b;
    private Vector bHat;
    private Vector c;
    private SquareMatrix coefs;

    private boolean fsal;
    private String name;

    public final static String DEFAULT_TABLEAU = "BS23";

    public ButcherTableau(String name, double[][] coefs, double[] c, double[] b, double[] bHat, boolean fsal) {

        if (c.length != b.length || c.length != bHat.length) {
            throw new IllegalArgumentException("Check dimensions of the input vectors");
        }

        if (coefs.length != coefs[0].length || coefs.length != c.length) {
            throw new IllegalArgumentException("Check dimensions of the input matrix array");
        }

        this.name = name;
        this.fsal = fsal;

        this.coefs = Matrices.createSquareMatrix(coefs);
        this.c = new Vector(c);
        this.b = new Vector(b);
        this.bHat = new Vector(bHat);
    }

    public int numberOfStages() {
        return b.dimension();
    }

    public SquareMatrix getMatrix() {
        return coefs;
    }

    public void setMatrix(SquareMatrix coefs) {
        this.coefs = coefs;
    }

    public Vector getEstimator() {
        return bHat;
    }

    public void setEstimator(Vector bHat) {
        this.bHat = bHat;
    }

    public Vector getInterpolator() {
        return b;
    }

    public void setInterpolator(Vector b) {
        this.b = b;
    }

    public Vector getC() {
        return c;
    }

    public void setC(Vector c) {
        this.c = c;
    }

    public boolean isFSAL() {
        return fsal;
    }

    @Override
    public String toString() {
        return name;
    }

    public String printTableau() {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < b.dimension(); i++) {

            sb.append(String.format("%n%3.8f | ", c.get(i)));

            for (int j = 0; j < b.dimension(); j++) {
                sb.append(String.format("%3.8f ", coefs.get(i, j)));
            }

        }

        sb.append(System.lineSeparator());

        for (int i = 0; i < b.dimension() + 1; i++) {
            sb.append(String.format("%-12s", "-"));
        }

        sb.append(System.lineSeparator() + String.format("%-10s | ", "-"));

        for (int i = 0; i < b.dimension(); i++) {
            sb.append(String.format("%3.8f ", b.get(i)));
        }

        sb.append(System.lineSeparator() + String.format("%-10s | ", "-"));

        for (int i = 0; i < b.dimension(); i++) {
            sb.append(String.format("%3.8f ", bHat.get(i)));
        }

        return sb.toString();

    }

    @Override
    public String describe() {
        return "Butcher tableau";
    }

}
