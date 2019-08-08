package pulse.ui.charts;

import org.leores.plot.JGnuplot;
import org.leores.plot.JGnuplot.Plot;
import org.leores.util.data.DataTableSet;

import pulse.tasks.TaskManager;

public class PreviewPlot {
	
	private final static int PREVIEW_PLOT_WIDTH = 640;
	private final static int PREVIEW_PLOT_HEIGHT = 480;	
	
	private PreviewPlot() {}	
	
	/*
	 * PREVIEW PLOTS USING JavaGnuplotHybrid
	 */
	
	public static void preview(String xLabel, String yLabel, double[] x, double[] y, double[] xerr, double[] yerr) {	
		JGnuplot jg = new JGnuplot() {
			{
				terminal = "wxt size " + PREVIEW_PLOT_WIDTH + "," + PREVIEW_PLOT_HEIGHT + " enhanced font 'Verdana,10' persist";
			}
		};
		
		Plot plot = new Plot( "Preview plot") {
			{
				xlabel = gnuplotLabelFromHTML(xLabel);
				ylabel = gnuplotLabelFromHTML(yLabel);
			}
		};
		
		jg.plotx = "$header$\n plot '-' title info2(1,1) with xyerrorbars pt 7 ps 2"
				+ ", '-' title info2(1,2) lw 1.5 smooth sbezier \n $data(1,2d)$";
		
		DataTableSet dts = plot.addNewDataTableSet("2D Plot");
		if((xerr != null) && (yerr != null))
			dts.addNewDataTable("Calculated Points (" + TaskManager.getSampleName() + ")", x, y, xerr, yerr);
		else
			dts.addNewDataTable("Calculated Points (" + TaskManager.getSampleName() + ")", x, y);
		dts.addNewDataTable("Bezier Approximation", x, y);
		
		Thread plottingThread = new Thread( new Runnable() {

			@Override
			public void run() {
				jg.execute(plot, jg.plotx);
			}
			
		});
		
		plottingThread.start();
		
	}
	
	private static String gnuplotLabelFromHTML(String propertyName) {
		String subReplace = propertyName.replace("<sub>", "_{");
		String subsubReplace = subReplace.replace("</sub>", "}");
		String supReplace = subsubReplace.replace("<sup>", "^{");
		String supsupReplace = supReplace.replace("</sup>", "}");
		return supsupReplace.replaceAll("\\<[^>]*>","");
	}
	
}
