/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.tilecache;

import java.io.Serial;
import java.util.List;
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
import org.geowebcache.storage.tilecache.TileCacheBackendInfo;
import org.geowebcache.storage.tilecache.TileCacheInfo;

/** Edits the tile cache settings in the model, including those of the selected backend. */
public class TileCachePanel extends Panel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String BACKEND_PANEL = "backendPanel";

    public TileCachePanel(String id, IModel<TileCacheInfo> model) {
        super(id, model);
        // an unset backend reads as a new default instance on every call, so pin it for its panel to edit
        TileCacheInfo info = model.getObject();
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
        IModel<TileCacheBackendInfo> backendModel = new PropertyModel<>(model, "backend");
        List<TileCacheBackendType> types = GeoServerExtensions.extensions(TileCacheBackendType.class);
        TileCacheBackendType current = types.stream()
                .filter(t -> t.getSettingsClass() == info.getBackend().getClass())
                .findFirst()
                .orElse(null);
        WebMarkupContainer backendContainer = new WebMarkupContainer("backendContainer");
        backendContainer.setOutputMarkupId(true);
        backendContainer.add(backendPanel(current, backendModel));
        settings.add(backendContainer);

        // ids by settings class, as the page holds deserialized copies of the type beans
        ChoiceRenderer<TileCacheBackendType> renderer = new ChoiceRenderer<>(null, "settingsClass.name");
        DropDownChoice<TileCacheBackendType> backend =
                new DropDownChoice<>("backend", new Model<>(current), types, renderer);
        backend.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                TileCacheBackendType selected = backend.getModelObject();
                backendModel.setObject(selected.newSettings());
                backendContainer.addOrReplace(backendPanel(selected, backendModel));
                target.add(backendContainer);
            }
        });
        settings.add(backend);
    }

    /** Empty when no type matches the configured backend, which then cannot be edited here. */
    private static Component backendPanel(TileCacheBackendType type, IModel<TileCacheBackendInfo> model) {
        return type == null ? new EmptyPanel(BACKEND_PANEL) : type.createPanel(BACKEND_PANEL, model);
    }
}
