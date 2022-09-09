package pulse.problem.statements;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.Set;

import pulse.input.ExperimentalData;
import pulse.math.Parameter;
import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.SOURCE_GEOMETRIC_FACTOR;
import pulse.ui.Messages;

public class NonlinearProblem extends ClassicalProblem {

    public NonlinearProblem() {
        super();
        setPulse(new Pulse2D());
        setComplexity(ProblemComplexity.MODERATE);
    }

    public NonlinearProblem(NonlinearProblem p) {
        super(p);
        setPulse(new Pulse2D((Pulse2D) p.getPulse()));
    }

    @Override
    public boolean isReady() {
        return getProperties().areThermalPropertiesLoaded();
    }

    @Override
    public void retrieveData(ExperimentalData c) {
        super.retrieveData(c);
        getProperties().setTestTemperature(c.getMetadata().numericProperty(TEST_TEMPERATURE));
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(TEST_TEMPERATURE);
        set.add(SPECIFIC_HEAT);
        set.add(DENSITY);
        set.remove(SPOT_DIAMETER);
        set.remove(SOURCE_GEOMETRIC_FACTOR);
        return set;
    }

    @Override
    public String toString() {
        return Messages.getString("NonlinearProblem.Descriptor");
    }

    public NumericProperty getThermalConductivity() {
        return derive(CONDUCTIVITY, getProperties().thermalConductivity());
    }

    /**
     * 
     * Does the same as super-class method plus updates the laser energy, if needed.
     * @param params
     * @throws pulse.problem.schemes.solvers.SolverException
     * @see pulse.problem.statements.Problem.getPulse()
     * 
     */ 
    
    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        getProperties().calculateEmissivity();

        for (Parameter p : params.getParameters()) {

            double value = p.inverseTransform();
            NumericPropertyKeyword key = p.getIdentifier().getKeyword();

            if (key == LASER_ENERGY) {
                this.getPulse().setLaserEnergy(derive(key, value));
            }

        }
    }
    
    /**
     * 
     * Does the same as super-class method plus extracts the laser energy and stores it in the {@code output}, if needed.
     * @param output
     * @param flags
     * @see pulse.problem.statements.Problem.getPulse()
     * 
     */
    
    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        
        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();

            if(key == LASER_ENERGY) {
                var bounds = Segment.boundsFrom(LASER_ENERGY);
                p.setBounds(bounds);
                p.setTransform(new StickTransform(bounds));
                p.setValue( (double) getPulse().getLaserEnergy().getValue());
            }

        }

    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return ImplicitScheme.class;
    }

    @Override
    public Problem copy() {
        return new NonlinearProblem(this);
    }

}