/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.geoserver.platform.resource.ResourceMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource.Type;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * JUnit Theory test class for Resource invariants. Subclasses should provide representative
 * DataPoints to test.
 *
 * @author Kevin Smith, Boundless
 */
@RunWith(Theories.class)
public abstract class ResourceTheoryTest {

    @Rule public ExpectedException exception = ExpectedException.none();

    protected abstract Resource getResource(String path) throws Exception;

    /** @return a resource that is not a data point of type DIRECTORY. */
    protected abstract Resource getDirectory();
    /** @return a resource that is not a data point of type RESOURCE. */
    protected abstract Resource getResource();
    /** @return a resource that is not a data point of type UNDEFINED. */
    protected abstract Resource getUndefined();

    @Theory
    public void theoryNotNull(String path) throws Exception {
        Resource res = getResource(path);

        assertThat(res, notNullValue());
    }

    @Theory
    public void theoryExtantHaveDate(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, defined());

        long result = res.lastmodified();

        assertThat(result, notNullValue());
    }

    @Theory
    public void theoryHaveSamePath(String path) throws Exception {
        Resource res = getResource(path);

        String result = res.path();

        assertThat(result, is(equalTo(path)));
    }

    @Theory
    public void theoryHaveName(String path) throws Exception {
        Resource res = getResource(path);

        String result = res.name();

        assertThat(result, notNullValue());
    }

    @Theory
    public void theoryNameIsEndOfPath(String path) throws Exception {
        Resource res = getResource(path);

        List<String> elements = Paths.names(path);
        String lastElement = elements.get(elements.size() - 1);

        String result = res.name();

        assertThat(result, equalTo(lastElement));
    }

    @Theory
    public void theoryLeavesHaveIstream(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, is(resource()));

        try (InputStream result = res.in()) {
            assertThat(result, notNullValue());
        }
    }

    @Theory
    public void theoryLeavesHaveOstream(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, is(resource()));

        try (OutputStream result = res.out()) {
            assertThat(result, notNullValue());
        }
    }

    @Theory
    public void theoryUndefinedHaveIstreamAndBecomeResource(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, is(undefined()));

        try (InputStream result = res.in()) {
            assertThat(result, notNullValue());
            assertThat(res, is(resource()));
        }
    }

    @Theory
    public void theoryUndefinedHaveOstreamAndBecomeResource(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, is(undefined()));

        try (OutputStream result = res.out()) {
            assertThat(result, notNullValue());
            assertThat(res, is(resource()));
        }
    }

    @Theory
    public void theoryNonDirectoriesPersistData(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, not(directory()));

        byte[] test = {42, 29, 32, 120, 69, 0, 1};

        try (OutputStream ostream = res.out()) {
            ostream.write(test);
        }

        byte[] result = new byte[test.length];

        try (InputStream istream = res.in()) {
            istream.read(result);
            assertThat(istream.read(), is(-1));
        }
        assertThat(result, equalTo(test));
    }

    @Theory
    public void theoryDirectoriesHaveNoIstreams(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));

        exception.expect(IllegalStateException.class);
        res.in().close();
    }

    @Theory
    public void theoryDirectoriesHaveNoOstream(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));

        exception.expect(IllegalStateException.class);
        res.out().close();
    }

    @Theory
    public void theoryLeavesHaveEmptyListOfChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(resource()));

        Collection<Resource> result = res.list();

        assertThat(result, empty());
    }

    @Theory
    public void theoryUndefinedHaveEmptyListOfChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(undefined()));

        Collection<Resource> result = res.list();

        assertThat(result, empty());
    }

    @Theory
    public void theoryDirectoriesHaveChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));

        Collection<Resource> result = res.list();

        assertThat(result, notNullValue());
    }

    @Theory
    public void theoryChildrenKnowTheirParents(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        Collection<Resource> children = res.list();
        assumeThat(children, not(empty())); // Make sure this resource has children

        for (Resource child : children) {
            Resource parent = child.parent();
            assertThat(parent, equalTo(res));
        }
    }

    @Theory
    public void theoryParentsKnowTheirChildren(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, is(directory()));
        Resource parent = res.parent();
        assumeThat(path, parent, notNullValue()); // Make sure this resource has a parent

        Collection<Resource> result = parent.list();

        assertThat(path, result, hasItem(res)); // this assumed equals was written!
    }

    @Theory
    public void theorySamePathGivesEquivalentResource(String path) throws Exception {
        Resource res1 = getResource(path);
        Resource res2 = getResource(path);

        assertThat(res2, equalTo(res1));
    }

    @Theory
    public void theoryParentIsDirectory(String path) throws Exception {
        Resource res = getResource(path);
        Resource parent = res.parent();
        assumeThat(path + " not root", parent, notNullValue());

        if (res.getType() != Type.UNDEFINED) {
            assertThat(path + " directory", parent, is(directory()));
        }
    }

    @Theory
    public void theoryHaveFile(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, resource());

        File result = res.file();

        assertThat(result, notNullValue());
    }

    @Theory
    public void theoryHaveDir(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, directory());

        File result = res.dir();

        assertThat(result, notNullValue());
    }

    @Theory
    public void theoryDeletedResourcesAreUndefined(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, resource());

        assertThat(res.delete(), is(true));
        assertThat(res, undefined());
    }

    @Theory
    public void theoryUndefinedNotDeleted(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, undefined());

        assertThat(res.delete(), is(false));
        assertThat(res, undefined());
    }

    @Theory
    public void theoryRenamedAreUndefined(String path) throws Exception {
        Resource res = getResource(path);
        assumeThat(res, defined());

        Resource target = getUndefined();
        assertThat(res.renameTo(target), is(true));
        assertThat(res, undefined());
    }

    @Theory
    public void theoryRenamedResourcesAreEquivalent(String path) throws Exception {
        final Resource res = getResource(path);
        assumeThat(res, resource());

        final byte[] expectedContent;
        try (InputStream in = res.in()) {
            expectedContent = IOUtils.toByteArray(in);
        }

        final Resource target = getUndefined();
        assertThat(res.renameTo(target), is(true));
        assertThat(target, resource());

        final byte[] resultContent;
        try (InputStream in = target.in()) {
            resultContent = IOUtils.toByteArray(in);
        }

        assertThat(resultContent, equalTo(expectedContent));
    }

    @Theory
    public void theoryNonDirectoriesHaveFileWithSameContents(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, not(directory()));

        byte[] test = {42, 29, 32, 120, 69, 0, 1};

        try (OutputStream ostream = res.out()) {
            ostream.write(test);
        }

        byte[] result = new byte[test.length];

        try (InputStream istream = new FileInputStream(res.file())) {
            istream.read(result);
            assertThat(istream.read(), is(-1));
        }
        assertThat(result, equalTo(test));
    }

    @Theory
    public void theoryDirectoriesHaveFileWithSameNamedChildren(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, is(directory()));

        File dir = res.dir();

        Collection<Resource> resChildren = res.list();
        String[] fileChildrenNames = dir.list();

        String[] resChildrenNames = new String[resChildren.size()];

        int i = 0;
        for (Resource child : resChildren) {
            resChildrenNames[i] = child.name();
            i++;
        }

        assertThat(fileChildrenNames, arrayContainingInAnyOrder(resChildrenNames));
    }

    // This is the behaviour of the file based implementation. Should this be required or left
    // undefined with clear documentation indicating that it's implementation dependent?
    // @Ignore
    @Theory
    public void theoryAlteringFileAltersResource(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, not(directory()));

        byte[] testResource = {42, 29, 32, 120, 69, 0, 1};
        byte[] testFile = {27, 3, 5, 90, -120, -3};

        // Write to resource
        try (OutputStream ostream = res.out()) {
            ostream.write(testResource);
        }

        // Write to file
        try (OutputStream ostream = new FileOutputStream(res.file())) {
            ostream.write(testFile);
        }

        // Read from resource
        byte[] result = new byte[testFile.length];

        try (InputStream istream = res.in()) {
            istream.read(result);
            assertThat(istream.read(), is(-1));
        }

        // Should be what was written to the file
        assertThat(result, equalTo(testFile));
    }

    // This is the behaviour of the file based implementation. Should this be required or left
    // undefined with clear documentation indicating that it's implementation dependent?
    // @Ignore
    @Theory
    public void theoryAddingFileToDirectoryAddsResource(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, is(directory()));

        File dir = res.dir();

        File file = new File(dir, "newFileCreatedDirectly");

        assumeTrue(file.createNewFile());

        Resource child = getResource(Paths.path(res.path(), "newFileCreatedDirectly"));
        Collection<Resource> children = res.list();

        assertThat(child, is(defined()));

        assertThat(children, hasItem(child));
    }

    @Theory
    public void theoryMultipleOutputStreamsAreSafe(String path) throws Exception {
        final Resource res = getResource(path);
        assumeThat(res, is(resource()));

        final byte[] thread1Content = "This is the content for thread 1".getBytes();
        final byte[] thread2Content = "Thread 2 has this content".getBytes();

        try (OutputStream out1 = res.out()) {
            try (OutputStream out2 = res.out()) {
                for (int i = 0; i < thread1Content.length || i < thread2Content.length; i++) {
                    if (i < thread1Content.length) {
                        out1.write(thread1Content[i]);
                    }
                    if (i < thread2Content.length) {
                        out2.write(thread2Content[i]);
                    }
                }
            }
        }

        final byte[] resultContent;
        try (InputStream in = res.in()) {
            resultContent = IOUtils.toByteArray(in);
        }

        // 2 streams being written to concurrently should result in the resource containing
        // what was written to one of the two streams.
        assertThat(resultContent, anyOf(equalTo(thread1Content), equalTo(thread2Content)));
    }

    @Theory
    public void theoryDoubleClose(String path) throws Exception {
        final Resource res = getResource(path);
        assumeThat(res, is(resource()));

        OutputStream os = res.out();
        os.close();
        os.close();
    }

    @Theory
    public void theoryRecursiveDelete(String path) throws Exception {
        final Resource res = getResource(path);
        assumeThat(res, is(directory()));
        assumeThat(res, is(directory()));

        Collection<Resource> result = res.list();
        assumeThat(result.size(), greaterThan(0));

        assertTrue(res.delete());
    }

    @Theory
    public void theoryRootSlashIsIgnored(String path) throws Exception {
        final Resource res = getResource(path);
        final Resource res2 = getResource("/" + path);
        assertTrue(res.equals(res2));
        assertTrue(res.path().equals(res2.path()));
    }
}
