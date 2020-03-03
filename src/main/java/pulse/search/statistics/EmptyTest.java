package pulse.search.statistics;

import pulse.tasks.SearchTask;

public class EmptyTest extends NormalityTest {

	/**
	 * Always returns true
	 */
	
	@Override
	public boolean test(SearchTask task) {
		return true;
	}
	
	@Override
	public String describe() {
		return "Don't test please";
	}

	@Override
	public void evaluate(SearchTask t) {
		//deliberately empty
	}

}