Automatic Quality Assurance checks
----------------------------------

The GeoServer builds on Travis and `https://build.geoserver.org/ <https://build.geoserver.org/>`_ apply
`PMD <https://pmd.github.io/>`_ and `Error Prone <https://errorprone.info/>`_ checks on the code base
and will fail the build in case of rule violation.

In case you want to just run the build with the full checks locally, use the following command::

    mvn clean install -Dqa -Dall

Add extra parameters as you see fit, like ``-T1C -nsu`` to speed up the build, or ``-Dfmt.skip=true -DskipTests``
to avoid running tests and code formatting.

PMD checks
----------

The `PMD <https://pmd.github.io/>`_ checks are based on the basic PMD validation, but limited to priority 2 checks:

https://github.com/geoserver/geoserver/blob/master/src/pmd-ruleset.xml

In order to activate the PMD checks, use the "-Ppmd" profile.

PMD will fail the build in case of violation, reporting the specific errors before the build
error message, and a reference to a XML file with the same information after it (example taken from GeoTools)::

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

In case of parallel build, the specific error messages will be in the body of the build, while the
XML file reference wil be at end end, just search for "PMD Failure" in the build logs to find the specific code issues.

PMD false positive suppression
""""""""""""""""""""""""""""""

Occasionally PMD will report a false positive failure, for those it's possible to annotate the method
or the class in question with a SuppressWarnings using ``PMD.<RuleName``, e.g. if the above error
was actually a legit use of ``System.out.println`` it could have been annotated with::

    @SuppressWarnings("PMD.SystemPrintln")
    public void methodDoingPrintln(...) {

PMD CloseResource checks
""""""""""""""""""""""""

PMD can check for Closeable that are not getting property closed by the code, and report about it.
PMD by default only checks for SQL related closeables, like "Connection,ResultSet,Statement", but it
can be instructed to check for more by configuration (do check the PMD configuration in 
``build/qa/pmd-ruleset.xml``.

The check is a bit fragile, in that there are multiple ways to close an object between direct calls,
utilities and delegate methods. The configuration lists the type of methods, and the eventual
prefix, that will be used to perform the close, for example::

    <rule ref="category/java/errorprone.xml/CloseResource" >
        <properties>
            <property name="closeTargets" value="releaseConnection,store.releaseConnection,closeQuietly,closeConnection,closeSafe,store.closeSafe,dataStore.closeSafe,getDataStore().closeSafe,close,closeResultSet,closeStmt"/>
        </properties>
    </rule>

For closing delegates that use an instance object instead of a class static method, the variable
name is included in the prefix, so some uninformity in variable names is required.

Error Prone
-----------

The `Error Prone <https://errorprone.info/>`_ checker runs a compiler plugin, requiring at least a 
JDK 9 to run (hence the suggestion to use JDK 11, as the supported JDKs are currently only JDK 8 and JDK 11).
Mind, running the profile with a JDK8 will result in a generic compile error!

In order to activate the Error Prone checks, use the "-Perrorprone" for JDK 11 builds, or "Perrorprone8" for JDK 8 builds.

Any failure to comply with the "Error Prone" rules will show up as a compile error in the build output, e.g. (example taken from GeoTools)::

        9476 [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project gt-coverage: Compilation failure
        9476 [ERROR] /home/user/devel/git-gt/modules/library/coverage/src/main/java/org/geotools/image/ImageWorker.java:[380,39] error: [IdentityBinaryExpression] A binary expression where both operands are the same is usually incorrect; the value of this expression is equivalent to `255`.
        9477 [ERROR]     (see https://errorprone.info/bugpattern/IdentityBinaryExpression)
        9477 [ERROR] 
        9477 [ERROR] -> [Help 1]
        org.apache.maven.lifecycle.LifecycleExecutionException: Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.8.0:compile (default-compile) on project gt-coverage: Compilation failure
        /home/user/devel/git-gt/modules/library/coverage/src/main/java/org/geotools/image/ImageWorker.java:[380,39] error: [IdentityBinaryExpression] A binary expression where both operands are the same is usually incorrect; the value of this expression is equivalent to `255`.
        (see https://errorprone.info/bugpattern/IdentityBinaryExpression)

In case Error Prone is reporting an invalid error, the method or class in question can be annotated
with SuppressWarnings with the name of the rule, e.g., to get rid of the above the following annotation could be used::

   @SuppressWarnings("IdentityBinaryExpression")

Spotbugs
--------

The `Spotbugs <https://spotbugs.github.io/>`_ checker runs as a post-compile bytecode analyzer.

Any failure to comply with the rules will show up as a compile error, e.g.::

        33630 [ERROR] page could be null and is guaranteed to be dereferenced in org.geotools.swing.wizard.JWizard.setCurrentPanel(String) [org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard] Dereferenced at JWizard.java:[line 278]Dereferenced at JWizard.java:[line 269]Null value at JWizard.java:[line 254]Known null at JWizard.java:[line 255] NP_GUARANTEED_DEREF

It is also possible to run the spotbugs:gui goal to have a Swing based issue explorer, e.g.::

    mvn spotbugs:gui -Pspotbugs -f wms

In case an invalid report is given, an annotation on the class/method/variable can be added to ignore it:

   @SuppressFBWarnings("NP_GUARANTEED_DEREF")

or if it's a general one that should be ignored, the ``${geoserverBaseDir}/build/qa/spotbugs-exclude.xml`` file can be modified.

Checkstyle
----------

Google Format is already in use to keep the code formatted, so Checkstyle is used mainly to verify javadocs errors
and presence of copyright headers, which none of the other tools can cover.

Any failure to comply with the rules will show up as a compiler error in the build output, e.g.::

        14610 [INFO] --- maven-checkstyle-plugin:3.0.0:check (default) @ gt-jdbc ---
        15563 [INFO] There is 1 error reported by Checkstyle 6.18 with /home/aaime/devel/git-gs/build/qa/checkstyle.xml ruleset.
        15572 [ERROR] wms/main/java/org/geoserver/wms/map/RenderedImageMapOutputFormat.java:[325,8] (javadoc) JavadocMethod: Unused @param tag for 'foobar'.

