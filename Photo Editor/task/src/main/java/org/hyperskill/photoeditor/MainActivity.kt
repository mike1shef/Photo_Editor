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
import android.provider.MediaStore
import android.provider.MediaStore.Images
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

private const val MEDIA_REQUEST_CODE = 0

class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    private lateinit var originalImage : Bitmap
    private lateinit var slider: Slider
    private lateinit var btnSave : Button
    private lateinit var btnGallery : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        slider.addOnChangeListener ( Slider.OnChangeListener { _, value, _ ->
                currentImage.setImageBitmap(adjustBrightness(value.toInt()))

            })

        //do not change this line
        currentImage.setImageBitmap(createBitmap())
        originalImage = (currentImage.drawable as BitmapDrawable).bitmap

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
            originalImage = (currentImage.drawable as BitmapDrawable).bitmap
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
        slider = findViewById(R.id.slBrightness)
        btnSave = findViewById(R.id.btnSave)
        btnGallery = findViewById(R.id.btnGallery)
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
