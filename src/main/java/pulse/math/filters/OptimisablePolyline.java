package pulse.math.filters;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import pulse.DiscreteInput;
import pulse.math.ParameterVector;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.Optimisable;
import pulse.util.PropertyHolder;

public class OptimisablePolyline extends PropertyHolder implements Optimisable {

    private final double[] x;
    private final double[] y;
    private final List<AssignmentListener> listeners;

    public OptimisablePolyline(List<Point2D> data) {
        x = data.stream().mapToDouble(d -> d.getX()).toArray();
        y = data.stream().mapToDouble(d -> d.getY()).toArray();
        listeners = new ArrayList<>();
    }

    @Override
    public void assign(ParameterVector input) throws SolverException {
        var ps = input.getParameters();
        for(int i = 0, size = ps.size(); i < size; i++) {
            y[i] = ps.get(i).getApparentValue();
        }
        listeners.stream().forEach(l -> l.onValueAssigned());
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        output.setValues(new Vector(y));
    }
    
    public List<Point2D> points() {
        return DiscreteInput.convert(x, y);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public double[] getX() {
        return x;
    }
    
    public double[] getY() {
        return y;
    }
    
    public void addAssignmentListener(AssignmentListener listener) {
        listeners.add(listener);
    }

}