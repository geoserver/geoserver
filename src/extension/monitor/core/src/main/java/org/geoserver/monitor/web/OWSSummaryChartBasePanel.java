/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.awt.Color;
import java.util.Map;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.monitor.Monitor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

public abstract class OWSSummaryChartBasePanel extends Panel {

    private static final long serialVersionUID = 8914945614020025223L;
    protected String owsService;

    public OWSSummaryChartBasePanel(String id, Monitor monitor, String owsService) {
        super(id);

        this.owsService = owsService;

        Map<String, Integer> data = gatherData(monitor);

        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            dataset.setValue(e.getKey(), e.getValue());
        }

        JFreeChart chart = ChartFactory.createPieChart(getChartTitle(), dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);

        BufferedDynamicImageResource resource = new BufferedDynamicImageResource();
        resource.setImage(chart.createBufferedImage(650, 500));

        add(new NonCachingImage("chart", resource));
    }

    protected abstract Map<String, Integer> gatherData(Monitor monitor);

    protected abstract String getChartTitle();
}
