#!/bin/bash
set -e
export ANDROID_HOME=~/android-sdk
export BUILD_TOOLS=$ANDROID_HOME/build-tools/33.0.2
export PLATFORM=$ANDROID_HOME/platforms/android-33/android.jar
export PROJECT="$(cd "$(dirname "$0")" && pwd)"
export SRC=$PROJECT/app/src/main
export BUILD=$PROJECT/build
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

rm -rf $BUILD
mkdir -p $BUILD/gen $BUILD/classes $BUILD/dex $BUILD/apk $BUILD/obj

echo "=== Step 1: Compile resources ==="
find $SRC/res -type f \( -name "*.xml" -o -name "*.png" \) | while read f; do
    $BUILD_TOOLS/aapt2 compile "$f" -o $BUILD/obj/ 2>/dev/null || true
done
echo "Resources compiled"

echo "=== Step 2: Link resources ==="
$BUILD_TOOLS/aapt2 link --manifest $SRC/AndroidManifest.xml -I $PLATFORM --java $BUILD/gen -o $BUILD/apk/base.apk --auto-add-overlay $BUILD/obj/*.flat 2>&1
echo "Resources linked"

echo "=== Step 3: Add assets to APK ==="
cd $BUILD/apk
if [ -d "$SRC/assets" ]; then
    mkdir -p assets_tmp/assets
    cp "$SRC/assets"/* assets_tmp/assets/
    cd assets_tmp
    zip -r ../base.apk assets/
    cd ..
    rm -rf assets_tmp
fi
cd $PROJECT
echo "Assets added"

echo "=== Step 4: Compile Java ==="
find $SRC/java -name "*.java" > $BUILD/java_files.txt
find $BUILD/gen -name "*.java" >> $BUILD/java_files.txt
javac -source 8 -target 8 -classpath $PLATFORM -d $BUILD/classes @$BUILD/java_files.txt 2>&1
echo "Java compiled"

echo "=== Step 5: Convert to DEX ==="
$BUILD_TOOLS/d8 --min-api 21 --output $BUILD/dex/ $(find $BUILD/classes -name "*.class" | tr '\n' ' ') 2>&1
echo "DEX created"

echo "=== Step 6: Build APK ==="
cp $BUILD/apk/base.apk $BUILD/test.apk
zip -j $BUILD/test.apk $BUILD/dex/classes.dex
echo "APK built"

echo "=== Step 7: Sign APK ==="
if [ ! -f $PROJECT/release.keystore ]; then
    echo "WARNING: no release.keystore found; generating a new one."
    echo "IMPORTANT: for release builds copy your release keystore into the project first,"
    echo "or all previously installed copies will be seen as a different app."
    : "${KEYSTORE_PASS:=testmock123}"
    echo "Generating keystore with generated password (dev only): $KEYSTORE_PASS"

    keytool -genkeypair -v -keystore $PROJECT/release.keystore -alias testmock -keyalg RSA -keysize 2048 -validity 10000 -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" -dname "CN=Test Mock, OU=Dev, O=Test, L=Beijing, ST=Beijing, C=CN" 2>&1
fi
$BUILD_TOOLS/apksigner sign --ks $PROJECT/release.keystore --ks-key-alias testmock --ks-pass pass:"$KEYSTORE_PASS" --key-pass pass:"$KEYSTORE_PASS" --out $PROJECT/build/test-location.apk $BUILD/test.apk 2>&1
echo "APK signed"

echo "APK ready at: $PROJECT/build/test-location.apk"
echo "=== Build complete ==="
ls -la $PROJECT/build/
