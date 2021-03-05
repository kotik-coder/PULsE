package pulse.search.direction.pso;

import pulse.math.ParameterVector;

public class ParticleState {
	
	private ParameterVector position;
	private ParameterVector velocity;
	private double fitness;

	public ParticleState(ParameterVector cur) {
		randomise(cur);
		this.velocity = new ParameterVector(cur);
		
		//set initial velocity to zero
		for(int i = 0, n = velocity.dimension(); i < n; i++) 
			velocity.set(i, 0.0);
		
		this.fitness = Double.MAX_VALUE;
	}
	
	public ParticleState(ParticleState another) {
		this.position = new ParameterVector(another.position);
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

	public void randomise(ParameterVector pos) {
		
		this.position = new ParameterVector(pos);
		
		for (int i = 0, n = position.dimension(); i < n; i++) {
			
			var bounds	= position.getBounds();
			
			double max = bounds[i].getMaximum();
			double min = bounds[i].getMinimum();
			
			double value = min + Math.random() * ( max - min );
			position.set(i, value );
			
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