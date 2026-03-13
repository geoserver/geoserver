/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Utility classes for supporting patching of configuration objects. These are used by the REST API to track which
 * properties of a configuration object have been modified by a patch request, and to apply those changes to the
 * existing configuration object.
 *
 * <p>The supported ways to nullify a property via patch are:
 *
 * <ul>
 *   <li>For JSON, setting an attribute explicitly to null, e.g. <code>"myAttribute": null</code>
 *   <li>For XML, using the xs:n:nil attribute, e.g. <code>&lt;myAttribute xsi:nil="true"/&gt;</code>
 * </ul>
 *
 * The complexity of this package stems from a number of concurrent issues:
 *
 * <ul>
 *   <li>GeoServer abuses the PUT operation as a sort of patch system, in other words, it allows to put partial
 *       documents and handles them as "the only bits that need to be updated"
 *   <li>XStream is used to parse the payloads, and it turns any explicit null reference into an empty string instead
 *   <li>The actual patching is performed, eventually, via {@link org.geoserver.ows.util.OwsUtils#copy(java.lang.Object,
 *       java.lang.Object, java.lang.Class)}, which takes a source and a target bean, and applies all the non-null
 *       properties in the input bean (the one received via PUT) to the target bean (the one coming from the catalog)
 *   <li>Patch behavior relies on being able to distinguish between "this property was not present in the payload, so it
 *       should be ignored" and "this property was explicitly set to null in the payload, so it should be set to null"
 *   <li>As a final complication, XStream serializes/deserializes based on field names, but
 *       {@link org.geoserver.ows.util.OwsUtils} works on bean properties, so we need to be able to correlate the two to
 *       determine which bean property is being set when we see an explicit null in the XML/JSON. Most of the time the
 *       two ends use the same name, but there is a number of cases where the field name is different, either due to
 *       reserved keywords being used (e.g., abstract vs abstractTxt), historical reasons (layers vs published) or
 *       simple case differences on acronyms (URI vs uri)
 * </ul>
 *
 * Given all the above, the tracking of null in patch operations is implemented as follows:
 *
 * <ol>
 *   <li>When a REST patch operation starts, a new {@link org.geoserver.config.util.patch.PatchContext} is created and
 *       associated with the current thread, from a REST dispatcher callback.
 *   <li>{@link org.geoserver.config.util.XStreamPersisterFactory} recognizes the active patch context and installs low
 *       level XStream class replacements allowing to track nulls during the parsing:
 *       <ul>
 *         <li>For XML, it creates a {@link org.geoserver.config.util.patch.PatchTrackingDriver} wrapping the default
 *             XppDriver, which is responsible for tracking the current XML path being deserialized and associating it
 *             with null detections via XML attributes (the class is used also for JSON, in that case a tracker string
 *             is used)
 *         <li>For JSON, it creates a {@link org.geoserver.config.util.patch.PatchTrackingDriver} wrapping a
 *             {@link org.geoserver.config.util.patch.NullAwareJettisonMappedXmlDriver}, a specialized driver that is
 *             able to detect explicit null values before XStream default machinery turns them into empty strings
 *       </ul>
 *   <li>In a similar way, {@link org.geoserver.config.util.SecureXStream} recognizes the active patch context and wraps
 *       the property mapper into a {@link org.geoserver.config.util.patch.PatchTrackingMapper} maps the XML/JSON
 *       serialized property name into a class field name, following XStream configured aliases. This class is also
 *       responsible for mapping the field into a bean property name, in case the two are mismatched. Once found, the
 *       mapping is sent to {@link org.geoserver.config.util.patch.PatchContext}
 *   <li>Finally, {@link org.geoserver.catalog.CatalogBuilder} update method is aware of the patching machinery, and
 *       sets up an appropriate copy policy for {@link org.geoserver.ows.util.OwsUtils}, with null tracking if the patch
 *       context is active. This allows to correctly apply the changes from the input bean to the existing configuration
 *       bean, including null values.
 * </ol>
 */
package org.geoserver.config.util.patch;
