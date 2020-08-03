package pulse.search;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.properties.Flag;

/**
 * An interface for dealing with optimisation variables. The variables are
 * collected in {@code IndexedVector}s according to the pattern set up by a list
 * of {@code Flag}s.
 */

public interface Optimisable {

	/**
	 * Assigns parameter values of this {@code Optimisable} using the optimisation
	 * vector {@code params}. Only those parameters will be updated, the types of
	 * which are listed as indices in the {@code params} vector.
	 * 
	 * @param params the optimisation vector, containing a similar set of parameters
	 *               to this {@code Problem}
	 * @see pulse.util.PropertyHolder.listedTypes()
	 */

	public void assign(IndexedVector params);

	/**
	 * Calculates the vector argument defined on <math><b>R</b><sup>n</sup></math>
	 * to the scalar objective function for this {@code Optimisable}.
	 * 
	 * @param output the output vector where the result will be stored
	 * @param flags  a list of {@code Flag} objects, which determine the basis of
	 *               the search
	 */

	public void optimisationVector(IndexedVector[] output, List<Flag> flags);

}