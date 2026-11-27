/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphicBuilder;
import org.geotools.util.logging.Logging;

/**
 * Helper class for a wicket ajax behavior that updates an {@link Image} component with the legend graphic of a style.
 *
 * <p>The legend is rendered in-process, independent of the WMS service being enabled, global services, workspace
 * isolation or strict CITE compliance.
 *
 * @author Gabriel Roldan
 * @sicne 2.1
 */
class LegendGraphicAjaxUpdater implements Serializable {

    @Serial
    private static final long serialVersionUID = 5543647283072466506L;

    private static final Logger LOGGER = Logging.getLogger(LegendGraphicAjaxUpdater.class);

    private Image image;

    public LegendGraphicAjaxUpdater(final Image image, final IModel<StyleInfo> styleInfoModel) {
        this.image = image;
        this.image.setImageResource(new LegendGraphicResource(styleInfoModel));
    }

    public void updateStyleImage(AjaxRequestTarget target) {
        if (target != null) {
            target.add(image);
        }
    }

    /** PNG legend graphic of the current style, rendered with {@link BufferedImageLegendGraphicBuilder}. */
    private static class LegendGraphicResource extends DynamicImageResource {

        @Serial
        private static final long serialVersionUID = -2405478627343493316L;

        private final IModel<StyleInfo> styleInfoModel;

        LegendGraphicResource(IModel<StyleInfo> styleInfoModel) {
            super("png");
            this.styleInfoModel = styleInfoModel;
        }

        @Override
        protected byte[] getImageData(Attributes attributes) {
            StyleInfo styleInfo = styleInfoModel.getObject();
            if (styleInfo == null) {
                return null;
            }
            try {
                GetLegendGraphicRequest request = new GetLegendGraphicRequest();
                request.setLayer(null);
                request.setStyle(styleInfo.getStyle());
                request.setStrict(false);
                BufferedImage legend = new BufferedImageLegendGraphicBuilder().buildLegendGraphic(request);
                return toImageData(legend);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to render legend graphic for style " + styleInfo.prefixedName(), e);
                return null;
            }
        }
    }
}
