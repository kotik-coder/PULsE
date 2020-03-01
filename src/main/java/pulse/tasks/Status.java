package pulse.tasks;

import java.awt.Color;

/**
 * An enum that represents the different states in which a {@code SearchTask} can be.
 *
 */

public enum Status {
	
	/**
	 * Not all necessary details have been uploaded to a {@code SearchTask} and 
	 * that it cannot be executed yet.
	 */
	
	INCOMPLETE(Color.RED),
	
	/**
	 * Everything seems to be in order and the task can now be executed.
	 */
	
	READY(Color.MAGENTA), 
	
	/**
	 * The task is being executed.
	 */
	
	IN_PROGRESS(Color.DARK_GRAY), 
	
	/**
	 * Task successfully finished. 
	 */
	
	DONE(Color.BLUE),
	
	/**
	 * An error has occurred during execution.
	 */
	
	EXECUTION_ERROR(Color.red), 
	
	/**
	 * The task has been terminated by the user.
	 */
	
	TERMINATED(Color.DARK_GRAY),
	
	/**
	 * Task has been queued and is waiting to be executed.
	 */
	
	QUEUED(Color.GREEN), 
	
	/**
	 * Task has finished, but the results cannot be considered reliable (perhaps, due to 
	 * large scatter of data points).
	 */
	
	AMBIGUOUS(Color.GRAY), 
	
	/**
	 * The iteration limit has been reached and the task aborted.
	 */
	
	TIMEOUT(Color.RED),
	
	/**
	 * Task has finished without errors, however failing to meet a statistical criterion.  
	 */
	
	FAILED(Color.RED);
	
	private final Color clr;
	private Details details = Details.MISSING_PROBLEM_STATEMENT;
	
	Status(Color clr) {
		this.clr = clr;
	}
	
	public final Color getColor() {
		return clr;
	}
	
	public Details getDetails() {
		return this == INCOMPLETE ? details : null;
	}
	
	public void setDetails(Details details) {
		this.details = details;
	}
	
	private static String parse(String str) {
		String[] tokens = str.split("_");
		StringBuilder sb = new StringBuilder();
		final char BLANK_SPACE = ' ';
		
		for(String t : tokens) {
			sb.append(t.toLowerCase());
			sb.append(BLANK_SPACE);
		}
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return parse(super.toString());
	}
	
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(toString());
		if(details != null) 
			 sb.append(" : ").append(details.toString());
		return sb.toString();
	}
	
	/**
	 * An enum which lists different possible problems wit the {@code SearchTask}.
	 *
	 */
	
	public enum Details {
		
		/**
		 * The {@code Problem} has not been specified by the user.
		 */
		
		MISSING_PROBLEM_STATEMENT, 
		
		/**
		 * The {@code DifferenceScheme} for solving the {@code Problem} has not been specified by the user.
		 */
		
		MISSING_DIFFERENCE_SCHEME, 
		
		/**
		 * A heating curve has not been set up for the {@code DifferenceScheme}.
		 */
		
		MISSING_HEATING_CURVE, 
		
		/**
		 * No information can be found about the selected path solver.
		 */
		
		MISSING_LINEAR_SOLVER,
		
		/**
		 * There is no information about the selected path solver. 
		 */
		
		MISSING_PATH_SOLVER, 
		
		/**
		 * The buffer has not been created.
		 */
		
		MISSING_BUFFER, 
		
		/**
		 * Some data is missing in the problem statement. Probably, the interpolation datasets
		 * have been set up incorrectly or the specific heat and density data have not been loaded. 
		 */
		
		INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT;
		
		@Override
		public String toString() {
			return parse(super.toString());
		}
		
	}
	
}