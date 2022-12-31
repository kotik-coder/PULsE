package pulse.ui.components;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;

import javax.swing.UIManager;
import org.jfree.chart.ChartFactory;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import static org.jfree.chart.plot.PlotOrientation.VERTICAL;
import org.jfree.chart.plot.XYPlot;

public abstract class AuxPlotter<T> {
    
    private ChartPanel chartPanel;
    private JFreeChart chart;
    private XYPlot plot;
    
    public AuxPlotter() {
        //empty
    }
    
    public AuxPlotter(String xLabel, String yLabel) {
        setChart( ChartFactory.createScatterPlot("", xLabel, yLabel, null, VERTICAL, true, true, false) );
      
        setPlot( chart.getXYPlot() );
        chart.removeLegend();

        setFonts();
    }
    
    public final void setFonts() {
        var jlabel = new JLabel();
        var label = jlabel.getFont().deriveFont(20f);
        var ticks = jlabel.getFont().deriveFont(16f);
        chart.getTitle().setFont(jlabel.getFont().deriveFont(20f));
        
        if (plot instanceof CombinedDomainXYPlot) {
            var combinedPlot = (CombinedDomainXYPlot) plot;
            combinedPlot.getSubplots().stream().forEach(sp -> setFontsForPlot((XYPlot)sp, label, ticks));
        } else {
            setFontsForPlot(plot, label, ticks);            
        }
        
    }
    
    private void setFontsForPlot(XYPlot p, Font label, Font ticks) {
        var foreColor = UIManager.getColor("Label.foreground");
        var domainAxis = p.getDomainAxis();
        Chart.setAxisFontColor(domainAxis, foreColor);        
        var rangeAxis = p.getRangeAxis();
        Chart.setAxisFontColor(rangeAxis, foreColor);
    }
      
    public abstract void plot(T t);
    
    public final ChartPanel getChartPanel() {
        return chartPanel;
    }
    
    public final JFreeChart getChart() {
        return chart;
    }
    
    public final XYPlot getPlot() {
        return plot;
    }
    
    public final void setPlot(XYPlot plot) {
        this.plot = plot;
        plot.setBackgroundPaint(chart.getBackgroundPaint());
    }
    
    public final void setChart(JFreeChart chart) {
        this.chart = chart;
        var color = UIManager.getLookAndFeelDefaults().getColor("TextPane.background");
        chart.setBackgroundPaint(color);
        chartPanel = new ChartPanel(chart);
        this.plot = chart.getXYPlot();
    }
    
}