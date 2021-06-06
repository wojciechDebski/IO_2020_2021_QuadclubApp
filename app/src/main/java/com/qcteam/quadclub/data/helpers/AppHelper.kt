package com.qcteam.quadclub.data.helpers

import android.graphics.Bitmap

object AppHelper {

    public fun resizePhoto(maxSize: Int, bitmap: Bitmap): Bitmap {
        val outWidth: Int
        val outHeight: Int
        val inWidth: Int = bitmap.width
        val inHeight: Int = bitmap.height
        if (inWidth > inHeight) {
            outWidth = maxSize
            outHeight = inHeight * maxSize / inWidth
        } else {
            outHeight = maxSize
            outWidth = inWidth * maxSize / inHeight
        }

        return Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false)
    }
}