package pulse.search.direction.pso;

public interface Mover {

    public ParticleState attemptMove(Particle p, Particle[] neighbours);

}
