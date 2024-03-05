package org.hyperskill.photoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.drawToBitmap
import com.google.android.material.slider.Slider


class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    private lateinit var originalImage : Bitmap
    private lateinit var slider: Slider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        currentImage.setImageBitmap(createBitmap())
        originalImage = (currentImage.drawable as BitmapDrawable).bitmap

        val btnGallery = findViewById<Button>(R.id.btnGallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
            originalImage = (currentImage.drawable as BitmapDrawable).bitmap

        }

        slider.addOnChangeListener (
            Slider.OnChangeListener { _, value, _ ->
                currentImage.setImageBitmap(adjustBrightness(value.toInt()))

            }
        )
    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
        slider = findViewById<Slider>(R.id.slBrightness)
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
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x+y) % 100 + 120

                pixels[index] = Color.rgb(R,G,B)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }

    fun adjustBrightness(offset : Int) : Bitmap {

        val image = originalImage.copy(Bitmap.Config.RGB_565, true)

        fun addOffset(color: Int, offset: Int): Int {
            val newColorValue = color + offset
            return if (newColorValue > 255) 255
            else if (newColorValue < 0) 0
            else newColorValue
        }

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

}
