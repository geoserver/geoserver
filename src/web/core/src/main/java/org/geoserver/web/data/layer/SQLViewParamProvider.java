/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.jdbc.VirtualTable;
import org.geotools.util.logging.Logging;

public class SQLViewParamProvider extends GeoServerDataProvider<Parameter> {

    /** serialVersionUID */
    private static final long serialVersionUID = 4823593149295419810L;

    private static final String DEFAULT_REGEXP = "^[\\w\\d\\s]+$";

    static final Logger LOGGER = Logging.getLogger(SQLViewParamProvider.class);

    List<Parameter> parameters = new ArrayList<Parameter>();

    static final Property<Parameter> NAME = new BeanProperty<Parameter>("name", "name");

    static final Property<Parameter> DEFAULT_VALUE =
            new BeanProperty<Parameter>("defaultValue", "defaultValue");

    static final Property<Parameter> REGEXP = new BeanProperty<Parameter>("regexp", "regexp");

    public SQLViewParamProvider() {
        setEditable(true);
    }

    @Override
    protected List<Parameter> getItems() {
        return parameters;
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<Parameter>>
            getProperties() {
        return Arrays.asList(NAME, DEFAULT_VALUE, REGEXP);
    }

    public void init(VirtualTable vt) {
        parameters.clear();
        for (String name : vt.getParameterNames()) {
            parameters.add(new Parameter(vt.getParameter(name)));
        }
    }

    /** Adds the parameters found in the sql definition */
    public void refreshFromSql(String sql) {
        Pattern p = Pattern.compile("%[\\w\\d\\s]+%");
        Matcher matcher = p.matcher(sql);
        Set<String> paramNames = new HashSet<String>();
        while (matcher.find()) {
            paramNames.add(matcher.group().replace('%', ' ').trim());
        }
        // remove the old ones
        parameters.clear();
        // add the new ones
        for (String name : paramNames) {
            parameters.add(new Parameter(name, null, DEFAULT_REGEXP));
        }
    }

    public void updateVirtualTable(VirtualTable vt) {
        for (String name : vt.getParameterNames()) {
            vt.removeParameter(name);
        }
        for (Parameter param : parameters) {
            vt.addParameter(param.toVirtualTableParameter());
        }
    }

    public void addParameter() {
        parameters.add(new Parameter(null, null, DEFAULT_REGEXP));
    }

    public void removeAll(List<Parameter> params) {
        parameters.removeAll(params);
    }
}
