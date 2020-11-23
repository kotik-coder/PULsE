package pulse.search.direction.pso;

import pulse.math.ParameterVector;
import pulse.tasks.SearchTask;

public class ParticleState {
	
	private ParameterVector position;
	private ParameterVector velocity;
	private double fitness;

	public ParticleState(SearchTask t) {
		randomise(t);
		this.fitness = Double.MAX_VALUE;
	}
	
	public ParticleState(ParticleState another) {
		this.position = new ParameterVector(another.position);
		if(another.velocity != null)
			this.velocity = new ParameterVector(another.velocity);
		this.fitness = another.fitness;
	}
	
	public ParticleState(ParameterVector p, ParameterVector v) {
		this.position = p;
		this.velocity = v;
	}
	
	public boolean isBetterThan(ParticleState s) {
		return this.fitness < s.fitness;
	}

	public void randomise(SearchTask task) {

		position = new ParameterVector( task.searchVector() );

		for (int i = 0, n = position.dimension(); i < n; i++) {
			
			var bounds	= position.getBounds();
			var t		= position.getTransform(i);
			
			double max = t.transform( bounds[i].getMaximum() );
			double min = t.transform( bounds[i].getMinimum() );

			double value = min + Math.random() * ( max - min );
			position.set(i, value);
			
		}

	}

	public ParameterVector getPosition() {
		return this.position;
	}

	public ParameterVector getVelocity() {
		return this.velocity;
	}

	public double getFitness() {
		return this.fitness;
	}

	protected void setFitness(double fitness) {
		this.fitness = fitness;
	}
	
}