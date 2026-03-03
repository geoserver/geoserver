---
render_macros: true
---

# Automatic Quality Assurance checks

The GeoServer builds on Github Actions and <https://build.geoserver.org/> apply [PMD](https://pmd.github.io/) and [Error Prone](https://errorprone.info/) checks on the code base and will fail the build in case of rule violation.

In case you want to just run the build with the full checks locally, use the following command:

``` bash
mvn clean install -Dqa
```

Add extra parameters as you see fit, like `-T1C -nsu` to speed up the build:

``` bash
mvn install -Prelease -Dqa -T1C -nsu -fae
```

Flags documented below can be used to shut off individual QA checks when trouble shooting.

## PMD checks

The [PMD](https://pmd.github.io/) checks are based on source code analysis for common errors, we have configured ***PMD*** to check for common mistakes and bad practices such as accidentally including debug `System.out.println()` statements in your commit.

The plugin version is managed in the pluginManagement section.

The actual plugin configuration and execution is defined as:

~~~xml
{%raw%}{%endraw%}{% include "../../../../src/pom.xml" start="<!-- doc-include-pmd-plugin-start" %}
~~~

Rules are configured in our build [build/qa/pmd-ruleset.xml](https://github.com/geoserver/geoserver/blob/main/build/qa/pmd-ruleset.xml):

~~~xml
{%raw%}{% include "../../../../build/qa/pmd-ruleset.xml" start="</description>" end="</ruleset>" %}
~~~

In order to activate the ***PMD*** checks, use the `-Ppmd` profile:

``` bash
mvn verify -Ppmd
```

Or run ``pmd:check`` (requires use of `initialize` to locate ``geoserverBaseDir/build/qa/pmd-ruleset.xml``):

``` bash
mvn initialize pmd:check -Ppmd
```

***PMD*** will fail the build in case of violation, reporting the specific errors before the build error message, and a reference to a XML file with the same information after it (example taken from GeoTools):

    7322 [INFO] --- maven-pmd-plugin:3.11.0:check (default) @ gt-main ---
    17336 [INFO] PMD Failure: org.geotools.data.DataStoreAdaptor:98 Rule:SystemPrintln Priority:2 System.out.println is used.
    17336 [INFO] PMD Failure: org.geotools.data.DataStoreAdaptor:98 Rule:SystemPrintln Priority:2 System.out.println is used.
    17337 [INFO] ------------------------------------------------------------------------
    17337 [INFO] BUILD FAILURE
    17337 [INFO] ------------------------------------------------------------------------
    17338 [INFO] Total time:  16.727 s
    17338 [INFO] Finished at: 2018-12-29T11:34:33+01:00
    17338 [INFO] ------------------------------------------------------------------------
    17340 [ERROR] Failed to execute goal org.apache.maven.plugins:maven-pmd-plugin:3.11.0:check (default) on project gt-main: You have 1 PMD violation. For more details see:       /home/yourUser/devel/git-gt/modules/library/main/target/pmd.xml -> [Help 1]
    17340 [ERROR] 

In case of parallel build, the specific error messages will be in the body of the build output, while the XML file reference will be at the end, search for `PMD Failure` in the build logs to find the specific code issues.

If you do have a ***PMD*** failure it is worth checking the pmd website which offers quite clear suggestions:

- [Java Rules](https://pmd.github.io/latest/pmd_rules_java_bestpractices.html)

### PMD false positive suppression

Occasionally PMD will report a false positive failure, for those it's possible to annotate the method or the class in question with a SuppressWarnings using `PMD.<RuleName`, e.g. if the above error was actually a legit use of `System.out.println` it could have been annotated with:

``` java
@SuppressWarnings("PMD.SystemPrintln")
public void methodDoingPrintln(...) {
```

### PMD CloseResource checks

PMD can check for Closeable that are not getting property closed by the code, and report about it. PMD by default only checks for SQL related closeables, like "Connection,ResultSet,Statement", but it can be instructed to check for more by configuration (do check the PMD configuration in `build/qa/pmd-ruleset.xml`.

The check is a bit fragile, in that there are multiple ways to close an object between direct calls, utilities and delegate methods. The configuration lists the type of methods, and the eventual prefix, that will be used to perform the close, for example:

``` xml
<rule ref="category/java/errorprone.xml/CloseResource" >
    <properties>
        <property name="closeTargets" value="releaseConnection,store.releaseConnection,closeQuietly,closeConnection,closeSafe,store.closeSafe,dataStore.closeSafe,getDataStore().closeSafe,close,closeResultSet,closeStmt"/>
    </properties>
</rule>
```

For closing delegates that use an instance object instead of a class static method, the variable name is included in the prefix, so some uninformity in variable names is required.

## Error Prone

The [Error Prone](https://errorprone.info/) checker runs a compiler plugin.

In order to activate the Error Prone checks, use the "-Perrorprone":

~~~xml
{%raw%}{%endraw%}{% include "../../../../src/pom.xml" end="</profile>" %}
~~~

Any failure to comply with the "Error Prone" rules will show up as a compile error in the build output, e.g. (example taken from GeoTools):

    9476 [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project gt-coverage: Compilation failure
    9476 [ERROR] /home/user/devel/git-gt/modules/library/coverage/src/main/java/org/geotools/image/ImageWorker.java:[380,39] error: [IdentityBinaryExpression] A binary expression where both operands are the same is usually incorrect; the value of this expression is equivalent to `255`.
    9477 [ERROR]     (see https://errorprone.info/bugpattern/IdentityBinaryExpression)
    9477 [ERROR] 
    9477 [ERROR] -> [Help 1]
    org.apache.maven.lifecycle.LifecycleExecutionException: Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project gt-coverage: Compilation failure
    /home/user/devel/git-gt/modules/library/coverage/src/main/java/org/geotools/image/ImageWorker.java:[380,39] error: [IdentityBinaryExpression] A binary expression where both operands are the same is usually incorrect; the value of this expression is equivalent to `255`.
    (see https://errorprone.info/bugpattern/IdentityBinaryExpression)

In case Error Prone is reporting an invalid error, the method or class in question can be annotated with SuppressWarnings with the name of the rule, e.g., to get rid of the above the following annotation could be used:

    @SuppressWarnings("IdentityBinaryExpression")

## Spotbugs

The [Spotbugs](https://spotbugs.github.io/) checker runs as a post-compile bytecode analyzer.

The plugin version is managed in the pluginManagement section.

The actual plugin configuration and execution is defined as:

~~~xml
{%raw%}{% include "../../../../src/pom.xml" start="<!-- doc-include-spotbugs-plugin-start" %}
~~~

Any failure to comply with the rules will show up as a compile error, e.g.:

    33630 [ERROR] page could be null and is guaranteed to be dereferenced in org.geotools.swing.wizard.JWizard.setCurrentPanel(String) [org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard] Dereferenced at JWizard.java:[line 278]Dereferenced at JWizard.java:[line 269]Null value at JWizard.java:[line 254]Known null at JWizard.java:[line 255] NP_GUARANTEED_DEREF

It is also possible to run the spotbugs:gui goal to have a Swing based issue explorer, e.g.:

``` bash
mvn spotbugs:gui -Pspotbugs -f wms
```

In case an invalid report is given, an annotation on the class/method/variable can be added to ignore it:

``` java
@SuppressFBWarnings("NP_GUARANTEED_DEREF")
```

or if it's a general one that should be ignored, the [build/qa/spotbugs-exclude.xml](https://github.com/geoserver/geoserver/blob/main/build/qa/spotbugs-exclude.xml) file can be modified.

~~~xml
{%raw%}{%endraw%}<!-- Include path goes outside docs directory: ../../../../build/qa/spotbugs-exclude.xml -->
<!-- TODO: Copy file to docs directory or use alternative approach -->
~~~

## Spotless

Spotless is used as a fast way to check that the [palantir-java -format](https://github.com/palantir/palantir-java-format?tab=readme-ov-file#palantir-java-format) is being applied to the codebase.

The plugin version is managed in the pluginManagement section.

The actual plugin configuration and execution is defined as:

~~~xml
{%raw%}{% include "../../../../src/pom.xml" start="<!-- doc-include-spotless-plugin-start" %}
~~~

This has been setup for incremental checking, with hidden **`.spotless-index`** files used determine when files were last checked.

To run the plugin directly:

``` bash
mvn spotless:apply
```

When using `check` any failure to comply with the rules will show up as a compiler error in the build output.

``` bash
mvn spotless:check
```

When verifying `spotless.action` is used to choose `apply` or `check` (defaults to `apply`):

``` bash
mvn verify -Dqa -Dspotless.action=check
```

Property `spotless.apply.skip` is used to skip spotless plugin when running `qa` build:

``` bash
mvn clean install -Dqa -Dspotless.apply.skip=true
```

!!! note

    IDE Plugins are available for [IntelliJ](https://plugins.jetbrains.com/plugin/13180-palantir-java-format) and [Eclipse](https://github.com/palantir/palantir-java-format/tree/develop/eclipse_plugin) IDEs.

## Sortpom

Sortpom is used to keep the **`pom.xml`** files formatting consistent:

The plugin version is managed in the pluginManagement section.

The actual plugin configuration and execution is defined as:

~~~xml
{%raw%}{%endraw%}{% include "../../../../src/pom.xml" start="<!-- doc-include-sortpom-plugin-start" %}
~~~

The plugin is attached to verification phase to sort **`pom.xml`** files.

To run the plugin directly:

``` bash
mvn sortpom:sort
```

Verification checks if (ignoring whitespace changes) is the current **`pom.xml`** in the correct order:

> mvn sortpom:verify

Property `pom.fmt.action` is used to choose `sort` or `verify` (defaults to `sort`):

``` bash
mvn verify -Dqa -Dpom.fmt.action=verify
```

Property `pom.fmt.skip` used to skip sortpom plugin when running `qa` build (defaults to `spotless.apply.skip` setting):

``` bash
mvn clean install -Dqa -Dpom.fmt.skip=true
```

## Checkstyle

Spotless is already in use to keep the code formatted, so [maven checkstyle plugin](https://maven.apache.org/plugins/maven-checkstyle-plugin/) is used mainly to verify javadocs errors and presence of copyright headers, which none of the other tools can cover.

The plugin version is managed in the pluginManagement section.

The actual plugin configuration and execution is defined as:

~~~xml
{%raw%}{%endraw%}{% include "../../../../src/pom.xml" start="<!-- doc-include-checkstyle-plugin-start" %}
~~~

The checkstyle ruleset checks the following:

~~~xml
{%raw%}<!-- Include path goes outside docs directory: ../../../../build/qa/checkstyle.xml -->
<!-- TODO: Copy file to docs directory or use alternative approach -->
~~~

To run the plugin directly:

``` bash
mvn initialize checkstyle:check
```
