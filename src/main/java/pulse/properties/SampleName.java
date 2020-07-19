package pulse.properties;

import java.util.Objects;

public class SampleName implements Property {

	private String name;

	public SampleName() {
		name = "Nameless";
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

		if (!(value instanceof String))
			throw new IllegalArgumentException(
					"Illegal type: " + value.getClass().getSimpleName() + ". String expected.");

		final boolean result = !name.equals(value);
		this.name = (String) value;
		return result;

	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		boolean result = false;

		if (o instanceof SampleName)
			result = name.equals(((SampleName) o).getValue());

		return result;
	}

}