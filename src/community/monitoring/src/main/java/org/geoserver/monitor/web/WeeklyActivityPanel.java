package org.geoserver.monitor.web;

import java.util.Calendar;
import java.util.Date;

import org.geoserver.monitor.Monitor;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.RegularTimePeriod;

public class WeeklyActivityPanel extends ActivityChartBasePanel {

    public WeeklyActivityPanel(String id, Monitor monitor) {
        super(id, monitor);
    }

    @Override
    protected Date[] getDateRange() {
        Date now = new Date();
        
        Calendar then = Calendar.getInstance();
        then.setTime(now);
        then.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        return new Date[]{then.getTime(), now};
    }

    @Override
    protected RegularTimePeriod getTimePeriod(Date time) {
        return new Hour(time);
    }

}
