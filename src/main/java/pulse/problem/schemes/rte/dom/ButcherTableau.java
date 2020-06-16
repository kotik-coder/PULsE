package pulse.problem.schemes.rte.dom;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import pulse.io.readers.ReaderManager;
import pulse.math.Matrix;
import pulse.math.Vector;
import pulse.properties.Property;

public class ButcherTableau implements Property {

	protected Vector b, bHat;
	protected Vector c;
	protected Matrix coefs;

	private boolean fsal;

	private static Set<ButcherTableau> allOptions;
	private String name;

	private final static String DEFAULT_TABLEAU = "BS23";

	static {
		URI uri = null;
		try {
			uri = ButcherTableau.class.getResource("/solvers/").toURI();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		allOptions = ReaderManager.readDirectory(new File(uri)).stream()
				.filter(object -> object instanceof ButcherTableau).map(obj -> (ButcherTableau) obj)
				.collect(Collectors.toSet());
	}

	public ButcherTableau(String name, double[][] coefs, double[] c, double[] b, double[] bHat, boolean fsal) {

		if (c.length != b.length || c.length != bHat.length)
			throw new IllegalArgumentException("Check dimensions of the input vectors");

		if (coefs.length != coefs[0].length || coefs.length != c.length)
			throw new IllegalArgumentException("Check dimensions of the input matrix array");

		setName(name);
		this.fsal = fsal;

		this.coefs = new Matrix(coefs);
		this.c = new Vector(c);
		this.b = new Vector(b);
		this.bHat = new Vector(bHat);
	}

	public int numberOfStages() {
		return b.dimension();
	}

	public Matrix getMatrix() {
		return coefs;
	}

	public void setMatrix(Matrix coefs) {
		this.coefs = coefs;
	}

	public Vector getEstimator() {
		return bHat;
	}

	public void setEstimator(Vector bHat) {
		this.bHat = bHat;
	}

	public Vector getInterpolator() {
		return b;
	}

	public void setInterpolator(Vector b) {
		this.b = b;
	}

	public Vector getC() {
		return c;
	}

	public void setC(Vector c) {
		this.c = c;
	}

	public boolean isFSAL() {
		return fsal;
	}

	public String toString() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String printTableau() {

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < b.dimension(); i++) {

			sb.append(String.format("%n%3.8f | ", c.get(i)));

			for (int j = 0; j < b.dimension(); j++)
				sb.append(String.format("%3.8f ", coefs.get(i, j)));

		}

		sb.append(System.lineSeparator());

		for (int i = 0; i < b.dimension() + 1; i++)
			sb.append(String.format("%-12s", "-"));

		sb.append(System.lineSeparator() + String.format("%-10s | ", "-"));

		for (int i = 0; i < b.dimension(); i++)
			sb.append(String.format("%3.8f ", b.get(i)));

		sb.append(System.lineSeparator() + String.format("%-10s | ", "-"));

		for (int i = 0; i < b.dimension(); i++)
			sb.append(String.format("%3.8f ", bHat.get(i)));

		return sb.toString();

	}

	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return "Butcher tableau";
	}

	@Override
	public Object getValue() {
		return this;
	}

	@Override
	public boolean attemptUpdate(Object value) {
		if (!(value instanceof String))
			return false;
		find((String) value);
		return true;
	}

	public static ButcherTableau find(String name) {
		var optional = allOptions.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
		if (optional.isEmpty())
			throw new IllegalArgumentException("Set element not found: " + name);

		return optional.get();
	}

	public static ButcherTableau getDefaultInstance() {
		return find(DEFAULT_TABLEAU);
	}

	public static Set<ButcherTableau> getAllOptions() {
		return allOptions;
	}

}