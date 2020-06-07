package pulse.util;

/**
 * A {@code DataEntry} is an immutable ordered pair of an instance of {@code T},
 * which is considered to be the 'key', and an instance of {@code R}, which is
 * considered to be the 'value'.
 * 
 * @param <T> the key
 * @param <R> the value
 */

public class ImmutableDataEntry<T, R> {
	private T key;
	private R value;

	/**
	 * Constructs a new {@code DataEntry} from {@code key} and {@code value}.
	 * 
	 * @param key   the key.
	 * @param value the value associated with this {@code key}.
	 */

	public ImmutableDataEntry(T key, R value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Gets the key object
	 * 
	 * @return the key
	 */

	public T getKey() {
		return key;
	}

	/**
	 * Gets the value object
	 * 
	 * @return the value
	 */

	public R getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "<" + key + " : " + value + ">";
	}

}