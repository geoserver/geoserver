/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;

/**
 * Drop down choice widget for available JDBC drivers.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCDriverChoice extends DropDownChoice<String> {

    public JDBCDriverChoice(String id) {
        super(id, new JDBCDriverClassNamesModel(), new JDBCDriverRenderer());
    }

    static class JDBCDriverClassNamesModel implements IModel<List<String>> {

        @Override
        public List<String> getObject() {
            List<String> driverClassNames = new ArrayList<String>();
            Enumeration<Driver> e = DriverManager.getDrivers();
            while (e.hasMoreElements()) {
                driverClassNames.add(e.nextElement().getClass().getCanonicalName());
            }
            return driverClassNames;
        }

        @Override
        public void detach() {
            // do nothing
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }
    }

    static class JDBCDriverRenderer extends ChoiceRenderer<String> {
        @Override
        public Object getDisplayValue(String object) {
            return object;
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }
}
