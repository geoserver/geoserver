/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.memorycache;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geowebcache.storage.memorycache.KeyValueStoreInfo;
import org.geowebcache.storage.memorycache.MemoryCacheInfo;

/** Edits the tile cache settings in the model, including those of the selected backend. */
public class MemoryCachePanel extends Panel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String BACKEND_PANEL = "backendPanel";

    public MemoryCachePanel(String id, IModel<MemoryCacheInfo> model) {
        super(id, model);
        // an unset backend reads as a new default instance on every call, so pin it for its panel to edit
        MemoryCacheInfo info = model.getObject();
        info.setBackend(info.getBackend());

        WebMarkupContainer settings = new WebMarkupContainer("settings") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                // hidden form components are not submitted, so their values are kept and saved unchanged
                setVisible(model.getObject().isEnabled());
            }
        };
        settings.setOutputMarkupPlaceholderTag(true);
        add(settings);
        add(new AjaxCheckBox("enabled", new PropertyModel<>(model, "enabled")) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(settings);
            }
        });

        settings.add(new CheckBox("passthrough", new PropertyModel<>(model, "passthrough")));
        NumberTextField<Long> timeToLive =
                new NumberTextField<>("timeToLive", new PropertyModel<>(model, "timeToLive"), Long.class);
        settings.add(timeToLive.setMinimum(1L).setRequired(true));
        NumberTextField<Long> statsInterval =
                new NumberTextField<>("statsInterval", new PropertyModel<>(model, "statsInterval"), Long.class);
        settings.add(statsInterval.setMinimum(0L).setRequired(true));

        // backend selection, swapping the settings and their panel on change
        IModel<KeyValueStoreInfo> backendModel = new PropertyModel<>(model, "backend");
        List<KeyValueStoreType<?>> types = new ArrayList<>();
        for (KeyValueStoreType<?> type : GeoServerExtensions.extensions(KeyValueStoreType.class)) types.add(type);
        KeyValueStoreType<?> current = types.stream()
                .filter(t -> t.getConfigClass() == info.getBackend().getClass())
                .findFirst()
                .orElse(null);
        WebMarkupContainer backendContainer = new WebMarkupContainer("backendContainer");
        backendContainer.setOutputMarkupId(true);
        backendContainer.add(backendPanel(current, backendModel));
        settings.add(backendContainer);

        // settings by class, so that switching backend and back keeps the edits; "Choose One" picks the original
        Map<Class<?>, KeyValueStoreInfo> edited = new HashMap<>();
        Class<?> original = info.getBackend().getClass();

        // ids by settings class, as the page holds deserialized copies of the type beans
        ChoiceRenderer<KeyValueStoreType<?>> renderer = new ChoiceRenderer<>(null, "configClass.name");
        DropDownChoice<KeyValueStoreType<?>> backend =
                new DropDownChoice<>("backend", new Model<>(current), types, renderer);
        backend.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                KeyValueStoreType<?> selected = backend.getModelObject();
                edited.put(backendModel.getObject().getClass(), backendModel.getObject());
                Class<?> key = selected == null ? original : selected.getConfigClass();
                backendModel.setObject(edited.computeIfAbsent(key, k -> selected.newConfigObject()));
                backendContainer.addOrReplace(backendPanel(selected, backendModel));
                target.add(backendContainer);
            }
        });
        settings.add(backend);
    }

    /** Empty when no type matches the configured backend, which then cannot be edited here. */
    @SuppressWarnings("unchecked") // the model holds settings of the type's config class, both switched together
    private static Component backendPanel(KeyValueStoreType<?> type, IModel<KeyValueStoreInfo> model) {
        if (type == null) return new EmptyPanel(BACKEND_PANEL);
        return ((KeyValueStoreType<KeyValueStoreInfo>) type).createPanel(BACKEND_PANEL, model);
    }
}
