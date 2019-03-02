rm -rf out 
cd core
. createjar.sh
cd ..
mkdir -p prebuilt_release/bin/
mv core/chkbugreport*.jar prebuilt_release/bin/chkbugreport.jar
echo Output is in $(pwd)/prebuilt_release/bin/
ls prebuilt_release/bin/



