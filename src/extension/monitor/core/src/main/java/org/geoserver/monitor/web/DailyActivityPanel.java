/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.geoserver.monitor.Monitor;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;

public class DailyActivityPanel extends ActivityChartBasePanel {

    private static final long serialVersionUID = -3347402344921524474L;

    public DailyActivityPanel(String id, Monitor monitor) {
        super(id, monitor);
    }

    @Override
    protected Date[] getDateRange() {
        Date now = Calendar.getInstance().getTime();

        Calendar then = Calendar.getInstance();
        then.setTime(now);
        then.set(Calendar.HOUR_OF_DAY, 0);
        then.set(Calendar.MINUTE, 0);
        then.set(Calendar.SECOND, 0);

        return new Date[] {then.getTime(), now};
    };

    @Override
    protected RegularTimePeriod getTimePeriod(Date time) {
        return new Second(time);
        // return new Minute(time);
        // return new Hour(time);
    }

    @Override
    protected String getChartTitle(Date[] range) {
        return "Activity " + new SimpleDateFormat("yyyy-MM-dd").format(range[0]);
    }
}
