package pulse.tasks;

import java.awt.Color;

public enum Status {
	
	INCOMPLETE(Color.RED), READY(Color.MAGENTA), IN_PROGRESS(Color.DARK_GRAY), DONE(Color.BLUE), 
	EXECUTION_ERROR(Color.red), TERMINATED(Color.DARK_GRAY), QUEUED(Color.GREEN); 
	
	private final Color clr;
	private Details details = null;
	
	Status(Color clr) {
		this.clr = clr;
	}
	
	public final Color getColor() {
		return clr;
	}
	
	public Details getDetails() {
		return details;
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
		if(details != null)
			return parse(super.toString() + " : " + details.toString());
		else return toString();
	}
	
	public enum Details {
		MISSING_PROBLEM_STATEMENT, MISSING_DIFFERENCE_SCHEME, MISSING_HEAT_CURVE, MISSING_LINEAR_SOLVER, 
		MISSING_PATH_SOLVER, MISSING_BUFFER, INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT, LIMITED_HEAT_LOSSES; 		
	}
	
}
