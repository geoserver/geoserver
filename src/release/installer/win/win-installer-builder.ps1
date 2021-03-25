# (c) 2020 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root application directory.
# Written by Idan Miara <idan@miara.com>
 
# you may change the version numbers here...
$geoserver_ver = "2.17.1"
$nsis_ver = "3.05"

$repo = "geoserver"
$geoserver_base_name = $repo+"-"+$geoserver_ver
$nsis_name = "nsis-"+$nsis_ver

"This powershell script will compile "+$geoserver_base_name+" With "+$nsis_name+"!"
"The instructions implented here can be found at:"
"https://docs.geoserver.org/latest/en/developer/win-installer.html"

"Cloning repo..."

git clone https://github.com/geoserver/geoserver.git $repo
git --git-dir=$repo/.git --work-tree=$repo pull
git --git-dir=$repo/.git --work-tree=$repo checkout -f $geoserver_ver


function DownloadAndExpand($name, $file, $url, $dst)
{
	"Directory name: "+$name 
	"File name: "+$file
	"URL: "+$url
	if (![System.IO.Directory]::Exists($name)) 
	{
		if (![System.IO.File]::Exists($file)) 
		{
			"Downloading "+$file
			"From "+$url
			$webclient = New-Object System.Net.WebClient
			$webclient.DownloadFile($url,$file)
		}
		"Expanding "+$file+" to: "+$name
		Expand-Archive -LiteralPath $file -DestinationPath $dst
	}
}

$geoserver_name = $geoserver_base_name+"-bin"
$geoserver_zip = $geoserver_name+".zip"
$geoserver_url = "http://sourceforge.net/projects/geoserver/files/GeoServer/"+$geoserver_ver+"/"+$geoserver_zip


$file = $geoserver_zip
$url = $geoserver_url
$name = $geoserver_name
$dst = $name
DownloadAndExpand $name $file $url $dst

$y=$geoserver_name+"\"
copy $repo\LICENSE.txt $y 
copy $repo\src\release\GPL.txt $y
copy $repo\src\release\installer\win\*.* $y


"Installing NSIS..."

$nsis_zip = $nsis_name+".zip"
$nsis_url = "https://sourceforge.net/projects/nsis/files/NSIS%203/"+$nsis_ver+"/"+$nsis_zip+"/download"

$file = $nsis_zip
$url = $nsis_url
$name = $nsis_name
$dst = ".\"
DownloadAndExpand $name $file $url $dst

"Installing NSIS Plugin..."

$nsis_plugin_name = "AccessControl"
$nsis_plugin_zip = $nsis_plugin_name+".zip"
$nsis_plugin_url = "https://nsis.sourceforge.io/mediawiki/images/4/4a/"+$nsis_plugin_zip

$file = $nsis_plugin_zip
$url = $nsis_plugin_url
$name = $nsis_plugin_name
$dst = $name
DownloadAndExpand $name $file $url $dst

$x=$nsis_plugin_name+"\Plugins\*.*"
$y=$nsis_name+"\Plugins\x86-ansi\" 
copy $x $y
$x=$nsis_plugin_name+"\Unicode\Plugins\*.*"
$y=$nsis_name+"\Plugins\x86-unicode\"
copy $x $y

$geoserver_exe = $geoserver_base_name+".exe"
$geoserver_exe_path = $geoserver_name+"\"+$geoserver_exe

"Compiling "+$geoserver_exe_path+"..."

$makensis = $nsis_name + "\makensis.exe" 

& $makensis $geoserver_name\GeoServerEXE.nsi


Move-Item $geoserver_exe_path ".\"

$geoserver_exe + " was created!"
