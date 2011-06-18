This component is EditArea, which you can download from http://sourceforge.net/projects/editarea/.

The current version of the component is 0.7.3.

When you upgrade EditArea to a newer version rember to:
- update this file
- rename images/go_to_line.gif into image/goto_line.gif
- change edit_area_full.js and edit_area_full_with_plugins.js accordingly

The change is due to a conflict with Wicket locale enabled resource resolution, Wicket is 
picking up "to" as a locale definition. 