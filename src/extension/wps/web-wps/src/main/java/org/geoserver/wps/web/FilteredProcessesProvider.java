/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.validator.WPSInputValidator;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Provides entries for the process filtering table in the {@link ProcessSelectionPage}
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class FilteredProcessesProvider
        extends GeoServerDataProvider<FilteredProcessesProvider.FilteredProcess> {

    /**
     * Represents a selectable process in the GUI
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class FilteredProcess implements Serializable, Comparable<FilteredProcess> {

        private boolean enabled;

        private Name name;

        private String description;

        private List<String> roles;

        private Multimap<String, WPSInputValidator> validators = ArrayListMultimap.create();

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

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public int compareTo(FilteredProcess other) {
            if (name == null) {
                return other.getName() == null ? 0 : -1;
            } else if (other.getName() == null) {
                return 1;
            } else {
                return name.getURI().compareTo(other.getName().getURI());
            }
        }

        public ProcessInfo toProcessInfo() {
            ProcessInfo pai = new ProcessInfoImpl();
            pai.setName(getName());
            pai.setEnabled(getEnabled());
            if (getRoles() != null && getRoles().size() > 0) {
                pai.getRoles().addAll(getRoles());
            }
            if (validators != null && validators.size() > 0) {
                pai.getValidators().putAll(validators);
            }

            return pai;
        }

        public Multimap<String, WPSInputValidator> getValidators() {
            return validators;
        }

        public void setValidators(Multimap<String, WPSInputValidator> validators) {
            this.validators = validators;
        }

        public boolean isValidated() {
            return validators != null && validators.size() > 0;
        }
    }

    private List<FilteredProcess> selectableProcesses;

    public FilteredProcessesProvider(ProcessGroupInfo pfi, Locale locale) {
        ProcessFactory pf = GeoServerProcessors.getProcessFactory(pfi.getFactoryClass(), false);
        Set<Name> names = pf.getNames();
        selectableProcesses = new ArrayList<FilteredProcess>();
        List<ProcessInfo> filteredProcesses = pfi.getFilteredProcesses();
        for (Name name : names) {
            InternationalString description =
                    GeoServerProcessors.getProcessFactory(pfi.getFactoryClass(), false)
                            .getDescription(name);
            String des = "";
            if (description != null) {
                des = description.toString(locale);
            }
            FilteredProcess sp = new FilteredProcess(name, des);
            sp.setEnabled(true);

            for (ProcessInfo fp : filteredProcesses) {
                if (sp.getName().equals(fp.getName())) {
                    sp.setEnabled(fp.isEnabled());
                    sp.setRoles(fp.getRoles());
                    sp.setValidators(fp.getValidators());
                }
            }

            selectableProcesses.add(sp);
        }

        Collections.sort(selectableProcesses);
    }

    @Override
    protected List<Property<FilteredProcess>> getProperties() {
        List<Property<FilteredProcess>> props =
                new ArrayList<GeoServerDataProvider.Property<FilteredProcess>>();
        props.add(new BeanProperty<FilteredProcess>("enabled", "enabled"));
        props.add(new BeanProperty<FilteredProcess>("name", "name"));
        props.add(new BeanProperty<FilteredProcess>("description", "description"));
        props.add(
                new AbstractProperty<FilteredProcess>("roles") {
                    @Override
                    public Object getPropertyValue(FilteredProcess item) {
                        return item.getRoles();
                    }

                    @Override
                    public IModel getModel(IModel itemModel) {
                        return new PropertyModel(itemModel, "roles");
                    }
                });
        props.add(new PropertyPlaceholder("edit"));
        props.add(new BeanProperty("validated", "validated"));
        return props;
    }

    @Override
    protected List<FilteredProcess> getItems() {
        return selectableProcesses;
    }
}
