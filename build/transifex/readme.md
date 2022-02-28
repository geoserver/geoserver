# Requirements

The sync commands uses the Transifex CLI in Go, which can be retrieved from GitHub on the Releases page (https://github.com/transifex/cli/releases).

Before use, retrieve an API token on Transifex website after authentication (https://www.transifex.com/user/settings/api/) and put the value in build.properties.

# To update resources in Transifex

Use `ant tx-push`

# To retrieve translations from Transifex

Use `ant tx-pull`

# To add a new source file to Transifex

You can use the `tx add` command but for long paths it is not very handy.

A preferred way is to simply copy/paste an existing block in the .tx/config file. It looks like this:
```
[o:GeoServer:p:geoserver-github-integration:r:src-web-wms-src-main-resources-geoserverapplication-properties--main]
file_filter  = ../../src/web/wms/src/main/resources/GeoServerApplication_<lang>.properties
source_file  = ../../src/web/wms/src/main/resources/GeoServerApplication.properties
type         = PROPERTIES
minimum_perc = 10
```

You have to change the following elements:
- `file_filter` : path to the PROPERTIES language files. Relative to the build/transifex folder. Must contain the tag <lang> where the language ISO code is expected
- `source_filter` : path to the PROPERTIES source file. Relative to the build/transifex folder.
- Change the part after `:r:` in the header. By convention, it is the path to the PROPERTIES source file, with all `/` replaced by '-'. The `geoserverapplication-properties--main` can be kept like this.

After pushing the new resource to Transifex, the administrator should edit the properties to give a better name to the resource and add tags.

# To remove a resource from translation

- Remove the matching block from the config file.
- Do not remove it from Transifex since its content can help to translate other files

# To add a new language

New languages must be configured on the Transifex website, by an administrator of the project.

# Actions when a GeoServer community module move to extension

To keep the translation sync in Transifex, the best things is to edit the configuration file and to only change the elements `file_filter` and `source_file` to match the new source tree.

After pushing the changes to Transifex, ask a Transifex administrator to change the name and the tags of the module in Transifex.