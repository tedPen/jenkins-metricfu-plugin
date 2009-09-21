package hudson.plugins.rubyMetrics;

import hudson.model.AbstractBuild;
import hudson.model.HealthReportingAction;
import hudson.model.Result;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.util.Calendar;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.plugins.rubyMetrics.model.MetricFuResults;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;

@SuppressWarnings("unchecked")
public class RubyMetricsBuildAction implements HealthReportingAction {

  private final MetricFuResults results;
  protected final AbstractBuild<?, ?> owner;

  protected RubyMetricsBuildAction(AbstractBuild<?, ?> owner, MetricFuResults results) {
    this.owner = owner;
    this.results = results;
  }

  public <T extends RubyMetricsBuildAction> T getPreviousResult() {
    AbstractBuild<?, ?> b = owner;
    while (true) {
      b = b.getPreviousBuild();
      if (b == null) {
        return null;
      }
      if (b.getResult() == Result.FAILURE) {
        continue;
      }
      RubyMetricsBuildAction r = b.getAction(this.getClass());
      if (r != null) {
        return (T) r;
      }
    }
  }

  public AbstractBuild<?, ?> getOwner() {
    return owner;
  }
  public HealthReport getBuildHealth() {
    return null;
  }

  public MetricFuResults getResults() {
    return results;
  }

  public String getDisplayName() {
    return "Ruby Metrics";
  }

  public String getIconFileName() {
    return "graph.gif";
  }

  public String getUrlName() {
    return "rubymetrics";
  }

  protected String getRangeAxisLabel() {
    return "";
  }

  protected DataSetBuilder<String, NumberOnlyBuildLabel> getFlogDataSetBuilder() {
    DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

    for (RubyMetricsBuildAction a = this; a != null; a = a.getPreviousResult()) {
      ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.owner);
      Float flogFloatMethodAverage = Float.parseFloat(a.results.getFlogMethodAverage());

      dsb.add(flogFloatMethodAverage, "Flog average", label);
    }

    return dsb;
  }

  protected DataSetBuilder<String, NumberOnlyBuildLabel> getFlayDataSetBuilder() {
    DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

    for (RubyMetricsBuildAction a = this; a != null; a = a.getPreviousResult()) {
      ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.owner);
      Float flayFloatMethodAverage = Float.parseFloat(a.results.getFlayTotal());

      dsb.add(flayFloatMethodAverage, "Flay average", label);
    }

    return dsb;
  }

  protected DataSetBuilder<String, NumberOnlyBuildLabel> getRcovDataSetBuilder() {
    DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

    for (RubyMetricsBuildAction a = this; a != null; a = a.getPreviousResult()) {
      ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.owner);
      Float rcovCoverageTotal = Float.parseFloat(a.results.getRcovCoverage());

      dsb.add(rcovCoverageTotal, "Rcov coverage", label);
    }

    return dsb;
  }

  public void doFlayGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
    if (ChartUtil.awtProblem) {
      rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
      return;
    }

    Calendar t = owner.getTimestamp();

    if (req.checkIfModified(t, rsp)) {
      return; // up to date
    }

    ChartUtil.generateGraph(req, rsp, createChart(getFlayDataSetBuilder().build(), getRangeAxisLabel()), 500, 200);
  }

  public void doFlogGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
    if (ChartUtil.awtProblem) {
      rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
      return;
    }

    Calendar t = owner.getTimestamp();

    if (req.checkIfModified(t, rsp)) {
      return; // up to date
    }

    ChartUtil.generateGraph(req, rsp, createChart(getFlogDataSetBuilder().build(), getRangeAxisLabel()), 500, 200);
  }

  public void doRcovGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
    if (ChartUtil.awtProblem) {
      rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
      return;
    }

    Calendar t = owner.getTimestamp();

    if (req.checkIfModified(t, rsp)) {
      return; // up to date
    }

    ChartUtil.generateGraph(req, rsp, createChart(getRcovDataSetBuilder().build(), getRangeAxisLabel()), 500, 200);
  }

  private JFreeChart createChart(CategoryDataset dataset, String rangeAxisLabel) {

    final JFreeChart chart = ChartFactory.createLineChart(
            null, // chart title
            null, // unused
            rangeAxisLabel, // range axis label
            dataset, // data
            PlotOrientation.VERTICAL, // orientation
            true, // include legend
            true, // tooltips
            false // urls
            );

    // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    final LegendTitle legend = chart.getLegend();
    legend.setPosition(RectangleEdge.RIGHT);

    chart.setBackgroundPaint(Color.white);

    final CategoryPlot plot = chart.getCategoryPlot();

    // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlinePaint(null);
    plot.setRangeGridlinesVisible(true);
    plot.setRangeGridlinePaint(Color.black);

    CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
    plot.setDomainAxis(domainAxis);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
    domainAxis.setLowerMargin(0.0);
    domainAxis.setUpperMargin(0.0);
    domainAxis.setCategoryMargin(0.0);

    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

    final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
    renderer.setBaseStroke(new BasicStroke(2.0f));
    ColorPalette.apply(renderer);

    // crop extra space around the graph
    plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

    return chart;
  }
}
