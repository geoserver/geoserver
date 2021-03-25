/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import org.geotools.coverage.grid.GridCoverage2D;

@JsonPropertyOrder({"type", "dataType", "axisNames", "shape", "values"})
public class NdArray extends CoverageJson {

    private static final String TYPE = "NdArray";

    @JsonProperty(required = true)
    private String dataType;

    @JsonProperty(required = true)
    private List<Integer> shape;

    @JsonProperty(required = true)
    private List<String> axisNames;

    @JsonIgnore private List<GridCoverage2D> coverages;

    public NdArray(int dataType, Domain domain, List<GridCoverage2D> coverages) {
        super(TYPE);
        this.dataType = getDataType(dataType);
        Map<String, Axis> axes = domain.getAxes();
        buildAxisNames(axes);
        buildShape(axes);
        this.coverages = coverages;
    }

    private void buildAxisNames(Map<String, Axis> axes) {
        // the input map is a LinkedHashMap
        // Let's get the axis in reverse order
        axisNames = new ArrayList<>();
        axisNames.addAll(axes.keySet());
        Collections.reverse(axisNames);
    }

    private void buildShape(Map<String, Axis> axes) {
        shape = new ArrayList<>(axes.size());
        for (String axisName : axisNames) {
            shape.add(axes.get(axisName).getSize());
        }
    }

    private static String getDataType(int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                return "integer";
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                return "float";
            default:
                return "string";
        }
    }

    @JsonProperty("values")
    public Iterator<Number> getValues() {

        return new Iterator<Number>() {
            int shapeSize = shape.size();
            int numRows = shape.get(shapeSize - 2);
            int numColumns = shape.get(shapeSize - 1);
            int numCoverages = coverages.size();
            int currentCoverage = 0;
            int currentRow = 0;
            int currentColumn = 0;

            // TODO: maybe deal with a tiled approach when images are big
            RenderedImage image = coverages.get(0).getRenderedImage();
            int minX = image.getMinX();
            int minY = image.getMinY();
            Sampler sampler = getSampler();

            @Override
            public boolean hasNext() {
                if (currentColumn < numColumns) return true;
                // start from next row
                currentColumn = 0;
                currentRow++;
                if (currentRow < numRows) return true;
                // Start from next coverage if available
                currentRow = 0;
                currentCoverage++;
                sampler.done();
                if (currentCoverage < numCoverages) {
                    sampler = getSampler();
                    return true;
                }

                return false;
            }

            private Sampler getSampler() {
                // Get a sampler for the current coverage
                RenderedImage ri = coverages.get(currentCoverage).getRenderedImage();
                RandomIter iterator = RandomIterFactory.create(ri, null);
                return Sampler.create(ri.getSampleModel().getDataType(), iterator);
            }

            @Override
            public Number next() {
                // TODO: think about coverages made of multiple bands/parameters
                return sampler.getSample(minX + currentColumn++, minY + currentRow, 0);
            }
        };
    }
}
