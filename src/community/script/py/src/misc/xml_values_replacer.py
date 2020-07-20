# (c) 2020 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root application directory.
# Written by Idan Miara <idan@miara.com>
'''
This script replaces the tile size (or other parameter) on all the layers of a selected workspace
usage: set data_dir path and workspace name at the bottom line, run.the script and restart geoserver
'''

import os
from pathlib import Path
import re

def change_pattern_in_file(filename, key, pattern, new_value, make_backup=True):
    # I could use proper read/write xml, but it seems like an overkill for this task
    print(filename)
    filename = Path(filename).resolve()
    if not filename.exists():
        raise Exception('filename {} does not exist'.format(filename))
    lines = open(str(filename), "r").readlines()
    line_i = None
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped == key:
            line_i = i+1
            break
    if line_i:
        old_line = lines[line_i]
        new_line = pattern.sub(new_value, old_line)
        if old_line == new_line:
            print('pattern was not replaced!')
            return False
        lines[line_i] = new_line
    else:
        print('key not found')
        return False
    if make_backup:
        backup_name = filename.with_suffix('.bak')
        if backup_name.exists():
            print('backup file exists {}'.format(backup_name))
        else:
            os.rename(str(filename), str(backup_name))
    open(str(filename), "w").writelines(lines)
    return True


def change_pattern_glob(root: Path, file_pattern: str, **kwargs):
    result = 0
    total = 0
    for filename in root.glob(file_pattern):
        total += 1
        if change_pattern_in_file(filename=filename, **kwargs):
            result += 1
    return total, result


# <coverage>
# ...
#   <parameters>
# ...
#     <entry>
#       <string>SUGGESTED_TILE_SIZE</string>
#       <string>512,512</string>
#     </entry>
# ...
#   </parameters>
# ...
# </coverage>


def geoserver_tilesize_change(data_dir, workspace):
    key = '<string>SUGGESTED_TILE_SIZE</string>'
    pattern = r'<string>{}</string>'
    p = re.compile(pattern.format('.*'))
    # new_value = r'<string>256,256</string>'
    new_value = pattern.format('256,256')
    root = Path(data_dir) / 'workspaces' / workspace
    file_pattern = '**/**/coverage.xml'
    make_backup = True

    total, success = change_pattern_glob(
        root=root, file_pattern=file_pattern,
        key=key, pattern=p, new_value=new_value, make_backup=make_backup)
    print('files processed {}/{}'.format(success, total))


if __name__ == '__main__':
    geoserver_tilesize_change(data_dir=r'c:\geoserver\data_dir', workspace='my_workspace')
