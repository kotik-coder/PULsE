package pulse.search.direction.pso;

import pulse.math.ParameterVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.search.direction.IterativeState;
import pulse.tasks.SearchTask;

public class SwarmState extends IterativeState {

	private ParameterVector seed;

	private Particle[] particles;
	private NeighbourhoodTopology neighborhoodTopology;

	private Particle bestSoFar;
	private int bestSoFarIndex;
	
	private final static int DEFAULT_PARTICLES = 16;
	
	public SwarmState() {
		this(DEFAULT_PARTICLES, StaticTopologies.GLOBAL);
	}
	
	public SwarmState(int numberOfParticles, NeighbourhoodTopology neighborhoodTopology) {
		this.neighborhoodTopology = neighborhoodTopology;
		this.particles = new Particle[numberOfParticles];
		this.bestSoFar = null;
		this.bestSoFarIndex = -1;
	}

	public void evaluate(SearchTask t) throws SolverException {
		for (var p : particles)
			p.evaluate(t);
	}

	public void prepare(SearchTask t) {
		seed = t.searchVector();
	}

	public void create() {
		for (int i = 0; i < particles.length; i++) 
			particles[i] = new Particle(new ParticleState(seed), i);
	}

	/**
	 * Returns the best state achieved by any particle so far.
	 * 
	 * @return State object.
	 */

	public ParticleState bestSoFar() {
		int bestIndex = 0;
		
		double fitness = 0;
		double bestFitness = Double.MAX_VALUE;
		
		for (int i = 0; i < particles.length; i++) {

			fitness = particles[i].getBestState().getFitness();
			
			if (fitness < bestFitness) {
				bestIndex	= i;
				bestFitness = fitness;
			}
			
		}

		this.bestSoFar		= particles[bestIndex];
		this.bestSoFarIndex	= bestIndex;

		return bestSoFar.getBestState();
	}

	public NeighbourhoodTopology getNeighborhoodTopology() {
		return neighborhoodTopology;
	}

	public void setNeighborhoodTopology(NeighbourhoodTopology neighborhoodTopology) {
		this.neighborhoodTopology = neighborhoodTopology;
	}

	/**
	 * Returns the particles of the swarm.
	 * 
	 * @return array of Particles.
	 */

	public Particle[] getParticles() {
		return particles;
	}

	public void setParticles(Particle[] particles) {
		this.particles = particles;
	}

	public Particle getBestSoFar() {
		return bestSoFar;
	}

	public void setBestSoFar(Particle bestSoFar) {
		this.bestSoFar = bestSoFar;
	}

	public int getBestSoFarIndex() {
		return bestSoFarIndex;
	}

	public void setBestSoFarIndex(int bestSoFarIndex) {
		this.bestSoFarIndex = bestSoFarIndex;
	}

}