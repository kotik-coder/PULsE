package pulse.problem.schemes.rte;

import java.io.Serializable;

/**
 * Used to listed to status updates in {@code RadiativeTransferSolver}
 * subclasses.
 *
 */
public interface RTECalculationListener extends Serializable {

    /**
     * Invoked when a sub-step of the RTE solution has finished.
     *
     * @param status the status of the completed step
     */
    public void onStatusUpdate(RTECalculationStatus status);

}
