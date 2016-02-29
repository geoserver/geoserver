package org.geoserver.params.extractor;

import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public abstract class TestSupport {

    private static final File TEST_DIRECTORY = new File(System.getProperty("java.io.tmpdir"), "params-extractor-data-directory");

    protected static final ApplicationContext APPLICATION_CONTEXT = new FileSystemXmlApplicationContext("file:" +
            TestSupport.class.getClassLoader().getResource("testApplicationContext.xml").getFile());

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

    protected static abstract class DoWork {

        public DoWork(String resourcePath) {
            URL resource = RulesDaoTest.class.getClassLoader().getResource(resourcePath);
            assertThat(resource, notNullValue());
            File file = new File(resource.getFile());
            assertThat(file.exists(), is(true));
            try (InputStream inputStream = new FileInputStream(file)) {
                if (inputStream.available() == 0) {
                    return;
                }
                doWork(inputStream);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        public abstract void doWork(InputStream inputStream);
    }

    protected void checkRule(Rule ruleA, Rule ruleB) {
        assertThat(ruleA, notNullValue());
        assertThat(ruleB, notNullValue());
        checkValue(ruleA.getId(), ruleB.getId());
        checkValue(ruleA.getActivated(), ruleB.getActivated());
        checkValue(ruleA.getPosition(), ruleB.getPosition());
        checkValue(ruleA.getMatch(), ruleB.getMatch());
        checkValue(ruleA.getParameter(), ruleB.getParameter());
        checkValue(ruleA.getActivation(), ruleB.getActivation());
        checkValue(ruleA.getTransform(), ruleB.getTransform());
        checkValue(ruleA.getRemove(), ruleB.getRemove());
        checkValue(ruleA.getCombine(), ruleB.getCombine());
    }

    protected void checkEchoParameter(EchoParameter echoParameterA, EchoParameter echoParameterB) {
        assertThat(echoParameterA, notNullValue());
        assertThat(echoParameterB, notNullValue());
        checkValue(echoParameterA.getId(), echoParameterB.getId());
        checkValue(echoParameterA.getActivated(), echoParameterB.getActivated());
        checkValue(echoParameterA.getParameter(), echoParameterB.getParameter());
    }

    protected <T> void checkValue(T valueA, T valueB) {
        if (valueA == null) {
            assertThat(valueB, nullValue());
        } else {
            assertThat(valueB, notNullValue());
            assertThat(valueA, is(valueB));
        }
    }

    protected EchoParameter findEchoParameter(String id, List<EchoParameter> echoParameters) {
        for (EchoParameter echoParameter : echoParameters) {
            if (echoParameter.getId().equals(id)) {
                return echoParameter;
            }
        }
        return null;
    }

    protected Rule findRule(String id, List<Rule> rules) {
        for (Rule rule : rules) {
            if (rule.getId().equals(id)) {
                return rule;
            }
        }
        return null;
    }
}
