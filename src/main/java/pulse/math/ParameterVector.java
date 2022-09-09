package pulse.math;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pulse.math.linear.Vector;
import pulse.properties.NumericProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * A wrapper subclass that assigns {@code ParameterIdentifier}s to specific
 * components of the vector. Used when constructing the optimisation vector.
 */
public class ParameterVector {

    private List<Parameter> params;

    /**
     * Constructs an {@code IndexedVector} with the specified list of keywords.
     *
     * @param indices a list of keywords
     */
    public ParameterVector(List<ParameterIdentifier> indices) {
        params = indices.stream().map(ind
                -> new Parameter(ind)).collect(Collectors.toList());
    }

    /**
     * Constructs an {@code IndexedVector} based on {@code v} and a list of
     * keyword {@code indices}
     *
     * @param proto prototype vector
     * @param v the vector to be copied
     */
    public ParameterVector(ParameterVector proto, Vector v) {
        params = new ArrayList<>();
        var protoParams = proto.params;
        for (Parameter p : protoParams) {
            var pp = new Parameter(p);  //copy
            pp.setValue(v.get(
                    protoParams.indexOf(p))); //set new value
            params.add(pp); //add
        }
    }

    /**
     * Copy constructor
     *
     * @param v another vector
     */
    public ParameterVector(ParameterVector v) {
        params = new ArrayList<>(v.params);
        for (Parameter p : params) {
            p.setValue(p.getApparentValue(), true);
        }
    }

    public void add(Parameter p) {
        params.add(p);
    }

    public double getParameterValue(NumericPropertyKeyword key, int index) {
        return params.stream().filter(p -> {
            var pid = p.getIdentifier();
            return pid.getKeyword() == key && pid.getIndex() == index;
        }
        ).findAny().get().getApparentValue();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Indices: ");
        for (var key : params) {
            sb.append(key.getIdentifier()).append(" ; ");
        }
        sb.append(System.lineSeparator());
        sb.append(" Values: ").append(super.toString());
        return sb.toString();
    }

    /**
     * Finds any elements of this vector which do not pass sanity checks.
     *
     * @return a list of malformed numeric properties
     * @see pulse.properties.NumericProperties.isValueSensible()
     */
    public List<NumericProperty> findMalformedElements() {
        var list = new ArrayList<NumericProperty>();

        params.stream().filter(p -> (p.getIdentifier().getKeyword() != null))
                .map(p -> NumericProperties.derive(p.getIdentifier().getKeyword(),
                p.inverseTransform()))
                .filter(property -> (!property.validate()))
                .forEachOrdered(property -> {
                    list.add(property);
                });

        return list;
    }

    public void setValues(Vector v) {
        int dim = v.dimension();
        if (dim != this.dimension()) {
            throw new IllegalArgumentException("Illegal vector dimension: "
                    + dim + " != " + this.dimension());
        }
        
        for(int i = 0; i < dim; i++) {
            params.get(i).setValue(v.get(i));
        }
                
    }

    public int dimension() {
        return params.size();
    }

    public List<Parameter> getParameters() {
        return params;
    }

    public Vector toVector() {
        return new Vector(params.stream().mapToDouble(p -> p.inverseTransform()).toArray());
    }

}
