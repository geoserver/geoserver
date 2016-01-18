/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;

public class StyleChooser extends Panel {
    private GeoServerDataProvider<StyleInfo> styleProvider;
    private GeoServerTablePanel<StyleInfo> styleTable;

    public StyleChooser(String id, final CssDemoPage demo) {
        super(id);

        styleProvider =
            new GeoServerDataProvider<StyleInfo>() {
                Property<StyleInfo> name =
                    new AbstractProperty<StyleInfo>("Name") {
                        public Object getPropertyValue(StyleInfo x) {
                            return x.getName();
                        }
                    };
                protected List<StyleInfo> getItems() {
                    return demo.catalog().getStyles();
                }
                public List<Property<StyleInfo>> getProperties() {
                    return Arrays.asList(name);
                }
            };
        styleTable =
            new GeoServerTablePanel<StyleInfo>("style.table", styleProvider) {
                @Override
                public Component getComponentForProperty(
                    String id, IModel value, final Property<StyleInfo> property
                ) {
                    final StyleInfo style = (StyleInfo) value.getObject();
                    Fragment fragment =
                        new Fragment(id, "style.link", StyleChooser.this);
                    AjaxLink link =
                        new AjaxLink("link") {
                            { 
                                add(new Label(
                                    "style.name",
                                    new Model(property.getPropertyValue(style).toString())
                                ));
                            }

                            public void onClick(AjaxRequestTarget target) {
                                PageParameters params = new PageParameters();
                                params.add("layer", demo.getLayer().prefixedName()
                                );
                                WorkspaceInfo workspace = style.getWorkspace();
                                if (workspace == null) {
                                    params.add("style", style.getName());
                                } else {
                                    params.add("style", workspace.getName() + ":" + style.getName());
                                }
                                setResponsePage(CssDemoPage.class, params);
                            }
                        };
                    fragment.add(link);
                    return fragment;
                }
            };
        add(styleTable);
    }
}
