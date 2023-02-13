package pulse.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class FunctionSerializer {

    private FunctionSerializer() {
        //empty
    }

    public static void writeSplineFunction(PolynomialSplineFunction f, ObjectOutputStream oos)
            throws IOException {
        // write the object
        double[] knots = f != null ? f.getKnots() : null;
        PolynomialFunction[] funcs = f != null ? f.getPolynomials() : null;
        oos.writeObject(knots);
        oos.writeObject(funcs);
    }

    public static PolynomialSplineFunction readSplineFunction(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        var knots = (double[]) ois.readObject(); // knots
        var funcs = (PolynomialFunction[]) ois.readObject();
        return knots != null & funcs != null ? new PolynomialSplineFunction(knots, funcs) : null;
    }

}
