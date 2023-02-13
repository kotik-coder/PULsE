package pulse.problem.schemes.rte.dom;

import java.util.List;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.FluxesAndExplicitDerivatives;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ClassicalProblem;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;

/**
 * A class that manages the solution of the radiative transfer equation using
 * the discrete ordinates method. The class provides an interface between the
 * ODE adaptive integrator and the iterative solver, which are used together to
 * solve to RTE.
 *
 */
public class DiscreteOrdinatesMethod extends RadiativeTransferSolver {

    private static final long serialVersionUID = 2881363894773388976L;
    private InstanceDescriptor<AdaptiveIntegrator> integratorDescriptor = new InstanceDescriptor<AdaptiveIntegrator>(
            "Integrator selector", AdaptiveIntegrator.class);
    private InstanceDescriptor<IterativeSolver> iterativeSolverSelector = new InstanceDescriptor<IterativeSolver>(
            "Iterative solver selector", IterativeSolver.class);
    private InstanceDescriptor<PhaseFunction> phaseFunctionSelector = new InstanceDescriptor<PhaseFunction>(
            "Phase function selector", PhaseFunction.class);

    private AdaptiveIntegrator integrator;
    private IterativeSolver iterativeSolver;

    /**
     * Constructs a discrete ordinates solver using the parameters (emissivity,
     * scattering albedo and optical thickness) declared by the {@code problem}
     * object.
     *
     * @param problem the coupled problem statement
     * @param grid the heat problem grid
     */
    public DiscreteOrdinatesMethod(ParticipatingMedium problem, Grid grid) {
        super();
        var properties = (ThermoOpticalProperties) problem.getProperties();
        setFluxes(new FluxesAndExplicitDerivatives(grid.getGridDensity(), properties.getOpticalThickness()));

        var discrete = new Discretisation(properties);

        integratorDescriptor.setSelectedDescriptor(TRBDF2.class.getSimpleName());
        setIntegrator(integratorDescriptor.newInstance(AdaptiveIntegrator.class, discrete));

        iterativeSolverSelector.setSelectedDescriptor(FixedIterations.class.getSimpleName());
        setIterativeSolver(iterativeSolverSelector.newInstance(IterativeSolver.class));

        phaseFunctionSelector.setSelectedDescriptor(HenyeyGreensteinPF.class.getSimpleName());
        initPhaseFunction(properties, discrete);

        init(problem, grid);

        integratorDescriptor.addListener(() -> setIntegrator(
                integratorDescriptor.newInstance(AdaptiveIntegrator.class, discrete))
        );

        iterativeSolverSelector
                .addListener(() -> setIterativeSolver(iterativeSolverSelector.newInstance(IterativeSolver.class)));
        
        phaseFunctionSelector.addListener(() -> initPhaseFunction(properties, discrete));
    }

    @Override
    public RTECalculationStatus compute(double[] tempArray) {
        integrator.getEmissionFunction().setInterpolation(interpolateTemperatureProfile(tempArray));

        var status = iterativeSolver.doIterations(integrator);

        if (status == RTECalculationStatus.NORMAL) {
            fluxesAndDerivatives(tempArray.length);
        }

        fireStatusUpdate(status);
        return status;
    }

    private void fluxesAndDerivatives(final int nExclusive) {
        final var interpolation = integrator.getHermiteInterpolator()
                .interpolateOnExternalGrid(nExclusive, integrator);

        final double DOUBLE_PI = 2.0 * Math.PI;
        final var discrete = integrator.getDiscretisation();
        var fluxes = (FluxesAndExplicitDerivatives) getFluxes();

        for (int i = 0; i < nExclusive; i++) {
            double flux = DOUBLE_PI * discrete.firstMoment(interpolation[0], i);
            fluxes.setFlux(i, flux);
            fluxes.setFluxDerivative(i,
                    -DOUBLE_PI * discrete.firstMoment(interpolation[1], i));
        }

    }

    @Override
    public String getDescriptor() {
        return "Discrete Ordinates Method (DOM)";
    }

    @Override
    public void init(ParticipatingMedium problem, Grid grid) {
        super.init(problem, grid);
        var top = (ThermoOpticalProperties) problem.getProperties();
        initPhaseFunction(top,
                integrator.getDiscretisation());
        integrator.init(problem);
        integrator.getPhaseFunction().init(top);
    }

    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(integratorDescriptor);
        list.add(iterativeSolverSelector);
        list.add(phaseFunctionSelector);
        return list;
    }

    public final AdaptiveIntegrator getIntegrator() {
        return integrator;
    }

    public final InstanceDescriptor<AdaptiveIntegrator> getIntegratorDescriptor() {
        return integratorDescriptor;
    }

    public final void setIntegrator(AdaptiveIntegrator integrator) {
        this.integrator = integrator;
        integrator.setParent(this);
        firePropertyChanged(this, integratorDescriptor);
    }

    public final IterativeSolver getIterativeSolver() {
        return iterativeSolver;
    }

    public final InstanceDescriptor<IterativeSolver> getIterativeSolverSelector() {
        return iterativeSolverSelector;
    }

    public final void setIterativeSolver(IterativeSolver solver) {
        this.iterativeSolver = solver;
        solver.setParent(this);
        firePropertyChanged(this, iterativeSolverSelector);
    }

    public final InstanceDescriptor<PhaseFunction> getPhaseFunctionSelector() {
        return phaseFunctionSelector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " : " + integrator.toString() + " ; " + iterativeSolver.toString();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        // intentionally left blank
    }

    private void initPhaseFunction(ThermoOpticalProperties top, Discretisation discrete) {
        var pf = phaseFunctionSelector.newInstance(PhaseFunction.class, top, discrete);
        integrator.setPhaseFunction(pf);
        pf.init(top);
        firePropertyChanged(this, phaseFunctionSelector);
    }

}
