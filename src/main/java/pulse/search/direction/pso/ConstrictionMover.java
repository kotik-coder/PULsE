package pulse.search.direction.pso;

import pulse.math.ParameterVector;
import pulse.math.linear.Vector;

public class ConstrictionMover implements Mover {

    private double c1; //social
    private double c2; //cognitive
    private double chi;
    public final static double DEFAULT_CHI = 0.7298;
    public final static double DEFAULT_C = 1.49618;

    public ConstrictionMover() {
        chi = DEFAULT_CHI;
        c1 = c2 = DEFAULT_C;
    }

    @Override
    public ParticleState attemptMove(Particle p, Particle[] neighbours, ParticleState gBest) {
        var current = p.getCurrentState();
        var curPos = current.getPosition();
        var curPosV = curPos.toVector();

        final int n = curPos.dimension();
        Vector nsum = new Vector(n);

        var localBest = p.getBestState().getPosition();    //best position by local particle
        var localBestV = localBest.toVector();
        var globalBest = gBest.getPosition();               //best position by any particle
        var globalBestV = globalBest.toVector();

        nsum = nsum.sum(Vector.random(n, 0.0, c1)
                .multComponents(localBestV.subtract(curPosV))
        );

        nsum = nsum.sum(Vector.random(n, 0.0, c2)
                .multComponents(globalBestV.subtract(curPosV))
        );

        var newVelocity = (current.getVelocity().toVector().sum(nsum)).multiply(chi);
        var newPosition = curPosV.sum(newVelocity);

        return new ParticleState(
                new ParameterVector(curPos, newPosition),
                new ParameterVector(curPos, newVelocity));
    }

}
