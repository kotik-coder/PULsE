package pulse.problem.schemes.rte.dom;

import static pulse.math.MathUtils.approximatelyEquals;

import java.util.Arrays;
import java.util.Set;

import pulse.io.readers.QuadratureReader;
import pulse.io.readers.ReaderManager;
import pulse.properties.Property;

public class OrdinateSet implements Property {

	private double[] mu;
	private double[] w;

	private int firstPositiveNode;
	private int firstNegativeNode;
	private int totalNodes;

	private static Set<OrdinateSet> allOptions = ReaderManager.load(QuadratureReader.getInstance(), "/quadratures/",
			"Quadratures.list");
	private final static String DEFAULT_SET = "G8M";

	private String name;

	public OrdinateSet(String name, double[] mu, double[] w) {
		if (mu.length != w.length)
			throw new IllegalArgumentException("Arrays sizes do not match: " + mu.length + " != " + w.length);

		setName(name);
		this.mu = mu;
		this.w = w;
		totalNodes = mu.length;

		checkWeights();

		firstPositiveNode = hasZeroNode() ? 1 : 0; //zero node should always be the first one
		firstNegativeNode = totalNodes / 2 + (hasZeroNode() ? 1 : 0);
		
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public String printOrdinateSet() {
		var sb = new StringBuilder();

		sb.append("Quadrature set: " + this.getName());
		sb.append(System.lineSeparator());

		for (int i = 0; i < mu.length; i++) {
			sb.append(String.format("%nmu[%1d] = %3.8f; w[%1d] = %3.8f", i, mu[i], i, w[i]));
		}

		return sb.toString();

	}

	public boolean hasZeroNode() {
		return Arrays.stream(mu).anyMatch(Double.valueOf(0.0)::equals);
	}

	public int getFirstPositiveNode() {
		return firstPositiveNode;
	}

	public int getFirstNegativeNode() {
		return firstNegativeNode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return "Ordinate set";
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

	public static OrdinateSet find(String name) {
		var optional = allOptions.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
		if (optional.isEmpty())
			throw new IllegalArgumentException("Ordinate set not found: " + name);

		return optional.get();
	}

	public static OrdinateSet defaultSet() {
		return find(DEFAULT_SET);
	}

	public static Set<OrdinateSet> getAllOptions() {
		return allOptions;
	}

	public int getNumberOfNodes() {
		return totalNodes;
	}

	public int getTotalNodes() {
		return totalNodes;
	}

	public double getNode(int i) {
		return mu[i];
	}

	public double getWeight(int i) {
		return w[i];
	}

	public int getHalfLength() {
		return firstNegativeNode - firstPositiveNode;
	}

	private void checkWeights() {
		final double sum = Arrays.stream(w).sum();
		if (!approximatelyEquals(sum, 2.0))
			throw new IllegalStateException("Summed quadrature weights != 2.0");
	}

}