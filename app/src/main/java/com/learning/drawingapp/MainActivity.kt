package com.learning.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var mDrawingView : DrawingView? = null
    private var brushBtn : ImageButton? = null
    private var mColorCurrent : ImageButton? = null
    var customProgressDialog : Dialog? = null

    val openGalleryLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == RESULT_OK && result.data!=null){
            val imageBg : ImageView = findViewById(R.id.iv_bgImage)
            imageBg.setImageURI(result.data?.data)
        }
    }

    val requestPermission : ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permission ->
            permission.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted){
                    Toast.makeText(this,"Permission is granted!",Toast.LENGTH_SHORT).show()

                    val intent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(intent)
                }
                else{
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "oops you just denied the permission!",Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDrawingView = findViewById(R.id.drawingView)
        brushBtn = findViewById(R.id.ib_brush)
        val galleryBtn : ImageButton = findViewById(R.id.imgSelect)
        val undoBtn : ImageButton = findViewById(R.id.undoBtn)
        val saveImgBtn : ImageButton = findViewById(R.id.imgSave)

        val linearLayoutPainColors = findViewById<LinearLayout>(R.id.ll_color_pallet)

        mColorCurrent = linearLayoutPainColors[1] as ImageButton

        brushBtn?.setOnClickListener {
            selectBrushSizeChooserDialog()
        }

        galleryBtn.setOnClickListener {
                requestStoragePermission()
        }

        undoBtn.setOnClickListener {
            mDrawingView!!.undoPaths()
        }

        saveImgBtn.setOnClickListener {
            if(isReadPermissionGranted()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.drawingView_container)
                    saveBitmapFile(getBitMapFromView(flDrawingView))
                }
            }
        }
    }

    private fun selectBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Select Brush : ")

        val smallButton : ImageView = brushDialog.findViewById(R.id.ib_size_small)
        smallButton.setOnClickListener{
            mDrawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumButton : ImageView = brushDialog.findViewById(R.id.ib_size_medium)
        mediumButton.setOnClickListener{
            mDrawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }

        val largeButton : ImageView = brushDialog.findViewById(R.id.ib_size_large)
        largeButton.setOnClickListener{
            mDrawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    private fun isReadPermissionGranted():Boolean{
        var result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("Kids Drawing App","Kids Drawing App"+
            "Needs access to your external storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    fun selectColorFromThePallet(view : View){
        if(view !== mColorCurrent){
            val imageButton = view as ImageButton
            val colorString = imageButton.tag.toString()
            mDrawingView!!.setColor(colorString)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.color_selected)
            )

            mColorCurrent!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_color)
            )

            mColorCurrent = view

        }
    }

    private fun getBitMapFromView(view: View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.custom_progress_dialog)
        customProgressDialog?.show()
    }

    private fun killProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private suspend fun saveBitmapFile(mBitmap : Bitmap):String {
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val byte = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90, byte)

                    val f = File(externalCacheDir?.absoluteFile.toString()+File.separator+"KidDrawingApp_"+System.currentTimeMillis()/1000+".png")
                    val fo = FileOutputStream(f)
                    fo.write(byte.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        killProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File saved successfully at $result",Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,"Something went wrong saving the file!",Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch (e : Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
                _, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
                shareIntent.type = "image/png"
                startActivity(shareIntent)
        }
    }

    private fun showRationaleDialog(
        title : String,
        message : String){
            val builder : AlertDialog.Builder = AlertDialog.Builder(this)

            builder.setTitle(title).setMessage(message).setPositiveButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
}