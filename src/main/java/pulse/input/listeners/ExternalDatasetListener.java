package pulse.input.listeners;

import pulse.input.InterpolationDataset.StandartType;

public interface ExternalDatasetListener {
	public void onDensityDataLoaded(StandartType type);
}