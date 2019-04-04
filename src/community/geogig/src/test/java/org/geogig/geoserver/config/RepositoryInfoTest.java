/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import org.junit.Test;

/** Unit tests for the RepositoryInfo object. */
public class RepositoryInfoTest {

    @Test
    public void testLocationMaskUtility() {
        // make sure that, given a repository name, the masking utility reutrns the expected masked
        // location URI
        final String repositoryName = "myRepo";
        final String actualMaskedLocation =
                GeoServerGeoGigRepositoryResolver.getURI(repositoryName);
        assertEquals(
                "Location masking produced unexpected URI",
                "geoserver://myRepo",
                actualMaskedLocation);
    }

    @Test
    public void testMaksedLocation() {
        final RepositoryInfo repositoryInfo = new RepositoryInfo();
        // make a location URI and set it
        final String fakeRepoName = "fakeRepo";
        final URI fakeUri = URI.create("file:///tmp/" + fakeRepoName);
        repositoryInfo.setLocation(fakeUri);
        // get the name
        final String reposiotoryName = repositoryInfo.getRepoName();
        // get the maksed location
        final String actualMaskedLocation = repositoryInfo.getMaskedLocation();
        // assert the masked location matches the resolver maksing pattern
        final String expectedMaskedLocation =
                GeoServerGeoGigRepositoryResolver.getURI(reposiotoryName);
        assertEquals(
                "Masked location value does not match Resolver pattern",
                expectedMaskedLocation,
                actualMaskedLocation);
        final String uriPrefix = GeoServerGeoGigRepositoryResolver.GEOSERVER_URI_SCHEME + "://";
        assertTrue(
                String.format("Maksed URI doesn't have '%s' prefix", uriPrefix),
                actualMaskedLocation.startsWith(uriPrefix));
        final String actualRepoName =
                actualMaskedLocation.substring(GeoServerGeoGigRepositoryResolver.SCHEME_LENGTH);
        assertEquals("Unexpected Repository name in masked location", fakeRepoName, actualRepoName);
    }
}
