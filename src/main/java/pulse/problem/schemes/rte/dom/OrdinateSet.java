package pulse.problem.schemes.rte.dom;

import java.util.Set;

import pulse.io.readers.QuadratureReader;
import pulse.io.readers.ReaderManager;
import pulse.properties.Property;

public class OrdinateSet implements Property {

	private double[] mu;
	private double[] w;

	private boolean hasZeroNode;
	private int firstPositiveNode;
	private int firstNegativeNode;
	private int totalNodes;

	private static Set<OrdinateSet> allOptions = ReaderManager.load(QuadratureReader.getInstance(), "/quadratures/",
			"Quadratures.list");
	private final static String DEFAULT_SET = "G8M";

	private String name;

	public double[] getNodes() {
		return mu;
	}

	public double[] getWeights() {
		return w;
	}

	public OrdinateSet(String name, double[] mu, double[] w) {
		if (mu.length != w.length)
			throw new IllegalArgumentException("Arrays size do not match: " + mu.length + " != " + w.length);

		setName(name);
		this.mu = mu;
		totalNodes = mu.length;
		this.w = w;

		if (!approximatelyEquals(summedWeights(), 2.0))
			throw new IllegalStateException("Summed quadrature weights != 2.0");

		hasZeroNode = Double.compare(mu[0], 0.0) == 0;

		firstPositiveNode = 0;

		firstNegativeNode = this.mu.length / 2;

		if (hasZeroNode) {
			firstPositiveNode += 1;
			firstNegativeNode += 1;
		}

	}

	private static boolean approximatelyEquals(double a, double b) {
		final double tolerance = 1E-5;
		return Math.abs(a - b) < tolerance;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public String printOrdinateSet() {
		StringBuilder sb = new StringBuilder();

		sb.append("Quadrature set: " + this.getName());
		sb.append(System.lineSeparator());

		for (int i = 0; i < mu.length; i++) {
			sb.append(String.format("%nmu[%1d] = %3.8f; w[%1d] = %3.8f", i, mu[i], i, w[i]));
		}

		return sb.toString();

	}

	private double summedWeights() {
		double sum = 0;
		for (int i = 0; i < w.length; i++) {
			sum += w[i];
		}
		return sum;
	}

	public boolean hasZeroNode() {
		return hasZeroNode;
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

	public static OrdinateSet getDefaultInstance() {
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

}