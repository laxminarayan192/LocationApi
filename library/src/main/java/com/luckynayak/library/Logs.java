package com.luckynayak.library;

import android.util.Log;

import static com.luckynayak.library.BuildConfig.DEBUG;

/**
 * Created by Lucky on 4/7/2017.
 */

class Logs {
    public static void wtf(Object object, String s, String s1) {
        if (! DEBUG)
            Log.wtf(object.getClass().getSimpleName() + "->" + s, s1);
    }
}
