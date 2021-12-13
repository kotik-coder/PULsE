package pulse.problem.statements.model;

import pulse.properties.NumericPropertyKeyword;

public enum SpectralRange {
    LASER("Laser Absorption"), THERMAL("Thermal Radiation Absorption");

    String name;

    SpectralRange(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public NumericPropertyKeyword typeOfAbsorption() {
        return this == SpectralRange.LASER ? NumericPropertyKeyword.LASER_ABSORPTIVITY
                : NumericPropertyKeyword.THERMAL_ABSORPTIVITY;
    }

}
