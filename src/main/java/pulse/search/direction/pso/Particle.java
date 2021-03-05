/* %%
 * 
 * JPSO
 *
 * Copyright 2006 Jeff Ridder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pulse.search.direction.pso;

import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.SearchTask;

/**
 * Class defining a particle - the basic unit of a swarm.
 */

public class Particle {
	
	private int id;

	private ParticleState current;
	private ParticleState pbest;
	
	private Particle[] neighbours;
	
	public Particle(ParticleState cur, int id) {
		this.id = id;
		current = cur;
		pbest	= new ParticleState(current);
	}
		
	public void adopt(ParticleState state) {
		this.current = state;
	}
	
	public void evaluate(SearchTask t) throws SolverException {
		var params = t.searchVector();
		t.assign( current.getPosition() );
		current.setFitness( t.solveProblemAndCalculateCost() );
		t.assign( params );
		
		if(current.isBetterThan(pbest))
			pbest = new ParticleState(current);
	}

	/**
	 * Returns the current state (position, velocity, fitness) of the particle.
	 * 
	 * @return current state.
	 */
	
	public ParticleState getCurrentState() {
		return current;
	}

	/**
	 * Returns the personal best state ever achieved by the particle.
	 * 
	 * @return personal best state.
	 */
	public ParticleState getBestState() {
		return pbest;
	}

	public int getId() {
		return id;
	}

	public Particle[] getNeighbours() {
		return neighbours;
	}

}