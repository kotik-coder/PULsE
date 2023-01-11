package pulse.properties;

import java.util.Objects;

public class SampleName implements Property {

    private String name;

    public SampleName(String name) {
        this.name = name;
    }

    @Override
    public Object getValue() {
        return name;
    }

    @Override
    public String getDescriptor(boolean addHtmlTags) {
        return "Sample name";
    }

    @Override
    public boolean attemptUpdate(Object value) {
        Objects.requireNonNull(value);

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Illegal type: " + value.getClass().getSimpleName() + ". String expected.");
        }

        final boolean result = !name.equals(value);
        this.name = (String) value;
        return result;

    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SampleName other = (SampleName) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

}