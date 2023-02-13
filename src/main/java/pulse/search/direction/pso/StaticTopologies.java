package pulse.search.direction.pso;

import java.util.Arrays;

public class StaticTopologies {

    /**
     * Global best
     *
     */
    public final static NeighbourhoodTopology GLOBAL = (p, state) -> state.getParticles();

    /**
     * Ring topology (1D - lattice)
     */
    public final static NeighbourhoodTopology RING = (p, state) -> {
        var ps = state.getParticles();
        final int i = Arrays.asList(ps).indexOf(p);
        return new Particle[]{ps[i > 0 ? i - 1 : ps.length - 1],
            ps[i + 1 < ps.length ? i + 1 : 0]
        };
    };

    /**
     * Von Neumann topology (square lattice) Condition: if( ( ps.length &
     * (ps.length - 1) ) != 0) throw new IllegalArgumentException("Number of
     * particles: " + ps.length + " is not power of 2");
     */
    public final static NeighbourhoodTopology SQUARE = (p, state) -> {
        var ps = state.getParticles();
        final int i = Arrays.asList(ps).indexOf(p);

        final int latticeParameter = (int) Math.sqrt(ps.length);

        final int row = i / latticeParameter;
        final int column = i - row * latticeParameter;

        final int above = column + (row > 0
                ? (row - 1) * latticeParameter : (latticeParameter - 1) * latticeParameter);

        final int below = column + (row + 1 < ps.length
                ? latticeParameter * (row + 1) : 0);

        final int left = row * latticeParameter + (column > 0 ? column - 1 : ps.length - 1);
        final int right = row * latticeParameter + (column + 1 < ps.length ? column + 1 : 0);

        return new Particle[]{ps[left], ps[right], ps[above], ps[below]};
    };

    private StaticTopologies() {
        //empty
    }

}
