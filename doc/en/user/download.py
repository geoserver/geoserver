import glob
import logging
import mkdocs.plugins
import os
import shutil

log = logging.getLogger('mkdocs')

@mkdocs.plugins.event_priority(-50)
def on_pre_build( config, **kwargs ):
    docs_dir = config['docs_dir']
    # print(docs_dir)
    
    pattern = os.path.normpath( os.path.join(docs_dir,'**','download','download.txt') )
    
    for download_txt in glob.glob(pattern, recursive=True):
        download_folder = os.path.dirname(download_txt)
        with open(download_txt, 'r') as file:
            path_list = file.read()
        
        for path in path_list.splitlines():
            resolved = os.path.normpath(os.path.join(download_folder,path))
            if os.path.exists(resolved):
                dest = os.path.normpath(os.path.join(download_folder,os.path.basename(path)))
                if not os.path.exists(dest) or (os.stat(resolved).st_mtime - os.stat(dest).st_mtime > 1):
                    print(f"Download {dest}")
                    shutil.copyfile(resolved, dest, follow_symlinks=True)
            else:
                log.warning(f"Download '{resolved}' not found")
