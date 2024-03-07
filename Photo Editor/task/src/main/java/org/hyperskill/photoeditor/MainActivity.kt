package org.hyperskill.photoeditor

import android.app.Activity
import android.content.Intent
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore.Images
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.pow

private const val MEDIA_REQUEST_CODE = 0
private const val TAG = "MainActivity Photo"


class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    private lateinit var originalImage : Bitmap
    private lateinit var tempImage : Bitmap
    private lateinit var sliderBrightness: Slider
    private lateinit var sliderContrast: Slider
    private lateinit var sliderSaturation: Slider
    private lateinit var sliderGamma: Slider
    private lateinit var btnSave : Button
    private lateinit var btnGallery : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        var lastJob : Job? = null

        fun applyFilters(
            brightness: Int,
            contrast: Int,
            saturation: Int,
            gamma: Float
        ) {
            lastJob?.cancel()

            lastJob = GlobalScope.launch(Dispatchers.Default) {
                val bitmap = currentImage.drawable?.toBitmap() ?: return@launch

                tempImage = async{ adjustBrightness(brightness) }.await()
                Log.d(TAG, "Brightness is adjusted")
                async { adjustContrast(tempImage, contrast) }.await()
                Log.d(TAG, "Contrast is adjusted")
                async { adjustSaturation(tempImage, saturation) }.await()
                Log.d(TAG, "Saturation is adjusted")
                async { adjustGamma(tempImage, gamma) }.await()
                Log.d(TAG, "Gamma is adjusted")

                ensureActive()

                runOnUiThread {
                    currentImage.setImageBitmap(tempImage)
                    Log.d(TAG, "Current Screen is updated")
                }
            }
        }

        sliderBrightness.addOnChangeListener ( Slider.OnChangeListener { _, value, _ ->
                applyFilters(
                    value.toInt(),
                    sliderContrast.value.toInt(),
                    sliderSaturation.value.toInt(),
                    sliderGamma.value
                )
            })

        sliderContrast.addOnChangeListener(Slider.OnChangeListener {_, value, _ ->
                applyFilters(
                    sliderBrightness.value.toInt(),
                    value.toInt(),
                    sliderSaturation.value.toInt(),
                    sliderGamma.value
                )
        })

        sliderSaturation.addOnChangeListener(Slider.OnChangeListener {_, value, _ ->
            applyFilters(
                    sliderBrightness.value.toInt(),
                    sliderContrast.value.toInt(),
                    value.toInt(),
                    sliderGamma.value
                )
        })

        sliderGamma.addOnChangeListener(Slider.OnChangeListener {_, value, _ ->
                applyFilters(
                    sliderBrightness.value.toInt(),
                    sliderContrast.value.toInt(),
                    sliderSaturation.value.toInt(),
                    value
                )
        })


        //do not change this line
        currentImage.setImageBitmap(createBitmap())
        originalImage = (currentImage.drawable as BitmapDrawable).bitmap
        tempImage = originalImage

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
            originalImage = (currentImage.drawable as BitmapDrawable).bitmap
            tempImage = originalImage
        }

        btnSave.setOnClickListener {
            when(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                PackageManager.PERMISSION_GRANTED -> {

                    val bitmap: Bitmap = currentImage.drawable.toBitmap()
                    val values = ContentValues()
                    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    values.put(Images.Media.MIME_TYPE, "image/jpeg")
                    values.put(Images.ImageColumns.WIDTH, bitmap.width)
                    values.put(Images.ImageColumns.HEIGHT, bitmap.height)

                    val uri = this@MainActivity.contentResolver.insert(
                        Images.Media.EXTERNAL_CONTENT_URI, values
                    ) ?: return@setOnClickListener

                    contentResolver.openOutputStream(uri).use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }
                }
                PackageManager.PERMISSION_DENIED -> {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MEDIA_REQUEST_CODE)
                }
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode){
            MEDIA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    btnSave = findViewById(R.id.btnSave)
                    btnSave.callOnClick()
                } else {
                    Toast.makeText(this, "Image cannot be saved without permission", Toast.LENGTH_SHORT).show()
                }

            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
        sliderBrightness = findViewById(R.id.slBrightness)
        sliderContrast = findViewById(R.id.slContrast)
        btnSave = findViewById(R.id.btnSave)
        btnGallery = findViewById(R.id.btnGallery)
        sliderSaturation = findViewById(R.id.slSaturation)
        sliderGamma = findViewById(R.id.slGamma)
    }

    private val activityResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoUri = result.data?.data ?: return@registerForActivityResult
                // code to update ivPhoto with loaded image
                currentImage.setImageURI(photoUri).also { originalImage = currentImage.drawable.toBitmap() }
            }
        }

    // do not change this function
    private fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var r: Int
        var g: Int
        var b: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                r = x % 100 + 40
                g = y % 100 + 80
                b = (x+y) % 100 + 120

                pixels[index] = Color.rgb(r,g,b)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }

    private fun adjustBrightness(offset : Int) : Bitmap {

        val image = originalImage.copy(Bitmap.Config.RGB_565, true)

        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0,0, image.width, image.height)

        for (i in pixels.indices) {
                val oldColor = pixels[i]

                val newColor = Color.rgb(
                    addOffset(oldColor.red, offset),
                    addOffset(oldColor.green, offset),
                    addOffset(oldColor.blue, offset)
                )

                pixels[i] = newColor
        }
        image.setPixels(pixels, 0, image.width, 0,0, image.width, image.height)
        return image
    }

    private fun addOffset(color: Int, offset: Int): Int {
        val newColorValue = color + offset
        return if (newColorValue > 255) 255
        else if (newColorValue < 0) 0
        else newColorValue
    }

    private fun adjustContrast(image : Bitmap, contrast : Int) : Bitmap {

        val width = image.width
        val height = image.height
        var total = 0

        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, width, 0,0, width, height)

        for (i in pixels.indices) {
            total += (pixels[i].red + pixels[i].green + pixels[i].blue)
        }

        val avgBrightness = total/(image.width * image.height * 3).toLong()
        val alpha : Double = (255 + contrast)/(255 - contrast).toDouble()

        fun makeContrast (color: Int) : Int {
            val newColorValue = (alpha * (color - avgBrightness) + avgBrightness).toInt()
            return if (newColorValue > 255) 255
            else if (newColorValue < 0) 0
            else newColorValue
        }

        for (i in pixels.indices) {

            val newColor = Color.rgb(
                makeContrast(pixels[i].red),
                makeContrast(pixels[i].green),
                makeContrast(pixels[i].blue)
            )
            pixels[i] = newColor
        }


        image.setPixels(pixels, 0, image.width, 0,0, image.width, image.height)
        return image
    }
    private fun adjustSaturation(image: Bitmap, saturation: Int): Bitmap {

        val width = tempImage.width
        val height = tempImage.height
        val alpha : Double = (255 + saturation)/(255 - saturation).toDouble()

        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, width, 0,0, width, height)

        fun makeAdjust (color: Int, rgbAvg : Int) : Int {
            val newColorValue = (alpha * (color - rgbAvg) + rgbAvg).toInt()
            return if (newColorValue > 255) 255
            else if (newColorValue < 0) 0
            else newColorValue
        }

        for (i in pixels.indices) {
            val rgbAvg : Int = (pixels[i].red + pixels[i].green + pixels[i].blue)/3

            val newRed = makeAdjust(pixels[i].red, rgbAvg)
            val newGreen = makeAdjust(pixels[i].green, rgbAvg)
            val newBlue = makeAdjust(pixels[i].blue, rgbAvg)


            val newColor = Color.rgb(
                newRed,
                newGreen,
                newBlue
            )

            pixels[i] = newColor
        }

        image.setPixels(pixels, 0, image.width, 0,0, image.width, image.height)
        return image
    }
    private fun adjustGamma(image: Bitmap, gamma: Float): Bitmap {

        val width = tempImage.width
        val height = tempImage.height


        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, width, 0,0, width, height)

        for (i in pixels.indices) {

            fun calculateGammaColor (color: Int) : Int {
                val newColor = 255 * ((color.toFloat() / 255).pow(gamma))
                return newColor.toInt()

            }

            val newRed = calculateGammaColor(pixels[i].red)
            val newGreen = calculateGammaColor(pixels[i].green)
            val newBlue = calculateGammaColor(pixels[i].blue)

            val newColor = Color.rgb(
                newRed,
                newGreen,
                newBlue
            )

            pixels[i] = newColor
        }

        image.setPixels(pixels, 0, image.width, 0,0, image.width, image.height)
        return image
    }

}
