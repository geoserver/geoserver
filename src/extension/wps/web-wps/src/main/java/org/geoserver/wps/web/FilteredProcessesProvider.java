/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;

/**
 * Provides entries for the process filtering table in the {@link ProcessSelectionPage}
 * 
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class FilteredProcessesProvider extends
        GeoServerDataProvider<FilteredProcessesProvider.FilteredProcess> {

    /**
     * Represents a selectable process in the GUI
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static class FilteredProcess implements Serializable, Comparable<FilteredProcess>{

        private Name name;

        private String description;

        public FilteredProcess(Name name, String description) {
            this.name = name;
            this.description = description;
        }

        public Name getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public int compareTo(FilteredProcess other) {
            if(name == null) {
                return other.getName() == null ? 0 : -1;
            } else if(other.getName() == null) {
                return 1;
            } else {
                return name.getURI().compareTo(other.getName().getURI());
            }
        }

    }

    private ProcessGroupInfo pfi;

    private Locale locale;

    private List<FilteredProcess> selectableProcesses;

    public FilteredProcessesProvider(ProcessGroupInfo pfi, Locale locale) {
        this.pfi = pfi;
        this.locale = locale;
        ProcessFactory pf = GeoServerProcessors.getProcessFactory(pfi.getFactoryClass(), false);
        Set<Name> names = pf.getNames();
        selectableProcesses = new ArrayList<FilteredProcess>();
        for (Name name : names) {
            String description = GeoServerProcessors
                    .getProcessFactory(pfi.getFactoryClass(), false).getDescription(name)
                    .toString(locale);
            FilteredProcess sp = new FilteredProcess(name, description);
            selectableProcesses.add(sp);
        }
        Collections.sort(selectableProcesses);
    }

    @Override
    protected List<Property<FilteredProcess>> getProperties() {
        List<Property<FilteredProcess>> props = new ArrayList<GeoServerDataProvider.Property<FilteredProcess>>();
        props.add(new BeanProperty<FilteredProcess>("name", "name"));
        props.add(new BeanProperty<FilteredProcess>("description", "description"));

        return props;
    }

    @Override
    protected List<FilteredProcess> getItems() {
        return selectableProcesses;
    }

    
}
