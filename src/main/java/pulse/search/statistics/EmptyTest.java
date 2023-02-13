package pulse.search.statistics;

import pulse.search.GeneralTask;

public class EmptyTest extends NormalityTest {

    private static final long serialVersionUID = 5919796302195242667L;

    /**
     * Always returns true
     */
    @Override
    public boolean test(GeneralTask task) {
        return true;
    }

    @Override
    public String getDescriptor() {
        return "Don't test please";
    }

    @Override
    public void evaluate(GeneralTask t) {
        // deliberately empty
    }

}
