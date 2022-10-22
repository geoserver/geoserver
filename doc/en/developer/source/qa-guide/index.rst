Automatic Quality Assurance checks
==================================

The GeoServer builds on Github Actions and `https://build.geoserver.org/ <https://build.geoserver.org/>`__ apply
`PMD <https://pmd.github.io/>`_ and `Error Prone <https://errorprone.info/>`_ checks on the code base
and will fail the build in case of rule violation.

In case you want to just run the build with the full checks locally, use the following command:

.. code-block:: bash

   mvn clean install -Dqa

Add extra parameters as you see fit, like ``-T1C -nsu`` to speed up the build:

.. code-block:: bash

   mvn install -Prelease -Dqa -T1C -nsu -fae

Flags documented below can be used to shut off individual QA checks when trouble shooting.

PMD checks
----------

The `PMD <https://pmd.github.io/>`__ checks are based on source code analysis for common errors, we have configured :command:`PMD` to check for common mistakes and bad practices such as accidentally including debug ``System.out.println()`` statements in your commit.

.. literalinclude:: /../../../../src/pom.xml
   :language: xml
   :start-at: <artifactId>maven-pmd-plugin</artifactId>
   :end-before: </plugin>
   :dedent: 12

Rules are configured in our build `build/qa/pmd-ruleset.xml <https://github.com/geoserver/geoserver/blob/main/build/qa/pmd-ruleset.xml>`_:

.. literalinclude:: /../../../../build/qa/pmd-ruleset.xml
   :language: xml
   :start-after: </description>
   :end-before: </ruleset>

In order to activate the :command:`PMD` checks, use the ``-Ppmd`` profile:

.. code-block:: bash

   mvn verify -Ppmd

Or run `pmd:check` (requires use of ``initialize`` to locate `geoserverBaseDir/build/qa/pmd-ruleset.xml`):

.. code-block:: bash

   mvn initialize pmd:check -Ppmd

:command:`PMD` will fail the build in case of violation, reporting the specific errors before the build
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

In case of parallel build, the specific error messages will be in the body of the build output, while the XML file reference will be at the end, search for ``PMD Failure`` in the build logs to find the specific code issues.

If you do have a :command:`PMD` failure it is worth checking the pmd website which offers quite clear suggestions:

* `Java Rules <https://pmd.github.io/latest/pmd_rules_java_bestpractices.html>`__

PMD false positive suppression
""""""""""""""""""""""""""""""

Occasionally PMD will report a false positive failure, for those it's possible to annotate the method
or the class in question with a SuppressWarnings using ``PMD.<RuleName``, e.g. if the above error
was actually a legit use of ``System.out.println`` it could have been annotated with:

.. code-block:: java

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
prefix, that will be used to perform the close, for example:

.. code-block:: xml

    <rule ref="category/java/errorprone.xml/CloseResource" >
        <properties>
            <property name="closeTargets" value="releaseConnection,store.releaseConnection,closeQuietly,closeConnection,closeSafe,store.closeSafe,dataStore.closeSafe,getDataStore().closeSafe,close,closeResultSet,closeStmt"/>
        </properties>
    </rule>

For closing delegates that use an instance object instead of a class static method, the variable
name is included in the prefix, so some uninformity in variable names is required.

Error Prone
-----------

The `Error Prone <https://errorprone.info/>`_ checker runs a compiler plugin.

In order to activate the Error Prone checks, use the "-Perrorprone":

.. literalinclude:: /../../../../src/pom.xml
   :language: xml
   :start-at: <id>errorprone</id>
   :end-before: </profile>
   :dedent: 6
      
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

.. literalinclude:: /../../../../src/pom.xml
   :language: xml
   :start-at: <groupId>com.github.spotbugs</groupId>
   :end-before: </plugin>
   :dedent: 12
   
Any failure to comply with the rules will show up as a compile error, e.g.::

   33630 [ERROR] page could be null and is guaranteed to be dereferenced in org.geotools.swing.wizard.JWizard.setCurrentPanel(String) [org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard, org.geotools.swing.wizard.JWizard] Dereferenced at JWizard.java:[line 278]Dereferenced at JWizard.java:[line 269]Null value at JWizard.java:[line 254]Known null at JWizard.java:[line 255] NP_GUARANTEED_DEREF

It is also possible to run the spotbugs:gui goal to have a Swing based issue explorer, e.g.:

.. code-block:: bash

    mvn spotbugs:gui -Pspotbugs -f wms

In case an invalid report is given, an annotation on the class/method/variable can be added to ignore it:

.. code-block:: java

   @SuppressFBWarnings("NP_GUARANTEED_DEREF")

or if it's a general one that should be ignored, the `build/qa/spotbugs-exclude.xml <https://github.com/geoserver/geoserver/blob/main/build/qa/spotbugs-exclude.xml>`__ file can be modified.

.. literalinclude:: /../../../../build/qa/spotbugs-exclude.xml
   :language: xml


Spotless
--------

Spotless is used as a fast way to check that the google-java-format is being applied to the codebase.

.. literalinclude:: /../../../../src/pom.xml
   :language: xml
   :start-at: <groupId>com.diffplug.spotless</groupId>
   :end-before: </plugin>
   :dedent: 8

This has been setup for incremental checking, with hidden :file:`.spotless-index` files used determine
when files were last checked.

To run the plugin directly:

.. code-block:: bash

   mvn spotless:apply

When using ``check`` any failure to comply with the rules will show up as a compiler error in the build output.

.. code-block:: bash

   mvn spotless:check

When verifying ``spotless.action`` is used to choose ``apply`` or ``check`` (defaults to ``apply``):

.. code-block:: bash

   mvn verify -Dqa -Dspotless.action=check

Property ``spotless.apply.skip`` is used to skip spotless plugin when running ``qa`` build:

.. code-block:: bash

   mvn clean install -Dqa -Dspotless.apply.skip=true

Sortpom
-------

Sortpom is used to keep the :file:`pom.xml` files formatting consistent:

.. literalinclude:: /../../../../src/pom.xml
   :language: xml
   :start-at: <groupId>com.github.ekryd.sortpom</groupId>
   :end-before: </plugin>
   :dedent: 8

The plugin is attached to verification phase to sort :file:`pom.xml` files.

To run the plugin directly:

.. code-block:: bash

   mvn sortpom:sort

Verification checks if (ignoring whitespace changes) is the current :file:`pom.xml` in the correct order:

   mvn sortpom:verify

Property ``pom.fmt.action`` is used to choose ``sort`` or ``verify`` (defaults to ``sort``):

.. code-block:: bash

   mvn verify -Dqa -Dpom.fmt.action=verify

Property ``pom.fmt.skip`` used to skip sortpom plugin when running ``qa`` build (defaults to ``spotless.apply.skip`` setting):

.. code-block:: bash

   mvn clean install -Dqa -Dpom.fmt.skip=true

Checkstyle
----------

Spotless is already in use to keep the code formatted, so `maven checkstyle plugin <https://maven.apache.org/plugins/maven-checkstyle-plugin/>`__ is used mainly to verify javadocs errors
and presence of copyright headers, which none of the other tools can cover.

.. literalinclude:: /../../../../src/pom.xml
   :language: xml
   :start-at: <artifactId>maven-checkstyle-plugin</artifactId>
   :end-before: </plugin>
   :dedent: 12

The checkstyle ruleset checks the following:

.. literalinclude:: /../../../../build/qa/checkstyle.xml
   :language: xml
   :start-at: <module name="Checker">

To run the plugin directly:

.. code-block:: bash

   mvn initialize checkstyle:check
