package com.boundlessgeo.gsr.translate.geometry;

import com.boundlessgeo.gsr.api.GeoServicesJacksonJsonConverter;
import com.boundlessgeo.gsr.model.geometry.Geometry;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKID;
import com.vividsolutions.jts.geom.Envelope;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.opengis.referencing.FactoryException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class QuantizedGeometryEncoderTest {

    @Test
    public void testRepresentation() throws URISyntaxException, IOException, FactoryException {
        //test based on real data from an ArcGIS JS API app
        final int testSRID = 102100;

        File jsonFile = new File(getClass().getResource("sample_geometry.json").toURI());
        net.sf.json.JSON json = JSONSerializer.toJSON(FileUtils.readFileToString(jsonFile, "UTF-8"));

        com.vividsolutions.jts.geom.Geometry inputGeometry = GeometryEncoder.jsonToJtsGeometry(json);

        QuantizedGeometryEncoder geometryEncoder = new QuantizedGeometryEncoder(
                QuantizedGeometryEncoder.Mode.view,
                QuantizedGeometryEncoder.OriginPosition.upperLeft,
                38.21851414253776,
                new Envelope(-17787495.232546896,19386896.43321582,1495796.1696307966,9570643.396627536));

        Geometry outputGeometry = geometryEncoder.toRepresentation(inputGeometry, new SpatialReferenceWKID(testSRID));

        String outputJson = new GeoServicesJacksonJsonConverter().getMapper().writeValueAsString(outputGeometry);
        /* This is what actually gets returned by ArcGIS; it has strange artifacts like [0,1],[0,-1]
        String expectedJson =
                "{\"geometryType\":\"esriGeometryPolygon\"," +
                "\"spatialReference\":{\"wkid\":102100,\"latestWkid\":102100}," +
                "\"rings\":[" +
                    "[" +
                        "[108049,91771],[0,1],[0,-1],[1,15],[-6,0],[-1,17],[0,15],[0,16],[0,16],[0,16],[0,15],[6,0]," +
                        "[0,8],[-6,0],[0,16],[0,15],[6,0],[0,9],[0,2],[-1,0],[0,2],[1,0],[-1,19],[-10,0],[-13,0]," +
                        "[0,8],[0,9],[-8,0],[0,12],[0,11],[0,16],[0,15],[-16,0],[-15,5],[-16,6],[-15,-1],[0,-10]," +
                        "[0,-5],[0,-1],[0,-1],[0,-1],[0,-1],[6,-2],[0,-1],[-1,0],[0,-1],[-1,0],[-9,-9],[-2,6]," +
                        "[-6,-5],[-1,0],[-1,-1],[-1,0],[0,-1],[0,-1],[-1,0],[0,-1],[-3,-6],[0,-1],[0,-1],[-1,0]," +
                        "[0,-1],[-1,-1],[0,-1],[-1,0],[0,-1],[-1,-1],[-1,0],[-1,-1],[-1,-1],[0,-1],[-1,0],[0,-1]," +
                        "[-1,-1],[0,-1],[-1,-1],[0,-2],[-1,-1],[0,-1],[0,-1],[0,-1],[-1,0],[0,-1],[0,-8],[-2,-16]," +
                        "[-1,-1],[0,-2],[0,-2],[0,-1],[0,-2],[-2,-14],[0,-1],[0,-1],[0,-1],[-1,-1],[0,-1],[0,-1]," +
                        "[-1,-1],[0,-1],[-4,-10],[-4,-11],[0,-1],[-6,-13],[0,-1],[0,-1],[-1,-1],[0,-1],[0,-1],[0,-1]," +
                        "[-1,-1],[0,-1],[0,-1],[0,-1],[0,-1],[0,-1],[0,-1],[1,-1],[0,-1],[0,-1],[0,-1],[1,-1],[0,-1]," +
                        "[0,-1],[1,-1],[0,-1],[1,-1],[0,-1],[1,0],[3,-4],[0,-5],[-8,0],[0,-8],[-7,0],[-4,0],[0,-1]," +
                        "[2,-2],[3,-4],[4,-11],[3,-11],[-4,-6],[1,-1],[1,-1],[4,-1],[4,-2],[4,-7],[2,-6],[-1,-4]," +
                        "[-4,-7],[0,-1],[2,-2],[4,-2],[5,0],[4,-4],[5,-3],[6,-3],[3,-1],[4,-7],[3,-11],[2,-5],[0,-8]," +
                        "[0,-2],[9,-4],[5,-4],[2,-12],[-2,-9],[1,-8],[4,-5],[2,-4],[0,-9],[6,-16],[3,-4],[5,-6]," +
                        "[1,-8],[5,-1],[5,-1],[5,-7],[4,-4],[7,-1],[4,-4],[0,1],[5,3],[1,0],[0,4],[4,0],[15,8]," +
                        "[13,8],[0,16],[0,16],[0,16],[0,15],[0,15],[0,14],[-1,1],[0,1],[0,1],[0,1],[0,15],[0,14]," +
                        "[0,1]" +
                    "]" +
                "]}";
        */

        String expectedJson =
                "{\"geometryType\":\"esriGeometryPolygon\"," +
                        "\"spatialReference\":{\"wkid\":102100,\"latestWkid\":102100}," +
                        "\"rings\":[" +
                        "[" +
                        "[108049,91771],[0,1],[1,14],[-6,0],[-1,17],[0,15],[0,16],[0,16],[0,16],[0,15],[6,0]," +
                        "[0,8],[-6,0],[0,16],[0,15],[6,0],[0,9],[0,2],[-1,2],[1,0],[-1,19],[-10,0],[-13,0]," +
                        "[0,8],[0,9],[-8,0],[0,12],[0,11],[0,16],[0,15],[-16,0],[-15,5],[-16,6],[-15,-1],[0,-10]," +
                        "[0,-5],[0,-1],[0,-1],[0,-1],[0,-1],[6,-2],[0,-1],[-1,0],[0,-1],[-1,0],[-9,-9],[-2,6]," +
                        "[-6,-5],[-1,0],[-1,-1],[-1,0],[0,-1],[0,-1],[-1,0],[0,-1],[-3,-6],[0,-1],[0,-1],[-1,0]," +
                        "[0,-1],[-1,-1],[0,-1],[-1,0],[0,-1],[-1,-1],[-1,0],[-1,-1],[-1,-1],[0,-1],[-1,0],[0,-1]," +
                        "[-1,-1],[0,-1],[-1,-1],[0,-2],[-1,-1],[0,-1],[0,-1],[0,-1],[-1,0],[0,-1],[0,-8],[-2,-16]," +
                        "[0,-1],[-1,-2],[0,-2],[0,-1],[0,-2],[-2,-14],[0,-1],[0,-1],[0,-1],[-1,-1],[0,-1],[0,-1]," +
                        "[-1,-1],[0,-1],[-4,-10],[-4,-11],[0,-1],[-6,-13],[0,-1],[0,-1],[-1,-1],[0,-1],[0,-1],[0,-1]," +
                        "[-1,-1],[0,-1],[0,-1],[0,-1],[0,-1],[0,-1],[0,-1],[1,-1],[0,-1],[0,-1],[0,-1],[1,-1],[0,-1]," +
                        "[0,-1],[1,-1],[0,-1],[1,-1],[0,-1],[1,0],[3,-4],[0,-5],[-8,0],[0,-8],[-7,0],[-4,0],[0,-1]," +
                        "[2,-2],[3,-4],[4,-11],[3,-11],[-4,-6],[1,-1],[1,-1],[4,-1],[4,-2],[4,-7],[2,-6],[-1,-4]," +
                        "[-4,-7],[0,-1],[2,-2],[4,-2],[5,0],[4,-4],[5,-3],[6,-3],[3,-1],[4,-7],[3,-11],[2,-5],[0,-8]," +
                        "[0,-2],[9,-4],[5,-4],[2,-12],[-2,-9],[1,-8],[4,-5],[2,-4],[0,-9],[6,-16],[3,-4],[5,-6]," +
                        "[1,-8],[5,-1],[5,-1],[5,-7],[4,-4],[7,-1],[4,-4],[0,1],[5,3],[1,0],[0,4],[4,0],[15,8]," +
                        "[13,8],[0,16],[0,16],[0,16],[0,15],[0,15],[0,14],[-1,1],[0,1],[0,1],[0,1],[0,15],[0,14]," +
                        "[0,1]" +
                        "]" +
                        "]}";

        assertEquals(expectedJson, outputJson);
    }
}
