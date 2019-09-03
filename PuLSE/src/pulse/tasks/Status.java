package pulse.tasks;

import java.awt.Color;

public enum Status {
	
	INCOMPLETE(Color.RED), READY(Color.MAGENTA), IN_PROGRESS(Color.DARK_GRAY), DONE(Color.BLUE), 
	EXECUTION_ERROR(Color.red), TERMINATED(Color.DARK_GRAY), QUEUED(Color.GREEN), AMBIGUOUS(Color.GRAY), TIMEOUT(Color.RED); 
	
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
	
	public enum Details {
		MISSING_PROBLEM_STATEMENT, MISSING_DIFFERENCE_SCHEME, MISSING_HEATING_CURVE, MISSING_LINEAR_SOLVER, 
		MISSING_PATH_SOLVER, MISSING_BUFFER, INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT, LIMITED_HEAT_LOSSES;
		
		@Override
		public String toString() {
			return parse(super.toString());
		}
		
	}
	
}
