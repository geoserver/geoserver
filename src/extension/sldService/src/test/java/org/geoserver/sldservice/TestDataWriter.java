package org.geoserver.sldservice;

import java.io.File;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

public class TestDataWriter {

    public static void main(String[] args) throws Exception {
        float[][] data = new float[100][100];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = i + j > 100 ? 10 : 0;
            }
        }

        GridCoverageFactory factory = new GridCoverageFactory();

        GridCoverage2D g2d =
                factory.create(
                        "test",
                        data,
                        new ReferencedEnvelope(0, 100, 0, 100, CRS.decode("EPSG:3857", true)));
        GeoTiffWriter writer =
                new GeoTiffWriter(
                        new File(
                                "/home/aaime/devel/git-gs/src/extension/sldService/src/test/resources/org/geoserver/sldservice/singleValueNoData.tif"));
        writer.write(g2d, null);
    }
}
