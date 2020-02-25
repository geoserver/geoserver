/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizingParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;

/**
 * Subform for a ParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public abstract class AbstractParameterFilterSubform<T extends ParameterFilter>
        extends FormComponentPanel<T> {

    private static final long serialVersionUID = -213688039804104263L;

    protected Component normalize;

    public AbstractParameterFilterSubform(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    public void convertInput() {
        visitChildren(
                (component, visit) -> {
                    if (component instanceof FormComponent) {
                        FormComponent<?> formComponent = (FormComponent<?>) component;
                        formComponent.processInput();
                    }
                });
        T filter = getModelObject();
        setConvertedInput(filter);
    }

    /**
     * Adds the {@link CaseNormalizerSubform} component.
     *
     * @implNote Note we're calling {@code filter.setNormalize(filter.getNormalize());} to make the
     *     {@link CaseNormalizer} instance variable explicitly set, because otherwise a new instance
     *     is returned upon each invocation of {@link
     *     CaseNormalizingParameterFilter#getNormalize()}. This is a workaround for a side effect of
     *     an implementation detail in CaseNormalizingParameterFilter (super class of
     *     StringParameterFilter and RegExParameterFilter) that makes this form's updated value not
     *     to be set if a CaseNormalizer wasn't explicitly set before. Rationale being that before
     *     GWC configuration objects properly implemented equals() and hashCode(),
     *     CaseNormalizer.equals() always returned false, and hence
     *     org.apache.wicket.Component.setDefaultModelObject() (from new
     *     PropertyModel<CaseNormalizer>(model, "normalize") bellow) will always update the model.
     *     With equals and hashCode properly implemented though, Component.setDefaultModelObject()
     *     will not enter the {@code if (!getModelComparator().compare(this, object))} condition and
     *     hence won't reach {@code model.setObject(object);}, since it will be comparing two
     *     equivalent instances, product of CaseNormalizingParameterFilter.getNormalize() returning
     *     a new CaseNormalizer instance when a value is not explicitly set.
     *     <p>This workaround is side effect free since the net result of Filter having an explicit
     *     value for its normalize instance variable is guaranteed anyways by the way the form
     *     works.
     *     <p>In the long run what should happen is that StringParameterFilter and CaseNormalizer
     *     behave like simple POJOs instead of being clever about returning a new default value on
     *     each accessor invocation.
     *     <p>This same workaround is applied to RegExParameterFilterSubform
     */
    protected void addNormalize(IModel<? extends CaseNormalizingParameterFilter> model) {
        CaseNormalizingParameterFilter filter = model.getObject();
        filter.setNormalize(filter.getNormalize());
        normalize =
                new CaseNormalizerSubform(
                        "normalize", new PropertyModel<CaseNormalizer>(model, "normalize"));
        add(normalize);
    }
}
