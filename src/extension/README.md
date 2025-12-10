# GeoServer Extensions

This directory contains GeoServer extension modules that provide additional functionality beyond the core application.

## Building Extensions

Extensions use a per-module assembly approach, where each extension has its own assembly descriptor that defines how it should be packaged for release.

### Build Process

To build all extension assemblies:

```bash
cd src
# Step 1: Build and install all modules (core + extensions) to local Maven repo
mvn clean install -Prelease -DskipTests

# Step 2: Package extensions into ZIP files (NO clean)
cd extension
mvn install -Prelease,assembly -DskipTests
```

**Important**:
- Step 1 compiles and installs all modules to `~/.m2/repository`
- Step 2 uses those installed artifacts to copy dependencies and create ZIPs
- `-Prelease` activates all extension modules
- `-Passembly` runs the assembly packaging process
- Do NOT use `clean` in step 2 - it would wipe out the compiled classes

Extension ZIP files are created in `src/target/release/`.

### Assembly Configuration

The assembly profile is defined in `src/extension/pom.xml` and includes:

1. **maven-antrun-plugin**: Checks if `src/assembly/assembly.xml` exists in each module
2. **maven-dependency-plugin**: Copies module dependencies to `target/dependency/`
3. **maven-assembly-plugin**: Creates the ZIP package using the assembly descriptor

The assembly only runs if a module has an `src/assembly/assembly.xml` file.

## Extension Types

### Single-Module Extensions

Most extensions are single modules with their assembly descriptor at:
```
src/extension/<module-name>/src/assembly/assembly.xml
```

Example: `app-schema`, `charts`, `control-flow`, etc.

The assembly descriptor uses `<directory>target/dependency</directory>` to include JARs.

### Multi-Module Extensions

Some extensions consist of multiple sub-modules and have a dedicated `-assembly` module:

- **csw**: `csw-assembly` and `csw-iso-assembly`
- **importer**: `importer-assembly`
- **ogcapi**: `ogcapi-features-assembly`
- **wps**: `wps-assembly`
- **dxf**: `dxf-assembly`
- etc.

The assembly module has:
```
<packaging>jar</packaging>
<dependencies>
  <!-- Dependencies on sibling modules -->
</dependencies>
```

And an assembly descriptor that includes JARs from all sub-modules.

### Pseudo-Modules

Some extensions are wrappers around GeoTools libraries with no GeoServer-specific code:

- **gdal**: Packages `gt-imagemosaic-gdal` from GeoTools
- **iau**: Packages `gt-iau-wkt` (planetary CRS support)
- **pyramid**: Packages `gt-imagepyramid` from GeoTools

These modules only have:
- `pom.xml` with dependencies on GeoTools libraries
- `src/assembly/assembly.xml` to package them

## Assembly Descriptor Structure

A typical assembly descriptor includes:

```xml
<assembly>
  <id>module-name-plugin</id>
  <formats>
    <format>zip</format>
  </formats>
  <includeBaseDirectory>false</includeBaseDirectory>
  <fileSets>
    <fileSet>
      <directory>target/dependency</directory>
      <outputDirectory></outputDirectory>
      <includes>
        <include>gs-module-*.jar</include>
        <include>dependency-*.jar</include>
      </includes>
    </fileSet>
    <fileSet>
      <directory>${geoserverBaseDir}/release/target/html/licenses</directory>
      <outputDirectory>licenses</outputDirectory>
      <includes>
        <include>*.html</include>
      </includes>
    </fileSet>
  </fileSets>
  <files>
    <file>
      <source>${geoserverBaseDir}/release/extensions/README.md</source>
      <outputDirectory></outputDirectory>
      <destName>README.txt</destName>
    </file>
    <file>
      <source>${geoserverBaseDir}/release/target/html/extensions/LICENSE.html</source>
      <outputDirectory></outputDirectory>
      <destName>LICENSE.html</destName>
    </file>
  </files>
</assembly>
```

### Path Variables

- `${geoserverBaseDir}`: Resolves to `src/` directory (set by directory-maven-plugin)
- Use `${geoserverBaseDir}/release/` for shared resources
- Use `target/dependency` for module-specific JARs

## Profiles

Extensions are activated through Maven profiles in `src/extension/pom.xml`:

- Individual profiles for each extension (e.g., `-Pwps`, `-Pcsw`)
- `allExtensions` profile: Builds all extensions
- `release` profile: Builds all extensions for release
- `assembly` profile: Creates ZIP packages from built modules

## Future Improvement: Migration to dependencySets

**Current Approach**: Most extensions use `<fileSets>` with manual JAR patterns:
- maven-dependency-plugin copies all dependencies to `target/dependency/` (~6.8GB for all extensions)
- Assembly descriptors reference JARs by name patterns from `target/dependency/`
- Requires maintaining JAR name patterns in each assembly.xml

**Proposed Approach**: Use `<dependencySets>` (correctly implemented in `src/community/gwc-gcs-blob`):

```xml
<assembly>
  <id>module-name-plugin</id>
  <formats>
    <format>zip</format>
  </formats>
  <includeBaseDirectory>false</includeBaseDirectory>
  <dependencySets>
    <dependencySet>
      <outputDirectory>/</outputDirectory>
      <useProjectArtifact>true</useProjectArtifact>
      <scope>runtime</scope>
      <useTransitiveDependencies>true</useTransitiveDependencies>
      <useTransitiveFiltering>false</useTransitiveFiltering>
    </dependencySet>
  </dependencySets>
</assembly>
```

