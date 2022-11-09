/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.system.status.MetricValue;
import org.geoserver.system.status.Metrics;
import org.geoserver.system.status.SystemInfoCollector;
import org.geoserver.web.wicket.DateLabel;
import org.geotools.util.logging.Logging;

/** Panel displaying system resources monitoring values that is refreshed periodically. */
public class RefreshedPanel extends Panel {

    private static final long serialVersionUID = -5616622546856772557L;

    public static final String datePattern = "yyyy-MM-dd HH:mm:ss.SSS";

    public RefreshedPanel(String id) {
        super(id);

        /*
         * Configure panel
         */

        final SystemInfoCollector systemInfoCollector =
                GeoServerExtensions.bean(SystemInfoCollector.class);
        final IModel<List<MetricValue>> metricMdl = Model.ofList(Collections.emptyList());
        final IModel<Boolean> statisticsIModel =
                Model.of(systemInfoCollector.getStatisticsStatus());

        final CheckBox statisticsCheckBox = new CheckBox("statistics", statisticsIModel);
        statisticsCheckBox.add(
                new AjaxFormComponentUpdatingBehavior("click") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        systemInfoCollector.setStatisticsStatus(
                                statisticsCheckBox.getModelObject());
                    }
                });
        statisticsCheckBox.setOutputMarkupId(true);
        add(statisticsCheckBox);

        DateLabel time = new DateLabel("time", Model.of(new Date()), datePattern);
        time.setOutputMarkupId(true);
        add(time);

        ListView<MetricValue> list =
                new ListView<MetricValue>("metrics", metricMdl) {
                    private static final long serialVersionUID = -5654700538264617274L;

                    private int counter;

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        counter = 0;
                    }

                    @Override
                    protected void populateItem(ListItem<MetricValue> item) {
                        item.add(
                                new Label(
                                        "info",
                                        new PropertyModel<MetricValue>(
                                                new MetricValueI18nDescriptionWrapper(
                                                        item.getModel().getObject(), this),
                                                "description")),
                                new Label(
                                        "value",
                                        new PropertyModel<MetricValue>(
                                                item.getModel(), "valueUnit")));
                        if (counter % 2 == 0) {
                            item.add(new AttributeModifier("class", "odd"));
                        } else {
                            item.add(new AttributeModifier("class", "even"));
                        }
                        counter++;
                    }
                };
        list.setOutputMarkupId(true);
        add(list);

        // Check every 5 seconds, or less frequently if metrics collection is slow
        long start = System.currentTimeMillis();
        systemInfoCollector.retrieveAllSystemInfo();
        long complete = System.currentTimeMillis();
        long sampleTime = complete - start;
        Duration updateInternval = Duration.milliseconds(Math.max(5000, sampleTime * 5));

        /*
         * Refresh every seconds
         */
        this.add(
                new AjaxSelfUpdatingTimerBehavior(updateInternval) {
                    private static final long serialVersionUID = -7009847252782601466L;

                    @Override
                    public void onConfigure(Component component) {
                        Metrics metrics = systemInfoCollector.retrieveAllSystemInfo();
                        metricMdl.setObject(metrics.getMetrics());
                        time.setDefaultModel(Model.of(new Date()));
                    }
                });
    }

    /** An internal wrapper for getting optional localization string on description values. */
    private static class MetricValueI18nDescriptionWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER =
                Logging.getLogger(MetricValueI18nDescriptionWrapper.class);

        private final MetricValue value;
        private final Component component;

        public MetricValueI18nDescriptionWrapper(MetricValue value, Component component) {
            super();
            this.value = value;
            this.component = component;
        }

        public String getDescription() {
            String keyValue = formatKeyValue(value);
            LOGGER.log(
                    Level.FINE,
                    "Getting localized name for {0} -> {1}",
                    new Object[] {keyValue, value.getDescription()});
            final String localizedValue =
                    component.getString(keyValue, null, value.getDescription());
            return localizedValue;
        }

        private String formatKeyValue(MetricValue value) {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(scapeKeyString(value.getName().toLowerCase()));
            keyBuilder.append("-");
            keyBuilder.append(scapeKeyString(value.getIdentifier().toLowerCase()));
            return keyBuilder.toString();
        }

        private String scapeKeyString(String value) {
            return value.replace(" ", "_").replace(":", "_").replace("=", "_");
        }
    }
}
