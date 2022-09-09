package pulse.search.direction;

import pulse.math.ParameterVector;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.ITERATION;

import pulse.properties.NumericProperty;
import pulse.search.GeneralTask;

public class IterativeState {

    private ParameterVector parameters;
    private double cost = Double.POSITIVE_INFINITY;
    private int iteration;
    private int failedAttempts;

    /**
     * Stores the parameter vector and cost function value associated with the specified state.
     * @param other another state of the optimiser
     */
    
    public IterativeState(IterativeState other) {
        this.parameters = new ParameterVector(other.parameters);
        this.cost = other.cost;
    }
    
    public IterativeState(GeneralTask t) {
        this.parameters = t.searchVector();
    }
    
    //default constructor
    public IterativeState() {}
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public void reset() {
        iteration = 0;
    }

    public NumericProperty getIteration() {
        return derive(ITERATION, iteration);
    }

    public void incrementStep() {
        iteration++;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void resetFailedAttempts() {
        failedAttempts = 0;
    }

    public void incrementFailedAttempts() {
        failedAttempts++;
    }
    
    public ParameterVector getParameters() {
        return parameters;
    }

    public void setParameters(ParameterVector parameters) {
        this.parameters = parameters;
    }

}