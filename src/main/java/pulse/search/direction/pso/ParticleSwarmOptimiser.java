package pulse.search.direction.pso;

import pulse.problem.schemes.solvers.SolverException;
import pulse.search.GeneralTask;
import pulse.search.direction.IterativeState;
import pulse.search.direction.PathOptimiser;
import pulse.search.statistics.OptimiserStatistic;

public class ParticleSwarmOptimiser extends PathOptimiser {

    private SwarmState swarmState;
    private Mover mover;

    public ParticleSwarmOptimiser() {
        swarmState = new SwarmState();
        mover = new ConstrictionMover();
    }

    protected void moveParticles() {
        var topology = swarmState.getNeighborhoodTopology();
        for (var p : swarmState.getParticles()) {
            p.adopt(mover.attemptMove(p,
                    topology.neighbours(p, swarmState),
                    swarmState.getBestSoFar()));
            var data = p.getCurrentState().getPosition().toVector().getData();
            StringBuilder sb = new StringBuilder().append(p.getId()).append(" ");
            for (var d : data) {
                sb.append(d).append(" ");
            }
            System.err.println(sb.toString());
        }
    }

    /**
     * Iterates the swarm.
     *
     */
    @Override
    public boolean iteration(GeneralTask task) throws SolverException {
        this.prepare(task);

        swarmState.evaluate(task);
        swarmState.bestSoFar();
        moveParticles();

        swarmState.incrementStep();

        task.assign(swarmState.getBestSoFar().getPosition());
        double cost = task.objectiveFunction();
        swarmState.setCost(cost);

        return true;
    }

    @Override
    public void prepare(GeneralTask task) throws SolverException {
        swarmState.prepare(task);
    }

    @Override
    public IterativeState initState(GeneralTask t) {
        swarmState.prepare(t);
        swarmState.create();
        return swarmState;
    }

    //TODO
    @Override
    public boolean compatibleWith(OptimiserStatistic os) {
        return false;
    }

}
