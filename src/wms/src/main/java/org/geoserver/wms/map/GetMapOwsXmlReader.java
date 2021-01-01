package org.geoserver.wms.map;

import java.util.List;

import org.geoserver.wms.WMS;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.ExpressionDOMParser;
import org.locationtech.jts.geom.Coordinate;
import org.w3c.dom.Node;

/**
 * Implementation of AbstractGetMapXmlReader that supports the GetMap XML Schema defined by the
 * "Web Map Service Implementation Specification - Part 2: XML for Requests using HTTP Post", OGC document 02-017r1;
 * see https://portal.opengeospatial.org/files/?artifact_id=1118, page 18 for the XML Schema.
 */
public class GetMapOwsXmlReader extends AbstractGetMapXmlReader {

  public GetMapOwsXmlReader(WMS wms) {
    super(wms, OWS.NAMESPACE);
  }

  @Override
  protected String getEpsgCode(Node nodeGetMap, Node nodeBbox) {
    Node srsNode = nodeBbox.getAttributes().getNamedItem("srsName");
    if (srsNode != null) {
      return srsNode.getNodeValue();
    } else {
      return null;
    }
  }

  @Override
  protected List<Coordinate> getCoordinateList(Node nodeBbox) {
    return new ExpressionDOMParser(CommonFactoryFinder.getFilterFactory2()).coords(nodeBbox);
  }
}