package pulse.search.direction.pso;

public interface NeighbourhoodTopology {

	public Particle[] neighbours(Particle p, SwarmState ss); 
	
}