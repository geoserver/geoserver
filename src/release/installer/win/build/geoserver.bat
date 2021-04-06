pushd %~dp0\..
call wrapper\setenv.bat
%wrapper_bat% %1 %conf_file%
popd
