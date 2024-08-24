package com.example.ndk_demo_lib;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Namespace;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.StdString;

/**
 * TODO: Add Description
 *
 * @author eriche 2024/8/24
 */
@Platform(include = "LegacyLibrary.h", library = "native-lib")
@Namespace("LegacyLibrary")
public class LegacyLibrary {
    public static class LegacyClass extends Pointer {
        static {
            Loader.load();
        }

        public LegacyClass() {
            allocate();
        }

        private native void allocate();

        // to call the getter and setter functions
        public native @StdString String get_property();

        public native void set_property(String property);

        // to access the member variable directly
        public native @StdString String property();

        public native void property(String property);
    }
}