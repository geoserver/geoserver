#
# (c) 2024 Open Source Geospatial Foundation
#
# MIT License
#
# Macros defined here are intended for use with mkdocs_translate and reproduce
# some sphinx-build directives.
# 
# Reference:
#
# * https://mkdocs-macros-plugin.readthedocs.io/ for background on what goes on here.
#

def define_env(env):
    """
    This is the hook for defining variables, macros and filters
        
    VARIABLES: mkdocs.yml extas section:
    
    - version: GeoServer Version 2.25
    - release: GeoServer Version 2.25.0    
    """
    
    MAIN_BRANCH: str = "2.25"
    STABLE_BRANCH: str = "2.24"
    MAINTENANCE_BRANCH: str = "2.23"
    
    @env.macro
    def download_release(distribution: str, download:str='' ) -> str:
        """
        Link to download distribution (war, bin, exe, zip):
        
            {{ download_release('war'} }}
            {{ download_release('war','snapshot'}}
        
        :param distribution: Distribution artifact ('war', 'bin', 'zip', 'exe')
        :param download: Use value of 'snapshot' to link to nightly build.
        :return: Markdown link to distribution download
        """
        
        release = env.variables['release']
        version = env.variables['version']
        snapshot = download and download.lower() == 'snapshot'
        
        if snapshot:
            if release.startswith(MAIN_BRANCH):
                branch = "main"
            else:
                branch = version + '.x'
            
            file = f"geoserver-{version}.x-latest-{distribution}.zip"
            link = f"https://build.geoserver.org/geoserver/{branch}/{file}"
        else:
            file = f"geoserver-{release}-{distribution}.zip"
            link = f"https://sourceforge.net/projects/geoserver/files/GeoServer/{release}/{file}"
        
        return f"[{file}]({link})"
        
    @env.macro
    def download_extension(extension: str, download:str='' ) -> str:
        """
        Link to download extension (wps, css, ...):
        
            {{ download_extension('wps'} }}
            {{ download_extension('css','snapshot'}}
            
            
        :param extension: Extension ('wps', 'css', 'printing', ...)
        :param download: Use value of 'snapshot' to link to nightly build.
        :return: Markdown link to extension download
        """
        release = env.variables['release']
        version = env.variables['version']
        snapshot = download and download.lower() == 'snapshot'
        
        if snapshot:
            if release.startswith(MAIN_BRANCH):
                branch = "main"
            else:
                branch = version + '.x'
            
            file = f"geoserver-{version}-SNAPSHOT-{extension}-plugin.zip"
            link = f"https://build.geoserver.org/geoserver/{branch}/ext-latest/{file}"
        else:
            file = f"geoserver-{release}-{extension}-plugin.zip"
            link = f"https://sourceforge.net/projects/geoserver/files/GeoServer/{release}/extensions/{file}"
            
        return f"[{file}]({link})"
        
    @env.macro
    def download_community(module: str, download:str='' ) -> str:
        """
        Link to download community module(, css, ...):
        
            {{ download_community('cog'} }}
            {{ download_community('sec-oauth2-openid-connect','snapshot'}}
            
            
        :param module: Community module ('cog', ...)
        :param download: Use value of 'snapshot' to link to nightly build.
        :return: Markdown link to community module
        """
        release = env.variables['release']
        version = env.variables['version']
        snapshot = download and download.lower() == 'snapshot'
        
        if snapshot:
            if release.startswith(MAIN_BRANCH):
                branch = "main"
            else:
                branch = version + '.x'
            
            file = f"geoserver-{version}-SNAPSHOT-{module}-plugin.zip"
            link = f"https://build.geoserver.org/geoserver/{branch}/community-latest/{file}"
            
            return f"[{file}]({link})"
        else:
            folder = module
            if module.startswith("sec-"):
                module = "security/"+module[5:]
            if module.startswith("ogciapi-"):
                module = "ogcapi/"+module
            
            return f"[compile {module} module](https://github.com/geoserver/geoserver/tree/{release}/src/community/{module})"
