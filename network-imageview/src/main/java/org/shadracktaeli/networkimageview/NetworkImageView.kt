package org.shadracktaeli.networkimageview

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.shadracktaeli.networkimageview.R.attr.imageUrl
import org.shadracktaeli.networkimageview.glide.CacheStrategy
import org.shadracktaeli.networkimageview.glide.ImageLoadingListener

class NetworkImageView @JvmOverloads constructor(
        context: Context, attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

    // Views
    private val imageView: ImageView
    private val progressBar: ProgressBar

    // Image url to load
    private var imageUrl: String?
    // Placeholder drawable resource
    var placeholderDrawableRes: Int
    // Error drawable resource
    var errorDrawableRes: Int
    // Cache type
    var cacheStrategy: CacheStrategy
    // Show progress loader
    private var showLoader: Boolean
    // Image loading listener
    private var imageLoadingListener: ImageLoadingListener? = null

    init {
        // Inflate views
        LayoutInflater.from(context).inflate(R.layout.network_image_view, this)
        imageView = findViewById(R.id.image)
        progressBar = findViewById(R.id.progress_bar)

        val attributes = context
            .obtainStyledAttributes(attributeSet, R.styleable.NetworkImageView, 0, 0)

        // Get image url
        imageUrl = attributes.getString(R.styleable.NetworkImageView_imageUrl)
        // Get placeholder drawable resource
        placeholderDrawableRes = attributes
            .getResourceId(R.styleable.NetworkImageView_placeholderDrawable, DEFAULT_PLACEHOLDER_VALUE)
        // Get error drawable resource
        errorDrawableRes = attributes
            .getResourceId(R.styleable.NetworkImageView_errorDrawable, DEFAULT_PLACEHOLDER_VALUE)
        // Get cache strategy
        cacheStrategy = CacheStrategy.values()[attributes.getInt(R.styleable.NetworkImageView_cacheStrategy, DEFAULT_CACHE_STRATEGY.ordinal)]
        // Get show progress loader
        showLoader = attributes
            .getBoolean(R.styleable.NetworkImageView_showLoader, DEFAULT_SHOW_IMAGE_LOADER)

        // Recycle attributes
        attributes.recycle()
    }

    fun setImageUrl(imageUrl: String) {
        this.imageUrl = imageUrl
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        // Load image
        loadImageInternal()
    }

    /**
     * Loads an image into [NetworkImageView] with the specified [imageUrl]
     */
    private fun loadImageInternal() {
        // Check if imageUrl is a valid url
        if (TextUtils.isEmpty(imageUrl) || !URLUtil.isValidUrl(imageUrl)) {
            val errorMessageRes = if (TextUtils.isEmpty(imageUrl)) R.string.error_blank_image_url else R.string.error_invalid_image_url
            Log.e(TAG, context.getString(errorMessageRes))
            // Show error placeholder
            imageView.setImageResource(errorDrawableRes)
            return
        }

        imageUrl?.apply {
            // Show loading progress
            showProgressBar()

            // @formatter:off
            Glide.with(context)
                .load(this)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                    ): Boolean {
                        hideProgressBar()
                        imageLoadingListener?.onLoadFailed(e)
                        return false
                    }

                    override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                    ): Boolean {
                        hideProgressBar()
                        imageLoadingListener?.onLoaded()
                        return false
                    }
                })
                .apply(RequestOptions()
                    .placeholder(placeholderDrawableRes)
                    .error(errorDrawableRes)
                    .diskCacheStrategy(cacheStrategy.value)
                )
                .into(imageView)
            // @formatter:on
        }
    }

    fun loadImage(
            imageUrl: String, @DrawableRes placeholderDrawableRes: Int = DEFAULT_PLACEHOLDER_VALUE, @DrawableRes errorDrawableRes: Int = DEFAULT_PLACEHOLDER_VALUE,
            showLoader: Boolean = false
    ) {
        this.imageUrl = imageUrl
        this.placeholderDrawableRes = placeholderDrawableRes
        this.errorDrawableRes = errorDrawableRes
        this.showLoader = showLoader
        loadImageInternal()
    }

    private fun showProgressBar() {
        if (showLoader) {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    companion object {
        private const val TAG = "NetworkImageView"
        private val DEFAULT_PLACEHOLDER_VALUE = R.color.network_image_view_placeholder_color
        private val DEFAULT_CACHE_STRATEGY = CacheStrategy.NONE
        private const val DEFAULT_SHOW_IMAGE_LOADER = false
    }
}