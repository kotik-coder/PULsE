package pulse.search.direction.pso;

public class ParticleSwarmOptimiser 
	//extends PathOptimiser 
		{

	private SwarmState swarmState;
	private Mover mover;
	
	public ParticleSwarmOptimiser() {
		swarmState = new SwarmState();
		mover	   = new FIPSMover();
	}

	protected void moveParticles() {
		var topology = swarmState.getNeighborhoodTopology();
		for (var p : swarmState.getParticles()) 
			p.adopt( mover.attemptMove( p, topology.neighbours(p, swarmState) ) );
	}

	/**
	 * Iterates the swarm.
	 * 
	 * @param max_iterations max number of iterations to be computed by the swarm.
	 */

	/*
	@Override
	public boolean iteration(SearchTask task) throws SolverException {
		this.prepare(task);

		swarmState.evaluate(task);
		moveParticles();

		swarmState.incrementStep();
		
		task.assign( swarmState.bestSoFar().getPosition() );
		task.solveProblemAndCalculateCost();
		
		return true;
	}
	*/

	/*
	@Override
	public void prepare(SearchTask task) throws SolverException {
		swarmState.prepare(task);
	}

	@Override
	public IterativeState initState(SearchTask t) {
		swarmState.prepare(t);
		swarmState.create();
		return swarmState;
	}
	*/

}