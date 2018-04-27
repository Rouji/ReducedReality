# Build
Compile using Android Studio or using `./gradlew assemble`

## OpenCV
To install the OpenCV libraries this app depends on, you need to install the "OpenCV Manager" app.  
Since the version on the Google PlayStore is too old, please use the correct one for your architecture in the included **opencv_manager** dir. (Or feel free to build it yourself; [this GitHub repo](https://github.com/tzutalin/build-opencv-for-android) is a good starting point for doing so)

# Running
The project targets SDK 25, so you'll need a phone with at least Android 7.1.
A debug-compiled APK is provided in the root directory, which can be installed using ```adb install reducedreality-debug.apk``` or by copying onto the device and installing from there, provided sideloading of apps is enabled.


## Permissions
For now, the app uses only the old way of specifying permissions in its manifest file. Depending on your ROM you might need to go into the app's permissions and explicitly allow camera access. You should then still be able to run as debug from within Android Studio.

# Caveat Emptor
Tested on a few Nexus, Samsung devices and a Oneplus One, all running Lineage OS 14. Apart from performance, functionally it worked the same on all of them. Please be aware, that if you have other hardware or a different ROM things might break. YMMV.
