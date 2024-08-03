# Community modules

The root of all the community modules.

## Assembly definitions

Rules to follow in order to create a zip for a module:

* Add an ``assembly.xml`` descriptor to the module
* Add the ``<skipAssembly>false<skipAssembly>`` property to the module (this activates assembly generation)
* If assembly needs to pick files from multiple independent modules, create a dedicated assembly module for it (e.g., check ``backup-restore/assembly`` that depends on the others)
* The assembly process will automatically copy the dependencies into ``target/dependency``, depending on the nature of the module you might have to add the generated module jar found in ``target`` too (see existing ``assembly.xml`` files for reference).

## Assembly execution

To assemble the zip files of extensions, run the following:

```
mvn clean install -DskipTests -PcommunityRelease,assembly -T1C -nsu -fae
```

If the assembly of a particular module, or group of modules, is desired, then use the associated profile, e.g.:

```
mvn clean install -DskipTests -PcolorMap,assembly -T1C -nsu -fae
```

It is important to run at least one maven phase (e.g. ``install``) for the assembly machinery to work, e.g., calling a goal directly will not work:

```
mvn assembly:single -DskipTests -PcolorMap,assembly -T1C -nsu -fae
```

