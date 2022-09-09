package pulse.problem.statements.model;

import pulse.util.Descriptive;
import pulse.util.Reflexive;

public abstract class Gas implements Reflexive, Descriptive {
    
    private double conductivity;
    private double thermalMass;
    private final int atoms;
    private final double mass;
    
    /**
     * Universal gas constant.
     */
    
    public final static double R = 8.314; //J/K/mol
    
    private final static double ROOM_TEMPERATURE = 300;
    private final static double NORMAL_PRESSURE = 1E5;
    
    public Gas(int atoms, double atomicWeight) {
        evaluate(ROOM_TEMPERATURE, NORMAL_PRESSURE);
        this.atoms = atoms;
        this.mass = atoms * atomicWeight/1e3;
    }
    
    public final void evaluate(double temperature, double pressure) {
        this.conductivity = thermalConductivity(temperature);
        this.thermalMass = cp() * density(temperature, pressure);
    }
    
    public final void evaluate(double temperature) {
        evaluate(temperature, NORMAL_PRESSURE);
    }

    public final double thermalDiffusivity() {
        return conductivity/thermalMass;
    }
    
    public abstract double thermalConductivity(double t);
    
    public double cp() {
        return (1.5 + atoms) * R / mass;
    }
    
    public double density(double temperature, double pressure) {
        return pressure * mass / (R * temperature);
    }
    
    public double getThermalMass() {
        return thermalMass;
    }
    
    public double getConductivity() {
        return conductivity;
    } 
    
    public double getNumberOfAtoms() {
        return atoms;
    }
    
    public double getMolarMass() {
        return mass;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(String.format(" : conductivity = %3.4f; thermal mass = %3.4f; ", conductivity, thermalMass));
        sb.append(String.format("atoms per molecule = %d; atomic weight = %1.4f", atoms, mass));
        return sb.toString();
    }
    
}