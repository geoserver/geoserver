Automatic Quality Assurance checks
----------------------------------

The GeoServer builds on Travis and `https://build.geoserver.org/ <https://build.geoserver.org/>`_ apply
`PMD <https://pmd.github.io/>`_ and `Error Prone <https://errorprone.info/>`_ checks on the code base
and will fail the build in case of rule violation.

In case you want to just run the build with the full checks locally, use the following command
if you are using a JDK 8::

    mvn clean install -Ppmd,errorprone8 -Dall

or the following if using JDK 11::

    mvn clean install -Ppmd,errorprone -Dall

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

Occasionally PMD will report a false positive failure, for those it's possible to annotate the method
or the class in question with a SuppressWarnings using ``PMD.<RuleName``, e.g. if the above error
was actually a legit use of ``System.out.println`` it could have been annotated with::

    @SuppressWarnings("PMD.SystemPrintln")
    public void methodDoingPrintln(...) {

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