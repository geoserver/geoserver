/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * https://github.com/opengeospatial/QoSE-DWG/blob/master/codelist/qos/metrics/1.0/metrics.rdf
 *
 * @author Fernando Miño, Geosolutions
 */
public class Metric implements Serializable {

    public static final String URL =
            "http://def.opengeospatial.org/codelist/qos/metrics/1.0/metrics.rdf#";

    private String href;
    private String title;

    public Metric() {}

    public Metric(String href, String title) {
        super();
        this.href = href;
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((href == null) ? 0 : href.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Metric other = (Metric) obj;
        if (href == null) {
            if (other.href != null) return false;
        } else if (!href.equals(other.href)) return false;
        return true;
    }

    public static List<Metric> values() {
        Metric[] mts =
                new Metric[] {
                    new Metric(URL + "ResponseTime", "ResponseTime"),
                    new Metric(URL + "ServicePerformance", "ServicePerformance"),
                    new Metric(URL + "RequestResponsePerformance", "RequestResponsePerformance"),
                    new Metric(URL + "InitialResponsePerformance", "InitialResponsePerformance"),
                    new Metric(
                            URL + "MinimumRequestResponsePerformance",
                            "MinimumRequestResponsePerformance"),
                    new Metric(URL + "ContinuousAvailability", "ContinuousAvailability"),
                    new Metric(URL + "AvailabilityMonthly", "AvailabilityMonthly"),
                    new Metric(URL + "AvailabilityDaily", "AvailabilityDaily"),
                    new Metric(URL + "RequestCapacity", "RequestCapacity"),
                    new Metric(URL + "RequestCapacityPerSecond", "RequestCapacityPerSecond")
                };
        return Arrays.asList(mts);
    }
}
