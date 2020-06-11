package pulse.problem.schemes.rte.dom;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import pulse.io.readers.ReaderManager;

public class OrdinateSet {

	private double[] mu;
	private double[] w;
	private String name;

	private static List<OrdinateSet> allSets;
	private boolean hasZeroNode;

	private int firstPositiveNode, firstNegativeNode;

	static {

		URI uri = null;
		try {
			uri = OrdinateSet.class.getResource("/quadratures/").toURI();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		allSets = ReaderManager.readDirectory(new File(uri)).stream().filter(object -> object instanceof OrdinateSet)
				.map(obj -> (OrdinateSet) obj).collect(Collectors.toList());

	}

	public static OrdinateSet DEFAULT_SET = OrdinateSet.get("G8M");

	public double[] getNodes() {
		return mu;
	}

	public double[] getWeights() {
		return w;
	}

	public OrdinateSet(String name, double[] mu, double[] w) {
		if (mu.length != w.length)
			throw new IllegalArgumentException("Arrays size do not match: " + mu.length + " != " + w.length);

		this.name = name;
		this.mu = mu;
		this.w = w;

		if (!approximatelyEquals(summedWeights(), 2.0))
			throw new IllegalStateException("Summed quadrature weights != 2.0");

		hasZeroNode = Double.compare(mu[0], 0.0) == 0;

		firstPositiveNode = 0;
		firstNegativeNode = mu.length / 2;

		if (hasZeroNode) {
			firstPositiveNode += 1;
			firstNegativeNode += 1;
		}

	}

	private static boolean approximatelyEquals(double a, double b) {
		final double tolerance = 1E-5;
		return Math.abs(a - b) < tolerance;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Quadrature set: " + this.getName());
		sb.append(System.lineSeparator());

		for (int i = 0; i < mu.length; i++)
			sb.append(String.format("%nmu[%1d] = %3.8f; w[%1d] = %3.8f", i, mu[i], i, w[i]));

		return sb.toString();

	}

	public static OrdinateSet get(String name) {
		var optional = allSets.stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst();
		if (optional.isEmpty())
			throw new IllegalArgumentException("Quadrature not found: " + name);

		return optional.get();
	}

	private double summedWeights() {
		double sum = 0;
		for (int i = 0; i < w.length; i++)
			sum += w[i];
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

}