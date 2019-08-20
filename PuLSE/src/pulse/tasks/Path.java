package pulse.tasks;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.PathSolver;
import pulse.search.math.Matrix;
import pulse.search.math.Vector;
import pulse.util.Accessible;

public class Path implements Accessible {
	
	private Vector    direction;
	private Vector    gradient;
	private double    minimumPoint;
	private Matrix    hessian;
	private int		  iteration;
	
	public Path(SearchTask t) {
		reset(t);
	}
	
	public void reset(SearchTask t) {
		this.gradient	= PathSolver.gradient(t);
		hessian 		= new Matrix(PathSolver.activeParameters().size(), 1.0);
		direction		= gradient.dimension() > 1 ? (hessian.invert()).multiply(gradient.invert()) : gradient.invert(); 
		minimumPoint	= 0.0;
	}
	
	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector currentDirection) {
		this.direction = currentDirection;
	}

	public Vector getGradient() {
		return gradient;
	}

	public void setGradient(Vector currentGradient) {
		this.gradient = currentGradient;
	}
	
	public double getMinimumPoint() {
		return minimumPoint;
	}
	
	public void setMinimumPoint(double min) {
		minimumPoint = min;
	}
	
	public Matrix getHessian() {
		return hessian;
	}
	
	public void setHessian(Matrix hes) {
		this.hessian = hes;
	}
	
	public NumericProperty getIteration() {
		return new NumericProperty(iteration, NumericProperty.ITERATION);
	}
	
	public void incrementStep() {
		iteration++;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		return;
	}

}
