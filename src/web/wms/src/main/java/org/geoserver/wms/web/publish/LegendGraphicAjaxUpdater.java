/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.io.Serializable;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.geoserver.catalog.StyleInfo;

/**
 * Helper class for a wicket ajax behavior that updates the {@code src} attribute of an {@link
 * Image} component to point to a WMS GetLegendGraphic request.
 *
 * @author Gabriel Roldan
 * @sicne 2.1
 */
class LegendGraphicAjaxUpdater implements Serializable {

    private static final long serialVersionUID = 5543647283072466506L;

    private Image image;

    private IModel<StyleInfo> styleInfoModel;
    private IModel<String> urlModel;

    public LegendGraphicAjaxUpdater(final Image image, final IModel<StyleInfo> styleInfoModel) {
        this.image = image;
        this.styleInfoModel = styleInfoModel;
        this.urlModel = new Model<>();
        this.image.add(new AttributeModifier("src", urlModel));
        updateStyleImage(null);
    }

    public void updateStyleImage(AjaxRequestTarget target) {
        StyleInfo styleInfo = styleInfoModel.getObject();
        if (styleInfo != null) {
            String url;
            if (styleInfo.getWorkspace() == null) {
                // the wms url is build without qualification to allow usage of global styles,
                // the style name and layer name will be ws qualified instead
                url = RequestCycle.get().getUrlRenderer().renderContextRelativeUrl("wms") + "?";
            } else {
                url =
                        RequestCycle.get()
                                        .getUrlRenderer()
                                        .renderContextRelativeUrl(
                                                styleInfo.getWorkspace().getName() + "/wms")
                                + "?";
            }
            String style = styleInfo.prefixedName();
            url +=
                    "REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&STRICT=false&style="
                            + style;
            urlModel.setObject(url);
            if (target != null) {
                target.add(image);
            }
        }
    }
}
