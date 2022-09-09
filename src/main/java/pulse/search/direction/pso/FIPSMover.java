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
    public ParticleState attemptMove(Particle p, Particle[] neighbours, ParticleState gBest) {
        var current = p.getCurrentState();
        var curPos = current.getPosition();
        var curPosV = curPos.toVector();
        
        final int n = curPos.dimension();
        final double nLength = (double) neighbours.length;

        Vector nsum = new Vector(n); 

        for (var neighbour : neighbours) {
            var nBestPos = neighbour.getBestState().getPosition();  //best position ever achieved so far by the neighbour
            nsum = nsum.sum(Vector.random(n, 0.0, phi/nLength)
                    .multComponents(nBestPos.toVector().subtract(curPosV))
            );
        }

        var newVelocity = (current.getVelocity().toVector().sum(nsum)).multiply(chi);
        var newPosition = curPosV.sum(newVelocity);
       
        return new ParticleState(
                new ParameterVector(curPos, newPosition),
                new ParameterVector(curPos, newVelocity));

    }

}