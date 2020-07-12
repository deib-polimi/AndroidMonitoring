cp -TRvu ./buildspec.mk ~/Desktop/pie/buildspec.mk
cp -TRvu ./frameworks ~/Desktop/pie/frameworks/
cp -TRvu ./system ~/Desktop/pie/system/
cd ~/Desktop/pie
cp -u system/sepolicy/public/service.te system/sepolicy/prebuilts/api/28.0/public/service.te
cp -u system/sepolicy/private/service_contexts system/sepolicy/prebuilts/api/28.0/private/service_contexts
cp -u system/sepolicy/private/compat/27.0/27.0.cil system/sepolicy/prebuilts/api/28.0/private/compat/27.0/27.0.cil
cp -u system/sepolicy/private/compat/26.0/26.0.cil system/sepolicy/prebuilts/api/28.0/private/compat/26.0/26.0.cil
source build/envsetup.sh
lunch 6
m
emulator
