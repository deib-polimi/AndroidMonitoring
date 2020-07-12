# Runtime Monitor #

## Requirements ##

Make sure to match the requirements specified on the [website](https://source.android.com/setup/build/requirements)

You can download the source and create a build without less than the required space, but you need at least 16GB of RAM and perhaps some Swap space.

## Build instructions ##

### 1. AOSP source code download ###
Download the source codes of Android following the instructions provided by the [AOSP website](https://source.android.com/setup/build/downloading)
```
repo init -u https://android.googlesource.com/platform/manifest -b android-9.0.0_r47 --depth=1

repo sync
```
This step may take a while as all the repositories weigh ~30GB

### 2. Build the AOSP source code ###
Build an image from the sources following the [instructions](https://source.android.com/setup/build/building)
```
source build/envsetup.sh

lunch 6

m
```
You may want to change the build type to **user** or **userdebug** as many applications may crash with **eng** mod.

To change the build mode you need to specify it in a file called *buildspec.mk* and located in the root folder of the sources.

You can either copy the file named *example.buildspec.mk* present in this repository and rename it or you can copy the file present in the /build folder and edit it.

The build may take some time. With a Ryzen 5 3600 CPU, an SSD and 16GB of RAM it takes about 1h20m, while with an HDD or on a VM it may take more than 3 hours.

After the build finished you may want to start the emulator and check the system:

```
emulator
```

### 3. Implement the changes

1. Copy the frameworks and system folders and overwrite the files.
2. Fix the SELinux policy

```
cp -u system/sepolicy/public/service.te system/sepolicy/prebuilts/api/28.0/public/service.te
cp -u system/sepolicy/private/service_contexts system/sepolicy/prebuilts/api/28.0/private/service_contexts
cp -u system/sepolicy/private/compat/27.0/27.0.cil system/sepolicy/prebuilts/api/28.0/private/compat/27.0/27.0.cil
cp -u system/sepolicy/private/compat/26.0/26.0.cil system/sepolicy/prebuilts/api/28.0/private/compat/26.0/26.0.cil
```

After implementing the changes you can build again the system following the same procedure.

You may want to automate the build process. You can take a look at the *example.build.sh* for an example script that copies the updated files and after the build starts the emulator. 

### 4. Generate an emulator image ###

To generate an emulator image you can follow the [instructions](https://source.android.com/setup/create/avd).

```
source build/envsetup.sh

lunch 6

make sdk sdk_repo
```

The build will probably generate some errors and crash due to some additional modules that need to be built. In that case you have to build the missing modules with:

```
make <modulename>
```
