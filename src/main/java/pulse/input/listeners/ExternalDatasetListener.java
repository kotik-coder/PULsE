package pulse.input.listeners;

import java.io.Serializable;
/**
 * A listener associated with the {@code InterpolationDataset} static repository
 * of interpolations.
 *
 */
public interface ExternalDatasetListener {

    public void onSpecificHeatDataLoaded();
    public void onDensityDataLoaded();

}