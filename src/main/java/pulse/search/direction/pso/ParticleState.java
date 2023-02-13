package pulse.search.direction.pso;

import pulse.math.ParameterVector;
import pulse.math.linear.Vector;

public class ParticleState {

    private ParameterVector position;
    private ParameterVector velocity;
    private double fitness;

    public ParticleState(ParameterVector cur) {
        randomise(cur);
        this.velocity = new ParameterVector(cur);

        //set initial velocity to zero
        velocity.setValues(new Vector(cur.dimension()));

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

    public final void randomise(ParameterVector pos) {

        double[] randomValues = pos.getParameters().stream().mapToDouble(p -> {
            double min = p.getBounds().getMinimum();
            double max = p.getBounds().getMaximum();
            return min + Math.random() * (max - min);
        }).toArray();

        Vector randomVector = new Vector(randomValues);
        position.setValues(randomVector);
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
