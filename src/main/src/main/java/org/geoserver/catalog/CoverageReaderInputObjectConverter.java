/* (c) 2014 - 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.Optional;
import javax.annotation.Nullable;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.util.factory.Hints;

/**
 * Extension point for {@link ResourcePool} that enables custom input object types when creating
 * {@link GridCoverageReader}s. Implementations may return any custom Object which can in turn be
 * used by {@link org.geotools.coverage.grid.io.AbstractGridFormat#getReader(Object)}
 * implementations to instantiate the appropriate GridCoverageReader and by {@link
 * javax.imageio.spi.ImageInputStreamSpi#getInputClass()} implementations to support the accurate
 * selection of {@link javax.imageio.stream.ImageInputStream} implementations.
 *
 * @author joshfix Created on 2/13/20
 */
public interface CoverageReaderInputObjectConverter<T> {

    /**
     * This method inspects the provided input object in an attempt to convert it to a custom class.
     * Any of the accompanying method parameters may optionally be used to better inform the
     * decision making logic. If an implementation does not support conversion for the given input
     * object, the method should return an empty {@link Optional}.
     *
     * @param input The input object.
     * @param coverageInfo The grid coverage metadata, may be <code>null</code>.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @return an {@link Optional} containing the converted value.
     */
    Optional<T> convert(Object input, @Nullable CoverageInfo coverageInfo, @Nullable Hints hints);

    /**
     * This method inspects the provided input object in an attempt to convert it to a custom class.
     * Any of the accompanying method parameters may optionally be used to better inform the
     * decision making logic. If an implementation does not support conversion for the given input
     * object, the method should return an empty {@link Optional}.
     *
     * @param input The input object.
     * @param coverageInfo The grid coverage metadata, may be <code>null</code>.
     * @param coverageStoreInfo The grid coverage store metadata, may be <code>null</code>.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @return an {@link Optional} containing the converted value.
     */
    Optional<T> convert(
            Object input,
            @Nullable CoverageInfo coverageInfo,
            @Nullable CoverageStoreInfo coverageStoreInfo,
            @Nullable Hints hints);
}
