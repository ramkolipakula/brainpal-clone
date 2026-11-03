package com.brainrot.rebuilt.ui.components

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MascotView(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setImageResource(resId)
                contentDescription?.let { this.contentDescription = it }
                (drawable as? Animatable)?.start()
            }
        },
        update = { imageView ->
            imageView.setImageResource(resId)
            contentDescription?.let { imageView.contentDescription = it }
            (imageView.drawable as? Animatable)?.start()
        }
    )
}

