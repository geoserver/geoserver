/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geotools.util.logging.Logging;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Select2MultiChoice;
import org.wicketstuff.select2.Settings;

@SuppressWarnings("serial")
public class Select2SetMultiChoice<T> extends FormComponentPanel<Set<T>> {

    private static final Logger log = Logging.getLogger(Select2SetMultiChoice.class);

    private Select2MultiChoice<T> select2;

    public Select2SetMultiChoice(String id, IModel<Set<T>> model, ChoiceProvider<T> provider) {
        super(id, model);

        Collection<T> initalValue = new ArrayList<>(model.getObject() == null ? Set.of() : model.getObject());
        IModel<Collection<T>> selectModel = Model.of(initalValue);

        add(select2 = new Select2MultiChoice<>("select", selectModel, provider));
        select2.getSettings().setQueryParam("qm");
        select2.getSettings().setPlaceholder("select"); // required for allowClear
        select2.getSettings().setAllowClear(true);
        select2.setOutputMarkupPlaceholderTag(true);
        this.setOutputMarkupPlaceholderTag(true);
        // set internal select2 component width to 100% to expand to its container's width and
        // respect the width of this container panel
        select2.getSettings().setWidth("100%");
        select2.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Collection<T> modelObject = select2.getModelObject();
                log.finer(() -> "multichoice model updated: %s".formatted(modelObject));
            }
        });
    }

    public Settings getSettings() {
        return select2.getSettings();
    }

    @Override
    protected void onModelChanged() {
        Set<T> modelObject = getModelObject();
        select2.setModelObject(modelObject);
    }

    @Override
    public void convertInput() {
        Collection<T> choices = select2.getModelObject();
        if (null == choices) choices = Set.of();
        setConvertedInput(new LinkedHashSet<>(choices));
    }
}
