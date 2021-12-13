package pulse.input.listeners;

import pulse.input.InterpolationDataset.StandartType;

/**
 * A listener associated with the {@code InterpolationDataset} static repository
 * of interpolations.
 *
 */
public interface ExternalDatasetListener {

    /**
     * Triggered when a data {@code type} has been loaded.
     *
     * @param type a type of the dataset, for which an interpolation is created.
     */
    public void onDataLoaded(StandartType type);

}
