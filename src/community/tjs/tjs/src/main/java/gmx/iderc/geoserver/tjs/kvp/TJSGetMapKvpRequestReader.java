package gmx.iderc.geoserver.tjs.kvp;

import gmx.iderc.geoserver.tjs.data.TJS_WMSLayer;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.styling.Style;
import org.geotools.styling.StyleAttributeExtractor;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 11/2/12
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJSGetMapKvpRequestReader extends GetMapKvpRequestReader {

    public TJSGetMapKvpRequestReader(WMS wms) {
        super(wms);
    }

    public static void checkStyle(Style style, MapLayerInfo mapLayerInfo) throws ServiceException {
        try {
            GetMapKvpRequestReader.checkStyle(style, mapLayerInfo);
        } catch (ServiceException ex) {
            //si la excepci'on es producto de una capa WMS vamos a trabajarla otra vez
            if (mapLayerInfo.getType() == mapLayerInfo.TYPE_WMS) {
                StyleAttributeExtractor sae = new StyleAttributeExtractor();
                sae.visit(style);
                String[] styleAttributes = sae.getAttributeNames();

                WMSLayerInfo wmsLayerInfo = (WMSLayerInfo) mapLayerInfo.getLayerInfo().getResource();
                try {
                    Object wmsLayer = wmsLayerInfo.getWMSLayer(new NullProgressListener());
                    if (wmsLayer instanceof TJS_WMSLayer) {
                        TJS_WMSLayer tjsLayer = (TJS_WMSLayer) wmsLayer;
                        FeatureType type = tjsLayer.getFeatureType();
                        Set attributes = new HashSet();
                        for (PropertyDescriptor pd : type.getDescriptors()) {
                            if (pd instanceof AttributeDescriptor) {
                                attributes.add(pd.getName().getLocalPart());
                            }
                        }

                        // check all attributes required by the style are available
                        String attName;
                        final int length = styleAttributes.length;
                        for (int i = 0; i < length; i++) {
                            attName = styleAttributes[i];

                            if (!attributes.contains(attName)) {
                                throw new ServiceException(
                                                                  "The requested Style can not be used with this layer.  The style specifies "
                                                                          + "an attribute of " + attName + " and the layer is: "
                                                                          + mapLayerInfo.getName());
                            }
                        }

                    }
                } catch (Exception ex2) {
                    throw new ServiceException(ex2.getMessage());
                }
            } else {// si no, se le da el camino normal de la excepci'on
                throw ex;
            }
        }
    }

}
