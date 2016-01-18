/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.StyleInfo;

/**
 * Helper class for a wicket ajax behavior that updates the {@code src} attribute of an
 * {@link Image} component to point to a WMS GetLegendGraphic request.
 * 
 * @author Gabriel Roldan
 * @sicne 2.1
 */
class LegendGraphicAjaxUpdater implements Serializable {

    private static final long serialVersionUID = 5543647283072466506L;

    private Image image;

    private IModel styleInfoModel;

    private String wmsURL;

    public LegendGraphicAjaxUpdater(final String wmsURL, final Image image,
            final IModel styleInfoModel) {
        this.wmsURL = wmsURL;
        this.image = image;
        this.styleInfoModel = styleInfoModel;
        updateStyleImage(null);
    }

    public void updateStyleImage(AjaxRequestTarget target) {
        String url = wmsURL
                + "REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&STRICT=false&style=";
        StyleInfo styleInfo = (StyleInfo) styleInfoModel.getObject();
        if (styleInfo != null) {
            String style = styleInfo.prefixedName();
            url += style;
            image.add(new AttributeModifier("src", new Model(url)));
            if (target != null) {
                target.add(image);
            }
        }
    }

}
