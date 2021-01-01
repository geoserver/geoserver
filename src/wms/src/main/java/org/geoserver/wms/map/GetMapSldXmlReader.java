package org.geoserver.wms.map;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.wms.WMS;
import org.geotools.sld.v1_1.SLD;
import org.locationtech.jts.geom.Coordinate;
import org.w3c.dom.Node;

/**
 * Implementation of AbstractGetMapXmlReader that supports the GetMap XML Schema defined by the
 * "Styled Layer Descriptor profile of the Web Map Service Implementation Specification", OGC document 05-078r4;
 * see http://schemas.opengis.net/sld/1.1/GetMap.xsd for the XML Schema.
 */
public class GetMapSldXmlReader extends AbstractGetMapXmlReader {

  public GetMapSldXmlReader(WMS wms) {
    super(wms, SLD.NAMESPACE);
  }

  @Override
  protected String getEpsgCode(Node nodeGetMap, Node nodeBbox) {
    Node nodeCrs = getNode(nodeGetMap, "CRS");
    if (nodeCrs != null) {
      return nodeCrs.getTextContent();
    } else {
      return null;
    }

  }

  @Override
  protected List<Coordinate> getCoordinateList(Node nodeBbox) {
    List<Coordinate> coordList = new ArrayList<>();
    Node lowerCornerNode = getNode(nodeBbox, "LowerCorner");
    if(lowerCornerNode != null) {
      coordList.add(getCoordinate(lowerCornerNode));
    }
    Node upperCornerNode = getNode(nodeBbox, "UpperCorner");
    if(upperCornerNode != null) {
      coordList.add(getCoordinate(upperCornerNode));
    }
    return coordList;
  }

  /** xs:element name="LowerCorner/UpperCorner" type="ows:PositionType2D"/> */
  private Coordinate getCoordinate(Node nodePosition) {
    Coordinate coordinate = new Coordinate();
    String[] lowerCornerUnparsed = nodePosition.getTextContent().split(" ");
    for (int i = 0; i < lowerCornerUnparsed.length; i++) {
      coordinate.setOrdinate(i, Double.parseDouble(lowerCornerUnparsed[i]));
    }
    return coordinate;
  }
}