/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.control;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.geoserver.security.urlchecker.URLEntry;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/** @author ImranR */
public class URLEntryProvider extends GeoServerDataProvider<URLEntry> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static Property<URLEntry> NAME = new BeanProperty<URLEntry>("name", "name");
    public static Property<URLEntry> DESCRIPTION =
            new BeanProperty<URLEntry>("description", "description");
    public static Property<URLEntry> REGEX_EXPRESSION =
            new BeanProperty<URLEntry>("regexExpression", "regexExpression");
    public static Property<URLEntry> ENABLE = new BeanProperty<URLEntry>("enable", "enable");

    static List<Property<URLEntry>> PROPERTIES =
            Arrays.asList(NAME, DESCRIPTION, REGEX_EXPRESSION, ENABLE);
    List<URLEntry> urlEntry;

    public URLEntryProvider(List<URLEntry> urlEntry) {
        setSort(new SortParam<Object>(NAME.getName(), true));
        this.urlEntry = urlEntry;
    }

    @Override
    protected List<Property<URLEntry>> getProperties() {
        return Collections.unmodifiableList(PROPERTIES);
    }

    @Override
    protected List<URLEntry> getItems() {
        return urlEntry;
    }
}
