/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public abstract class TestSupport {

    private static final File TEST_DIRECTORY =
            new File(System.getProperty("java.io.tmpdir"), "params-extractor-data-directory");

    protected static final ApplicationContext APPLICATION_CONTEXT =
            new FileSystemXmlApplicationContext(
                    "file:"
                            + TestSupport.class
                                    .getClassLoader()
                                    .getResource("testApplicationContext.xml")
                                    .getFile());

    protected ResourceStore resourceStore;

    @Before
    public void voidSetup() throws IOException {
        deleteTestDirectoryQuietly();
        TEST_DIRECTORY.mkdir();
        resourceStore = new FileSystemResourceStore(TEST_DIRECTORY);
    }

    @After
    public void voidClean() throws IOException {
        deleteTestDirectoryQuietly();
    }

    private void deleteTestDirectoryQuietly() {
        try {
            IOUtils.delete(TEST_DIRECTORY);
        } catch (Exception exception) {
        }
    }

    protected static void doWork(String resourcePath, Consumer<InputStream> consumer)
            throws Exception {
        URL resource = EchoParametersDaoTest.class.getClassLoader().getResource(resourcePath);
        assertThat(resource, notNullValue());
        File file = new File(resource.getFile());
        assertThat(file.exists(), is(true));
        try (InputStream inputStream = new FileInputStream(file)) {
            if (inputStream.available() == 0) {
                return;
            }
            consumer.accept(inputStream);
        }
    }

    protected void checkRule(Rule ruleA, Rule ruleB) {
        assertThat(ruleA, notNullValue());
        assertThat(ruleB, notNullValue());
        checkValue(ruleA, ruleB, Rule::getId);
        checkValue(ruleA, ruleB, Rule::getActivated);
        checkValue(ruleA, ruleB, Rule::getPosition);
        checkValue(ruleA, ruleB, Rule::getMatch);
        checkValue(ruleA, ruleB, Rule::getParameter);
        checkValue(ruleA, ruleB, Rule::getActivation);
        checkValue(ruleA, ruleB, Rule::getTransform);
        checkValue(ruleA, ruleB, Rule::getRemove);
        checkValue(ruleA, ruleB, Rule::getCombine);
    }

    protected void checkEchoParameter(EchoParameter echoParameterA, EchoParameter echoParameterB) {
        assertThat(echoParameterA, notNullValue());
        assertThat(echoParameterB, notNullValue());
        checkValue(echoParameterA, echoParameterB, EchoParameter::getId);
        checkValue(echoParameterA, echoParameterB, EchoParameter::getActivated);
        checkValue(echoParameterA, echoParameterB, EchoParameter::getParameter);
    }

    protected <T, R> void checkValue(T objectA, T objectB, Function<T, R> getter) {
        R valueA = getter.apply(objectA);
        R valueB = getter.apply(objectB);
        if (valueA == null) {
            assertThat(valueB, nullValue());
        } else {
            assertThat(valueB, notNullValue());
            assertThat(valueA, is(valueB));
        }
    }

    protected EchoParameter findEchoParameter(String id, List<EchoParameter> rules) {
        return rules.stream()
                .filter(echoParameter -> echoParameter.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    protected Rule findRule(String id, List<Rule> rules) {
        return rules.stream().filter(rule -> rule.getId().equals(id)).findFirst().orElse(null);
    }
}
