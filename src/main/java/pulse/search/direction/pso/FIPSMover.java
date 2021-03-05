package pulse.search.direction.pso;

import pulse.math.ParameterVector;
import pulse.math.linear.Vector;

public class FIPSMover implements Mover {

	private double chi;
	private double phi;
	public final static double DEFAULT_CHI = 0.7298;
	public final static double DEFAULT_PHI = 4.1;
	
	public FIPSMover() {
		chi = DEFAULT_CHI;
		phi = DEFAULT_PHI;
	}
	
	@Override
	public ParticleState attemptMove(Particle p, Particle[] neighbours) {
		var current	= p.getCurrentState();
		
		var pos		= current.getPosition();
		
		final int n	= pos.dimension();
		var nsum	= new Vector(n);
		
		for(var neighbour : neighbours) {
			var nPos 	= neighbour.getCurrentState().getPosition();
			nsum		= nsum.sum( Vector.random(n, 0.0, phi).multComponents( nPos.subtract(pos) ) );
		}
			
		nsum = nsum.multiply(1.0/((double)neighbours.length));
	
		var newVelocity = ( current.getVelocity().sum(nsum) ).multiply(chi);
		var newPosition = pos.sum(newVelocity);
		System.out.println(newPosition);
		
		return new ParticleState( 
				new ParameterVector(pos, newPosition ), 
				new ParameterVector(pos, newVelocity ) );
		
	}

}
