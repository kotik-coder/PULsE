package pulse.math.filters;

import java.io.Serializable;

public interface AssignmentListener extends Serializable {

    public void onValueAssigned();

}