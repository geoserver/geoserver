/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.List;
import org.apache.wicket.model.ChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;

/** A model for a LayerGroupStyle */
public class LayerGroupStyleModel extends ChainingModel<LayerGroupStyle> {

    private IModel<List<PublishedInfo>> layersModel;
    private IModel<List<StyleInfo>> stylesModel;
    private IModel<StyleInfo> nameModel;
    private IModel<String> strNameModel;

    public LayerGroupStyleModel(IModel<LayerGroupStyle> chainedModel) {
        super(chainedModel);
        this.layersModel = new PropertyModel<>(getChainedModel(), "layers");
        this.stylesModel = new PropertyModel<>(getChainedModel(), "styles");
        this.nameModel = new PropertyModel<>(getChainedModel(), "name");
        this.strNameModel = new PropertyModel<>(nameModel, "name");
    }

    /**
     * Get the List of layers held by the underlying model object.
     *
     * @return the list of layer of the underlying LayerGroupStyle.
     */
    public List<PublishedInfo> getLayers() {
        return layersModel.getObject();
    }

    /**
     * Get the List of styles held by the underlying model object.
     *
     * @return the list of styles of the underlying LayerGroupStyle.
     */
    public List<StyleInfo> getStyles() {
        return stylesModel.getObject();
    }

    /**
     * Get the Model of the name attribute of the LayerGroupStyle as a Model<String>.
     *
     * @return a String Model of the LayerGroupStyle name.
     */
    public IModel<String> nameModel() {
        return strNameModel;
    }

    /**
     * Set the Layer lists to the model object.
     *
     * @param pubList the list of PublishedInfo objects.
     */
    public void setLayers(List<PublishedInfo> pubList) {
        layersModel.setObject(pubList);
    }

    /**
     * Set the list of Styles to the model object.
     *
     * @param styles the list of StyleInfo objects.
     */
    public void setStyles(List<StyleInfo> styles) {
        stylesModel.setObject(styles);
    }
}
