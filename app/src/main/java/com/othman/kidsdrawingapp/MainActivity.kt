package com.othman.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var imageButtonCurrentPaint : ImageButton? = null
    private var ibColorPickerTest : ImageButton? = null
    private var colorPickerDefaultColor : Int = 0
    private var colorPickerDialog : AmbilWarnaDialog? = null
    private var customProgressBarDialog : Dialog?  = null
    private val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val ivBackground = findViewById<ImageView>(R.id.ivBackground)
                ivBackground.setImageURI(result.data?.data)
            }
        }
    private val storageResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (isGranted) {
                        Toast.makeText(this, "Permission granted now you can access storage", Toast.LENGTH_SHORT).show()
                        openGalleryLauncher.launch(pickIntent)
                    } else {
                        Toast.makeText(this, "Oops you just denied storage permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)

        drawingView?.setBrushSize(20f)
        val linerLayoutPaintColors = findViewById<LinearLayout>(R.id.llPaintColors)
        imageButtonCurrentPaint = linerLayoutPaintColors[1] as ImageButton
        ibColorPickerTest = linerLayoutPaintColors[8] as ImageButton
        imageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        val ibBrush : ImageButton = findViewById(R.id.ibBrush)
        ibBrush.setOnClickListener{
            showBushSizeChooserDialog()
        }

        val ibGallery : ImageButton = findViewById(R.id.ibGallery)
        ibGallery.setOnClickListener{
            if (!isReadStorageAllowed()) {
                permissionLauncher()
            }else{
                openGalleryLauncher.launch(pickIntent)
            }
        }

        val ibUndo : ImageButton = findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val ibRedo : ImageButton = findViewById(R.id.ibRedo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val ibSave : ImageButton = findViewById(R.id.ibSave)
        ibSave.setOnClickListener{
            if (isReadStorageAllowed()){
                showProgressBarDialog()
                lifecycleScope.launch{
                    val flDrawingView = findViewById<FrameLayout>(R.id.flDrawingViewContainer)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }else{
                permissionLauncher()
            }
        }

    }

    private fun permissionLauncher() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ||
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            showRationalDialog(
                "Kids Drawing App ",
                "Kids Drawing App needs to Access Your External Storage"
            )
        } else {
            storageResultLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showBushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("brush size ")
        var ibSmallBrush : ImageButton = brushDialog.findViewById(R.id.ibSmallBrush)
        ibSmallBrush.setOnClickListener{
            drawingView?.setBrushSize(10.0f)
            brushDialog.dismiss()
        }
        var ibMediumBrush : ImageButton = brushDialog.findViewById(R.id.ibMediumBrush)
        ibMediumBrush.setOnClickListener{
            drawingView?.setBrushSize(20.0f)
            brushDialog.dismiss()
        }
        var ibBigBrush : ImageButton = brushDialog.findViewById(R.id.ibBigBrush)
        ibBigBrush.setOnClickListener{
            drawingView?.setBrushSize(30.0f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClick(view: View){
       if (view !== imageButtonCurrentPaint) {
           var imageButton = view as ImageButton
           if (imageButton !== ibColorPickerTest) {
               var colorTag = Color.parseColor(imageButton.tag.toString())
               drawingView?.setColor(colorTag)
           } else {
               val colorPickerListener = object : AmbilWarnaDialog.OnAmbilWarnaListener {
                   override fun onCancel(dialog: AmbilWarnaDialog?) {
                   }
                   override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                       drawingView?.setColor(color)
                       colorPickerDefaultColor = color
                   }
               }
               colorPickerDialog = AmbilWarnaDialog(this, colorPickerDefaultColor, colorPickerListener)
               colorPickerDialog?.show()
           }
           imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
           imageButtonCurrentPaint?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
           imageButtonCurrentPaint = imageButton
       }
        if (view == ibColorPickerTest){
            colorPickerDialog?.show()
        }
    }

    private fun showRationalDialog(title : String, message : String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        /* val bgDrawable = view.background // no matter
         if (bgDrawable != null) {
             bgDrawable.draw(canvas)
         }else{
             canvas.drawColor(Color.WHITE)
         }*/
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap) : String {
        var result = ""
        withContext(Dispatchers.IO){
            if (bitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val folder = File(getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString(),"kids" )
                    if (!folder.exists()) {
                        folder.mkdir()
                    }
                    val path = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + "kids"
                    val file = File(path + File.separator + "kidsDrawingApp_" + System.currentTimeMillis()/1000 + ".png")
                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(bytes.toByteArray())
                    fileOutputStream.close()
                    result  = file.absolutePath

                    runOnUiThread{
                        if (result.isNotEmpty()){
                            cancelProgressBarDialog()
                            alertDialogFun(result)
                        }
                    }
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun isReadStorageAllowed() : Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun isWriteStorageAllowed() : Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun showProgressBarDialog() {
        customProgressBarDialog = Dialog(this)
        customProgressBarDialog?.setContentView(R.layout.custo_dialog_progress_layout)
        customProgressBarDialog?.setCancelable(false)
        customProgressBarDialog?.show()
    }

    private fun cancelProgressBarDialog() {
        if (customProgressBarDialog != null) {
            customProgressBarDialog?.dismiss()
            customProgressBarDialog = null
        }
    }

    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private fun alertDialogFun(result:String){
        val builder  = AlertDialog.Builder(this)
        builder.setTitle("Kids Drawing App")
        builder.setMessage("Do you want to share this image")
        builder.setIcon(android.R.drawable.ic_menu_share)
        builder.setPositiveButton("yes"){dialog, which ->
            shareImage(result)
            dialog.dismiss()
        }
        builder.setNegativeButton("no"){dialog, which ->
            Toast.makeText(
                this@MainActivity,
                "File saved successfully: $result",
                Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        val alertDialog : AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

}