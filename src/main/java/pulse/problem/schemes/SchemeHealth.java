package pulse.problem.schemes;

public class SchemeHealth {

	private boolean healthy;
	private String details;

	public SchemeHealth() {
		reset();
	}

	public void triggerProblem(String string) {
		healthy = false;
		details = string;
	}

	public void reset() {
		healthy = true;
		details = null;
	}

	public boolean isHealthy() {
		return healthy;
	}

	public String getDetails() {
		return details;
	}

}