**Benefits**:
- **Disk space savings**: Eliminates 6.8GB of copied dependencies (no maven-dependency-plugin needed)
- **Build performance**: Faster builds without dependency copying step
- **Simpler descriptors**: ~20 lines vs ~40-50 lines per assembly.xml
- **Auto-correct**: Dependencies resolved directly from pom.xml, no manual JAR patterns
- **Easier maintenance**: Dependency changes automatically reflected in assemblies

**Requirements**:
- Dependency scopes must be correct in pom.xml files
- Use `<scope>provided</scope>` for dependencies that shouldn't be packaged (e.g., servlet-api, gs-main)
- Use `compile` or `runtime` scope for dependencies to include

**To Truly Save Disk Space**:

Currently, maven-dependency-plugin still copies all dependencies to `target/dependency/` even for modules using `<dependencySets>`. To skip this wasteful step, add this property to the module's pom.xml:

```xml
<properties>
  <assembly.useDependencySets>true</assembly.useDependencySets>
</properties>
```

Then update the assembly profile in `src/extension/pom.xml` and `src/community/pom.xml`:

```xml
<plugin>
  <artifactId>maven-dependency-plugin</artifactId>
  <executions>
    <execution>
      <phase>install</phase>
      <goals>
        <goal>copy-dependencies</goal>
      </goals>
      <configuration>
        <skip>${assembly.useDependencySets}</skip> <!-- Skip if using dependencySets -->
      </configuration>
    </execution>
  </executions>
</plugin>
```

This change is needed to realize the full 6.8GB disk space savings and build performance improvements.

**Migration Strategy**:
1. Start with new extensions - use dependencySets from the beginning
2. Gradually migrate existing extensions one at a time
3. Verify each migration produces identical ZIP contents
4. Once all migrated, remove maven-dependency-plugin from assembly profile

**Examples**:
- ✅ **Correct usage**: `src/community/gwc-gcs-blob/src/assembly/assembly.xml` - Uses only dependencySets, no fileSets needed
- ⚠️ **Incorrect usage**: `src/community/pmtiles-store/src/assembly/assembly.xml` - Uses dependencySets but also has redundant fileSets that can be removed

## Extension Documentation

Extension-specific documentation is kept in `src/release/extensions/` (not in the extension module itself) for the following reasons:

1. **Centralized markdown processing**: The `src/release/pom.xml` uses `markdown-page-generator-plugin` to convert all markdown files from `src/release/extensions/` into HTML
2. **HTML generation**: Assembly descriptors reference both the markdown (as README.txt) and generated HTML (as README.html and LICENSE.html)
3. **Shared location**: Makes it easy for the release module to process all extension documentation in one pass

### Documentation Conventions

- **Generic extensions**: Most extensions use only the shared `src/release/extensions/README.md` file
- **Extension-specific docs**: Extensions with custom setup or configuration needs have dedicated directories:
  - `src/release/extensions/<extension-name>/<extension-name>-README.md` - Installation and usage guide
  - `src/release/extensions/<extension-name>/<extension-name>-LICENSE.md` - Special licensing (if applicable)

### Extensions with Custom Documentation

The following extensions have dedicated documentation directories:
- `app-schema`, `cas`, `db2`, `dxf`, `excel`, `gdal`, `h2`, `iau`, `jp2k`, `mapml`, `mysql`, `ogr`, `oracle`, `printing`, `pyramid`, `vectortiles`

### Referencing Documentation in assembly.xml

**For extensions with specific documentation:**
```xml
<file>
  <source>${geoserverBaseDir}/release/extensions/my-extension/my-extension-README.md</source>
  <outputDirectory></outputDirectory>
  <destName>README.txt</destName>
</file>
<file>
  <source>${geoserverBaseDir}/release/target/html/extensions/my-extension/my-extension-README.html</source>
  <outputDirectory></outputDirectory>
  <destName>README.html</destName>
</file>
```

**For extensions using generic README:**
```xml
<file>
  <source>${geoserverBaseDir}/release/extensions/README.md</source>
  <outputDirectory></outputDirectory>
  <destName>README.txt</destName>
</file>
<file>
  <source>${geoserverBaseDir}/release/target/html/extensions/LICENSE.html</source>
  <outputDirectory></outputDirectory>
  <destName>LICENSE.html</destName>
</file>
```

## Adding a New Extension

To add a new extension with assembly support:

1. Create the extension module with standard Maven structure
2. Add `src/assembly/assembly.xml` descriptor (for single-module) or create an `-assembly` sub-module (for multi-module)
3. Add module to appropriate profile in `src/extension/pom.xml`
4. *(Optional)* Add custom documentation in `src/release/extensions/<module-name>/` if the extension requires special setup instructions

Example single-module structure:
```
src/extension/my-extension/
├── pom.xml
├── src/
│   ├── main/java/...
│   ├── test/java/...
│   └── assembly/
│       └── assembly.xml
```

Example multi-module structure:
```
src/extension/my-extension/
├── pom.xml (parent)
├── my-extension-core/
│   ├── pom.xml
│   └── src/main/java/...
├── my-extension-web/
│   ├── pom.xml
│   └── src/main/java/...
└── my-extension-assembly/
    ├── pom.xml (with dependencies on siblings)
    └── src/assembly/
        └── assembly.xml
```
