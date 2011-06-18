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

    protected String owsService;
    
    public OWSSummaryChartBasePanel(String id, Monitor monitor, String owsService) {
        super(id);
        
        this.owsService = owsService;
        
        Map<String,Integer> data = gatherData(monitor);
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            dataset.setValue(e.getKey(), e.getValue());
        }
        
        JFreeChart chart = 
            ChartFactory.createPieChart(getChartTitle(), dataset,  true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        
        BufferedDynamicImageResource resource = new BufferedDynamicImageResource();
        resource.setImage(chart.createBufferedImage(650,500));
        
        add(new NonCachingImage("chart", resource));

    }

    protected abstract Map<String, Integer> gatherData(Monitor monitor);
    
    protected abstract String getChartTitle();

}
