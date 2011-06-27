package org.geoserver.gwc.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.config.GWCConfig;

public class CachingOptionsPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public CachingOptionsPanel(final String id, final IModel<GWCConfig> gwcConfigModel) {

        super(id, gwcConfigModel);

        IModel<Boolean> autoCacheLayersModel;
        autoCacheLayersModel = new PropertyModel<Boolean>(gwcConfigModel, "cacheLayersByDefault");
        CheckBox autoCacheLayers = new CheckBox("cacheLayersByDefault", autoCacheLayersModel);
        add(autoCacheLayers);

        IModel<Boolean> nonDefaultStylesModel;
        nonDefaultStylesModel = new PropertyModel<Boolean>(gwcConfigModel, "cacheNonDefaultStyles");
        CheckBox cacheNonDefaultStyles = new CheckBox("cacheNonDefaultStyles",
                nonDefaultStylesModel);
        add(cacheNonDefaultStyles);

        List<Integer> metaTilingChoices = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IModel<Integer> metaTilingXModel = new PropertyModel<Integer>(gwcConfigModel, "metaTilingX");
        DropDownChoice<Integer> metaTilingX = new DropDownChoice<Integer>("metaTilingX",
                metaTilingXModel, metaTilingChoices);
        add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<Integer>(gwcConfigModel, "metaTilingY");
        DropDownChoice<Integer> metaTilingY = new DropDownChoice<Integer>("metaTilingY",
                metaTilingYModel, metaTilingChoices);
        add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<Integer>(gwcConfigModel, "gutter");
        List<Integer> gutterChoices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50);
        DropDownChoice<Integer> gutterChoice = new DropDownChoice<Integer>("gutter", gutterModel,
                gutterChoices);
        add(gutterChoice);

        final List<String> formats = Arrays.asList("image/png", "image/png8", "image/jpeg",
                "image/gif");
        {
            IModel<List<String>> vectorFormatsModel = new PropertyModel<List<String>>(
                    gwcConfigModel, "defaultVectorCacheFormats");
            CheckGroup<String> vectorFormatsGroup = new CheckGroup<String>("vectorFormatsGroup",
                    vectorFormatsModel);
            add(vectorFormatsGroup);
            vectorFormatsGroup.add(new ListView<String>("vectorFromats", formats) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check<String>("vectorFormatsOption", item.getModel()));
                    item.add(new Label("name", item.getModel()));
                }
            });
        }

        {
            IModel<List<String>> rasterFormatsModel = new PropertyModel<List<String>>(
                    gwcConfigModel, "defaultCoverageCacheFormats");
            CheckGroup<String> rasterFormatsGroup = new CheckGroup<String>("rasterFormatsGroup",
                    rasterFormatsModel);
            add(rasterFormatsGroup);
            rasterFormatsGroup.add(new ListView<String>("rasterFromats", formats) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check<String>("rasterFormatsOption", item.getModel()));
                    item.add(new Label("name", item.getModel()));
                }
            });
        }
        {
            IModel<List<String>> otherFormatsModel = new PropertyModel<List<String>>(
                    gwcConfigModel, "defaultOtherCacheFormats");
            CheckGroup<String> otherFormatsGroup = new CheckGroup<String>("otherFormatsGroup",
                    otherFormatsModel);
            add(otherFormatsGroup);
            otherFormatsGroup.add(new ListView<String>("otherFromats", formats) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check<String>("otherFormatsOption", item.getModel()));
                    item.add(new Label("name", item.getModel()));
                }
            });
        }
    }
}
