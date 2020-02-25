/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.MultiplicityValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geoserver.wps.validator.WPSInputValidator;
import org.geoserver.wps.web.FilteredProcessesProvider.FilteredProcess;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.springframework.context.ApplicationContext;

/**
 * Allows the admin to edit the limits for a specific process
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessLimitsPage extends GeoServerSecuredPage {

    private static final Set<Class<?>> PRIMITIVE_NUMBERS =
            new ImmutableSet.Builder<Class<?>>()
                    .add(byte.class)
                    .add(char.class)
                    .add(double.class)
                    .add(float.class)
                    .add(int.class)
                    .add(long.class)
                    .add(short.class)
                    .build();

    private GeoServerTablePanel<InputLimit> table;

    private FilteredProcess process;

    public ProcessLimitsPage(Page returnPage, final FilteredProcess process) {
        setReturnPage(returnPage);
        this.process = process;

        Form form = new Form("form");
        add(form);

        final List<InputLimit> inputLimits = buildInputLimits(process);
        GeoServerDataProvider<InputLimit> inputLimitsProvider =
                new GeoServerDataProvider<ProcessLimitsPage.InputLimit>() {

                    @Override
                    protected List<
                                    org.geoserver.web.wicket.GeoServerDataProvider.Property<
                                            InputLimit>>
                            getProperties() {
                        List<Property<InputLimit>> result = new ArrayList<>();
                        result.add(new BeanProperty<InputLimit>("name", "name"));
                        result.add(new PropertyPlaceholder("type"));
                        result.add(new BeanProperty<InputLimit>("editor", "validator"));
                        return result;
                    }

                    @Override
                    protected List<InputLimit> getItems() {
                        return inputLimits;
                    }
                };

        table =
                new GeoServerTablePanel<InputLimit>("table", inputLimitsProvider) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<InputLimit> itemModel,
                            Property<InputLimit> property) {
                        InputLimit limit = (InputLimit) itemModel.getObject();
                        String propertyName = property.getName();
                        if (propertyName.equals("type")) {
                            String type;
                            try {
                                String key = "type." + limit.getValidator().getClass().getName();
                                type =
                                        new ParamResourceModel(key, ProcessLimitsPage.this)
                                                .getString();
                            } catch (Exception e) {
                                type = limit.validator.getClass().getSimpleName();
                            }
                            return new Label(id, type);
                        } else if (propertyName.equals("editor")) {
                            WPSInputValidator validator = limit.getValidator();
                            if (validator instanceof MaxSizeValidator) {
                                Fragment f = new Fragment(id, "textEditor", ProcessLimitsPage.this);
                                PropertyModel maxSizeModel =
                                        new PropertyModel(itemModel, "validator.maxSizeMB");
                                TextField<Integer> text =
                                        new TextField<Integer>("text", maxSizeModel, Integer.class);
                                f.add(text);
                                return f;
                            } else if (validator instanceof MultiplicityValidator) {
                                Fragment f = new Fragment(id, "textEditor", ProcessLimitsPage.this);
                                PropertyModel maxMultiplicityModel =
                                        new PropertyModel(itemModel, "validator.maxInstances");
                                TextField<Integer> text =
                                        new TextField<Integer>(
                                                "text", maxMultiplicityModel, Integer.class);
                                f.add(text);
                                return f;
                            } else if (validator instanceof NumberRangeValidator) {
                                Fragment f =
                                        new Fragment(id, "rangeEditor", ProcessLimitsPage.this);
                                PropertyModel rangeModel =
                                        new PropertyModel(itemModel, "validator.range");
                                RangePanel rangeEditor = new RangePanel("range", rangeModel);
                                f.add(rangeEditor);
                                return f;
                            }
                        }
                        // have the base class create a label for us
                        return null;
                    }
                };
        table.setOutputMarkupId(true);
        table.setFilterable(false);
        table.setPageable(false);
        table.setItemReuseStrategy(new DefaultItemReuseStrategy());
        table.setSortable(false);
        form.add(table);

        SubmitLink apply =
                new SubmitLink("apply") {
                    @Override
                    public void onSubmit() {
                        // super.onSubmit();
                        process.setValidators(buildValidators(inputLimits));
                        doReturn();
                    }
                };
        form.add(apply);
        Link cancel =
                new Link("cancel") {
                    @Override
                    public void onClick() {
                        doReturn();
                    }
                };
        form.add(cancel);
    }

    /**
     * Turn the input limits into the UI into a set of operational validators, filtering out those
     * that have empty or default input
     */
    protected Multimap<String, WPSInputValidator> buildValidators(List<InputLimit> inputLimits) {
        Multimap<String, WPSInputValidator> result = ArrayListMultimap.create();
        for (InputLimit inputLimit : inputLimits) {
            String name = inputLimit.getName();
            WPSInputValidator validator = inputLimit.getValidator();
            // skip validators that are not going to do anything
            if (validator.isUnset()) {
                continue;
            }

            result.put(name, validator);
        }

        return result;
    }

    /**
     * Go from the available process validator to a UI representation, adding also the possible
     * validators that are not yet set
     */
    private List<InputLimit> buildInputLimits(FilteredProcess process) {
        ApplicationContext applicationContext = GeoServerApplication.get().getApplicationContext();
        Multimap<String, WPSInputValidator> validators = process.getValidators();
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(process.getName(), false);
        Map<String, Parameter<?>> parameters = pf.getParameterInfo(process.getName());
        List<InputLimit> result = new ArrayList<>();
        for (Parameter param : parameters.values()) {
            String name = param.getName();
            Collection<WPSInputValidator> paramValidators =
                    validators != null ? validators.get(name) : null;

            // add the existing validators and collect their types
            Set<Class> validatorTypes = new HashSet<>();
            if (paramValidators != null) {
                for (WPSInputValidator validator : paramValidators) {
                    validatorTypes.add(validator.getClass());
                    // we deep clone the validator to avoid changing the WPS state until
                    // the main security page gets submitted
                    result.add(new InputLimit(name, validator.copy()));
                }
            }

            // see if we need to add some missing validator
            if (param.getMaxOccurs() > 1 && !validatorTypes.contains(MultiplicityValidator.class)) {
                int max = 0;
                if (param.getMaxOccurs() < Integer.MAX_VALUE) {
                    max = param.getMaxOccurs();
                }
                result.add(new InputLimit(name, new MultiplicityValidator(max)));
            }
            boolean isComplex = ProcessParameterIO.isComplex(param, applicationContext);
            if (isComplex) {
                if (!validatorTypes.contains(MaxSizeValidator.class)) {
                    result.add(new InputLimit(name, new MaxSizeValidator(0)));
                }
            } else {
                if (isNumeric(param) && !validatorTypes.contains(NumberRangeValidator.class)) {
                    result.add(new InputLimit(name, new NumberRangeValidator(null)));
                }
            }
        }

        return result;
    }

    private boolean isNumeric(Parameter param) {
        return Number.class.isAssignableFrom(param.getType())
                || PRIMITIVE_NUMBERS.contains(param.getType());
    }

    protected String getTitle() {
        return new ParamResourceModel("title", this, process.getName().getURI()).getString();
    }

    /**
     * The input limit
     *
     * @author Andrea Aime - GeoSolutions
     */
    static final class InputLimit implements Serializable {
        private static final long serialVersionUID = -4763254264009929615L;

        String name;

        WPSInputValidator validator;

        public InputLimit(String name, WPSInputValidator validator) {
            this.name = name;
            this.validator = validator;
        }

        @Override
        public String toString() {
            return "InputLimit [name=" + name + ", validator=" + validator + "]";
        }

        public String getName() {
            return name;
        }

        public WPSInputValidator getValidator() {
            return validator;
        }
    }
}
