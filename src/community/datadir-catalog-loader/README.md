# Data-Directory GeoServer Loader

Provides an alternative [GeoServerLoader](https://github.com/geoserver/geoserver/blob/main/src/main/src/main/java/org/geoserver/config/GeoServerLoader.java), [DataDirectoryGeoServerLoader](src/main/java/org/geoserver/catalog/datadir/DataDirectoryGeoServerLoader.java), that attempts to load and parse "data directories"
faster than the default one, especially over shared drives such as NFS.

The approach is to

- Parallelize both I/O calls and parsing of Catalog and Config info objects
- "Serialize" I/O calls as much as possible, trying to make a single pass over the `workspaces` directory tree, and load both catalog (workspaces, layers, etc.) and config (services, settings, etc.) files in one pass.

The point is that large Catalogs contain several thousand small XML files that need to be read and parsed, and NFS in particular is really bad at serving small files.

## Configuration

Even if the plugin is in the JVM classpath, it can be disabled through the `datadir.loader.enabled=false` System Property, or the `DATADIR_LOADER_ENABLED` environment variable.

```
export DATADIR_LOADER_ENABLED=false
bin/startup.sh
```

### Parallelism level

The number of threads to use for loading and parsing configuration files is determined by an heuristic resolving to the minimum between `16` and the number of available processors as reported by `Runtime#availableProcessors()`.

This can be overridden by an environment variable or system property  called `DATADIR_LOAD_PARALLELISM`.

```
export DATADIR_LOAD_PARALLELISM=4
bin/startup.sh
```

 