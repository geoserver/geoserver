/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.referencing.operation.projection;

import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.MathTransform;

/**
 * Mercator 1SP variation used by Google, which basically requires to accept lat/lon values as
 * spherical coordinates, that is, avoiding to do any conversion from ellipsoid to the sphere.
 *
 * @author Andrea Aime
 * @deprecated Since GeoTools 2.4.0 there is no need to use this custom projection anymore, use the
 *     WKT definition suggested in {@link "https://osgeo-org.atlassian.net/browse/GEOT-1511"}
 *     instead
 */
public class Mercator1SPGoogle extends Mercator {

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameter values in standard units.
     * @throws ParameterNotFoundException if a mandatory parameter is missing.
     */
    protected Mercator1SPGoogle(final ParameterValueGroup parameters)
            throws ParameterNotFoundException {
        super(parameters);
    }

    /** {@inheritDoc} */
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Provider.PARAMETERS;
    }

    /**
     * Provides the transform equations for the spherical case of the Mercator projection.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     * @author Rueben Schulz
     */
    private static final class Spherical extends Mercator.Spherical {
        /**
         * Constructs a new map projection from the suplied parameters.
         *
         * @param parameters The parameter values in standard units.
         * @throws ParameterNotFoundException if a mandatory parameter is missing.
         */
        protected Spherical(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            super(parameters);
        }

        /** {@inheritDoc} */
        public ParameterDescriptorGroup getParameterDescriptors() {
            return Provider.PARAMETERS;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                          ////////
    ////////                                 PROVIDERS                                ////////
    ////////                                                                          ////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The {@linkplain org.geotools.referencing.operation.MathTransformProvider math transform
     * provider} for a {@linkplain Mercator1SP Mercator 1SP} projection (EPSG code 9804).
     *
     * @since 2.2
     * @version $Id$
     * @author Martin Desruisseaux
     * @author Rueben Schulz
     * @see org.geotools.referencing.operation.DefaultMathTransformFactory
     */
    public static class Provider extends AbstractProvider {
        /** The parameters group. */
        static final ParameterDescriptorGroup PARAMETERS =
                createDescriptorGroup(
                        new NamedIdentifier[] {
                            new NamedIdentifier(Citations.OGC, "Mercator_1SP_Google"),
                            new NamedIdentifier(Citations.GEOTOOLS, "Mercator_1SP_Google")
                        },
                        new ParameterDescriptor[] {
                            SEMI_MAJOR,
                            SEMI_MINOR,
                            LATITUDE_OF_ORIGIN,
                            CENTRAL_MERIDIAN,
                            SCALE_FACTOR,
                            FALSE_EASTING,
                            FALSE_NORTHING
                        });

        /** Constructs a new provider. */
        public Provider() {
            super(PARAMETERS);
        }

        /** Returns the operation type for this map projection. */
        public Class getOperationType() {
            return CylindricalProjection.class;
        }

        /**
         * Creates a transform from the specified group of parameter values.
         *
         * @param parameters The group of parameter values.
         * @return The created math transform.
         * @throws ParameterNotFoundException if a required parameter was not found.
         */
        protected MathTransform createMathTransform(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            // make sure we assume a spherical reference
            parameters
                    .parameter("semi_minor")
                    .setValue(parameters.parameter("semi_major").getValue());
            return new Spherical(parameters);
        }
    }
}
