/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.awt.RenderingHints.Key;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.text.Text;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Process factory that maintains backward compatibility for deprecated process names.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DeprecatedProcessFactory
        implements ProcessFactory, DisposableBean, ApplicationListener<ApplicationEvent> {

    volatile Map<Name, Name> map;

    GeoServer geoServer;
    ConfigurationListener listener;

    public DeprecatedProcessFactory() {
        Processors.addProcessFactory(this);
    }

    @Override
    public void destroy() throws Exception {
        if (geoServer != null) {
            geoServer.removeListener(listener);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return null;
    }

    @Override
    public InternationalString getTitle() {
        return Text.text("Deprecated processes");
    }

    @Override
    public Set<Name> getNames() {
        return getProcessMappings().keySet();
    }

    @Override
    public InternationalString getTitle(Name name) {
        return doAction(
                name,
                new Action<InternationalString>() {
                    @Override
                    public InternationalString perform(
                            ProcessFactory f, Name oldName, Name newName, Object... args) {
                        return f.getTitle(newName);
                    }
                });
    }

    @Override
    public InternationalString getDescription(Name name) {
        return doAction(
                name,
                new Action<InternationalString>() {
                    @Override
                    public InternationalString perform(
                            ProcessFactory f, Name oldName, Name newName, Object... args) {
                        return f.getDescription(newName);
                    }
                });
    }

    @Override
    public String getVersion(Name name) {
        return doAction(
                name,
                new Action<String>() {
                    @Override
                    public String perform(
                            ProcessFactory f, Name oldName, Name newName, Object... args) {
                        return f.getVersion(newName);
                    }
                });
    }

    @Override
    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        return doAction(
                name,
                new Action<Map<String, Parameter<?>>>() {
                    @Override
                    public Map<String, Parameter<?>> perform(
                            ProcessFactory f, Name oldName, Name newName, Object... args) {
                        return f.getParameterInfo(newName);
                    }
                });
    }

    @Override
    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> parameters)
            throws IllegalArgumentException {
        return doAction(
                name,
                new Action<Map<String, Parameter<?>>>() {
                    @Override
                    public Map<String, Parameter<?>> perform(
                            ProcessFactory f, Name oldName, Name newName, Object... args) {
                        return f.getResultInfo(newName, (Map<String, Object>) args[0]);
                    }
                },
                parameters);
    }

    @Override
    public boolean supportsProgress(Name name) {
        Boolean b =
                doAction(
                        name,
                        new Action<Boolean>() {
                            @Override
                            public Boolean perform(
                                    ProcessFactory f, Name oldName, Name newName, Object... args) {
                                return f.supportsProgress(newName);
                            }
                        });
        return b != null ? b : false;
    }

    @Override
    public Process create(Name name) {
        return doAction(
                name,
                new Action<Process>() {
                    @Override
                    public Process perform(
                            ProcessFactory f, Name oldName, Name newName, Object... args) {
                        return f.create(newName);
                    }
                });
    }

    interface Action<T> {
        T perform(ProcessFactory f, Name oldName, Name newName, Object... args);
    }

    <T> T doAction(Name oldName, Action<T> a, Object... args) {
        Map<Name, Name> map = getProcessMappings();
        if (map.containsKey(oldName)) {
            Name newName = map.get(oldName);
            ProcessFactory pf = GeoServerProcessors.createProcessFactory(newName, false);
            if (pf != null) {
                return a.perform(pf, oldName, newName, args);
            }
        }
        return null;
    }

    Map<Name, Name> getProcessMappings() {
        if (map == null) {
            synchronized (DeprecatedProcessFactory.class) {
                if (map == null) {
                    map = new LinkedHashMap();

                    // JTS namespace
                    registerProcessMapping(
                            new NameImpl("JTS", "length"), new NameImpl("geo", "length"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "isEmpty"), new NameImpl("geo", "isEmpty"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "contains"), new NameImpl("geo", "contains"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "disjoint"), new NameImpl("geo", "disjoint"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "intersects"),
                            new NameImpl("geo", "intersects"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "isClosed"), new NameImpl("geo", "isClosed"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "isValid"), new NameImpl("geo", "isValid"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "buffer"), new NameImpl("geo", "buffer"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "getY"), new NameImpl("geo", "getY"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "getX"), new NameImpl("geo", "getX"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "union"), new NameImpl("geo", "union"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "intersection"),
                            new NameImpl("geo", "intersection"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "difference"),
                            new NameImpl("geo", "difference"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "distance"), new NameImpl("geo", "distance"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "envelope"), new NameImpl("geo", "envelope"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "dimension"),
                            new NameImpl("geo", "dimension"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "overlaps"), new NameImpl("geo", "overlaps"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "reproject"),
                            new NameImpl("geo", "reproject"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "simplify"), new NameImpl("geo", "simplify"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "isSimple"), new NameImpl("geo", "isSimple"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "equalsExact"),
                            new NameImpl("geo", "equalsExact"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "getGeometryN"),
                            new NameImpl("geo", "getGeometryN"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "isWithinDistance"),
                            new NameImpl("geo", "isWithinDistance"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "touches"), new NameImpl("geo", "touches"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "crosses"), new NameImpl("geo", "crosses"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "within"), new NameImpl("geo", "within"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "relate"), new NameImpl("geo", "relate"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "convexHull"),
                            new NameImpl("geo", "convexHull"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "symDifference"),
                            new NameImpl("geo", "symDifference"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "centroid"), new NameImpl("geo", "centroid"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "interiorPoint"),
                            new NameImpl("geo", "interiorPoint"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "numPoints"),
                            new NameImpl("geo", "numPoints"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "area"), new NameImpl("geo", "area"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "isRing"), new NameImpl("geo", "isRing"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "exteriorRing"),
                            new NameImpl("geo", "exteriorRing"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "numInteriorRing"),
                            new NameImpl("geo", "numInteriorRing"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "numGeometries"),
                            new NameImpl("geo", "numGeometries"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "geometryType"),
                            new NameImpl("geo", "geometryType"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "boundary"), new NameImpl("geo", "boundary"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "densify"), new NameImpl("geo", "densify"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "relatePattern"),
                            new NameImpl("geo", "relatePattern"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "equalsExactTolerance"),
                            new NameImpl("geo", "equalsExactTolerance"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "pointN"), new NameImpl("geo", "pointN"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "startPoint"),
                            new NameImpl("geo", "startPoint"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "endPoint"), new NameImpl("geo", "endPoint"), map);
                    registerProcessMapping(
                            new NameImpl("JTS", "interiorRingN"),
                            new NameImpl("geo", "interiorRingN"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "polygonize"),
                            new NameImpl("geo", "polygonize"),
                            map);
                    registerProcessMapping(
                            new NameImpl("JTS", "splitPolygon"),
                            new NameImpl("geo", "splitPolygon"),
                            map);

                    // gs geometry processes
                    registerProcessMapping(
                            new NameImpl("gs", "ReprojectGeometry"),
                            new NameImpl("geo", "reproject"),
                            map);

                    // gs feature processes
                    registerProcessMapping(
                            new NameImpl("gs", "Aggregate"), new NameImpl("vec", "Aggregate"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Bounds"), new NameImpl("vec", "Bounds"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "BufferFeatureCollection"),
                            new NameImpl("vec", "BufferFeatureCollection"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Centroid"), new NameImpl("vec", "Centroid"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Clip"), new NameImpl("vec", "Clip"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "CollectGeometries"),
                            new NameImpl("vec", "CollectGeometries"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Count"), new NameImpl("vec", "Count"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Feature"), new NameImpl("vec", "Feature"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Grid"), new NameImpl("vec", "Grid"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "InclusionFeatureCollection"),
                            new NameImpl("vec", "InclusionFeatureCollection"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "IntersectionFeatureCollection"),
                            new NameImpl("vec", "IntersectionFeatureCollection"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "LRSGeocode"),
                            new NameImpl("vec", "LRSGeocode"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "LRSMeasure"),
                            new NameImpl("vec", "LRSMeasure"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "LRSSegment"),
                            new NameImpl("vec", "LRSSegment"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Nearest"), new NameImpl("vec", "Nearest"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "PointBuffers"),
                            new NameImpl("vec", "PointBuffers"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "PointStacker"),
                            new NameImpl("vec", "PointStacker"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Query"), new NameImpl("vec", "Query"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "RectangularClip"),
                            new NameImpl("vec", "RectangularClip"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Reproject"), new NameImpl("vec", "Reproject"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Simplify"), new NameImpl("vec", "Simplify"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Snap"), new NameImpl("vec", "Snap"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Transform"), new NameImpl("vec", "Transform"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "UnionFeatureCollection"),
                            new NameImpl("vec", "UnionFeatureCollection"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Unique"), new NameImpl("vec", "Unique"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "VectorZonalStatistics"),
                            new NameImpl("vec", "VectorZonalStatistics"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "Heatmap"), new NameImpl("vec", "Heatmap"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "BarnesSurface"),
                            new NameImpl("vec", "BarnesSurface"),
                            map);

                    // gs raster processes
                    registerProcessMapping(
                            new NameImpl("gs", "AddCoverages"),
                            new NameImpl("ras", "AddCoverages"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "AreaGrid"), new NameImpl("ras", "AreaGrid"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "Contour"), new NameImpl("ras", "Contour"), map);
                    registerProcessMapping(
                            new NameImpl("gs", "CropCoverage"),
                            new NameImpl("ras", "CropCoverage"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "MultiplyCoverages"),
                            new NameImpl("ras", "MultiplyCoverages"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "PolygonExtraction"),
                            new NameImpl("ras", "PolygonExtraction"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "RangeLookup"),
                            new NameImpl("ras", "RangeLookup"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "RasterAsPointCollection"),
                            new NameImpl("ras", "RasterAsPointCollection"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "RasterZonalStatistics"),
                            new NameImpl("ras", "RasterZonalStatistics"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "ScaleCoverage"),
                            new NameImpl("ras", "ScaleCoverage"),
                            map);
                    registerProcessMapping(
                            new NameImpl("gs", "StyleCoverage"),
                            new NameImpl("ras", "StyleCoverage"),
                            map);

                    // gt vector processes
                    registerProcessMapping(
                            new NameImpl("gt", "VectorToRaster"),
                            new NameImpl("vec", "VectorToRaster"),
                            map);
                }
            }
        }
        return map;
    }

    void resetProcessMappings() {
        map = null;
    }

    static void registerProcessMapping(Name oldName, Name newName, Map<Name, Name> map) {
        if (GeoServerProcessors.createProcess(newName) != null) {
            map.put(oldName, newName);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // add and remove the process factory as necessary
        if (event instanceof ContextLoadedEvent) {
            // JD: look up GeoServer and register now rather than use constructor injection
            // to avoid circular dependency during service loading startup
            geoServer =
                    GeoServerExtensions.bean(
                            GeoServer.class, ((ContextLoadedEvent) event).getApplicationContext());
            listener =
                    new ConfigurationListenerAdapter() {
                        @Override
                        public void handleServiceChange(
                                ServiceInfo service,
                                List<String> propertyNames,
                                List<Object> oldValues,
                                List<Object> newValues) {
                            if (service instanceof WPSInfo) {
                                resetProcessMappings();
                            }
                        }
                    };
            geoServer.addListener(listener);
        }
        if (event instanceof ContextRefreshedEvent) {
            Processors.addProcessFactory(this);
        } else if (event instanceof ContextClosedEvent) {
            Processors.removeProcessFactory(this);
        }
    }
}
