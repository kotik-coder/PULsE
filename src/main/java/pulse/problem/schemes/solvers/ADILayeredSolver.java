package pulse.problem.schemes.solvers;

import java.util.HashMap;

import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.LayeredGrid2D;
import pulse.problem.schemes.Partition;
import pulse.problem.schemes.Partition.Location;
import pulse.problem.statements.CoreShellProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class ADILayeredSolver extends ADIScheme implements Solver<CoreShellProblem> {

	private final static NumericProperty SHELL_GRID_DENSITY = NumericProperty
			.derive(NumericPropertyKeyword.SHELL_GRID_DENSITY, 10);

	public ADILayeredSolver() {
		super();
		initGrid(getGrid().getGridDensity(), SHELL_GRID_DENSITY, getGrid().getTimeFactor());
	}

	public ADILayeredSolver(NumericProperty nCore, NumericProperty nShell, NumericProperty timeFactor) {
		initGrid(nCore, nShell, timeFactor);
	}

	public ADILayeredSolver(NumericProperty nCore, NumericProperty nShell, NumericProperty timeFactor,
			NumericProperty timeLimit) {
		setTimeLimit(timeLimit);
		initGrid(nCore, nShell, timeFactor);
	}

	public void initGrid(NumericProperty nCore, NumericProperty nShell, NumericProperty timeFactor) {
		var map = new HashMap<Location, Partition>();
		map.put(Location.CORE_X, new Partition((int) nCore.getValue(), 1.0, 0.5));
		map.put(Location.CORE_Y, new Partition((int) nCore.getValue(), 1.0, 0.0));
		map.put(Location.FRONT_Y, new Partition((int) nShell.getValue(), 1.0, 0.0));
		map.put(Location.REAR_Y, new Partition((int) nShell.getValue(), 1.0, 0.0));
		map.put(Location.SIDE_X, new Partition((int) nShell.getValue(), 1.0, 0.0));
		map.put(Location.SIDE_Y, new Partition((int) nShell.getValue(), 1.0, 0.0));
		setGrid(new LayeredGrid2D(map, timeFactor));
		getGrid().setTimeFactor(timeFactor);
	}

	private void prepareGrid(CoreShellProblem problem) {
		var layeredGrid = (LayeredGrid2D) getGrid(); // TODO
		layeredGrid.getPartition(Location.FRONT_Y).setGridMultiplier(problem.axialFactor());
		layeredGrid.getPartition(Location.REAR_Y).setGridMultiplier(problem.axialFactor());
		layeredGrid.getPartition(Location.SIDE_X).setGridMultiplier(problem.radialFactor());
	}

	@Override
	public void solve(CoreShellProblem problem) {
		prepareGrid(problem);

		// TODO

	}

	@Override
	public DifferenceScheme copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Problem> domain() {
		return CoreShellProblem.class;
	}

	@Override
	public double signal() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void timeStep(int m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finaliseStep() {
		// TODO Auto-generated method stub
		
	}

}