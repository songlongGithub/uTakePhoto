package com.sl.utakephoto

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Toast
import com.sl.utakephoto.compress.CompressConfig
import com.sl.utakephoto.crop.CropOptions
import com.sl.utakephoto.exception.TakeException
import com.sl.utakephoto.manager.ITakePhotoResult
import com.sl.utakephoto.manager.UTakePhoto
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.DocumentsContract





class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        UTakePhoto.init(
//            CompressConfig.Builder().setLeastCompressSize(300).create(),
//            CropOptions.Builder().setOutputX(500).setOutputY(500).setWithOwnCrop(true).create()
//        )


        cropRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.outputBtn) {
                cropText.text = "*"
            } else {
                cropText.text = "/"
            }
        }

        capture.setOnClickListener {

            val takePhotoManager = UTakePhoto.with(this)
            if (take_photo_btn.isChecked) {

                takePhotoManager.openCamera("Pictures/uTakePhoto")
            } else {
                takePhotoManager.openAlbum()
            }
            var crop: CropOptions.Builder? = null
            if (noCropBtn.isChecked) {
                crop = null
            } else if (system_crop_btn.isChecked) {
                crop = CropOptions.Builder()
                crop.setWithOwnCrop(false)
            } else if (own_crop_btn.isChecked) {
                crop = CropOptions.Builder()
                crop.setWithOwnCrop(true)
            }
            if (crop != null && (TextUtils.isEmpty(outputX.text) || TextUtils.isEmpty(outputY.text))) {
                Toast.makeText(this@MainActivity, "请输入宽高", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (outputBtn.isChecked) {
                crop?.setOutputX(outputX.text.toString().toInt())
                    ?.setOutputY(outputY.text.toString().toInt())
            } else {
                crop?.setAspectX(outputX.text.toString().toInt())
                    ?.setAspectY(outputY.text.toString().toInt())
            }
            if (crop != null) {
                takePhotoManager.setCrop(crop.create())
            } else {
                takePhotoManager.setCrop(null)
            }
            takePhotoManager.setCameraPhotoRotate(rotateProcessing.isChecked)
            if (compress.isChecked) {
                takePhotoManager.setCompressConfig(
                    CompressConfig.Builder().setLeastCompressSize(50).setTargetUri(
                        contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                            Uri.parse("content://media/external_primary/images/media"),
                            ContentValues()
                        )

                    ).create()
                )
            } else {
                takePhotoManager.setCompressConfig(null)
            }

            takePhotoManager.build(object : ITakePhotoResult {
                override fun takeSuccess(uriList: MutableList<Uri>?) {
                    uriList?.get(0)?.let { it1 ->

                        val pfd = contentResolver.openFileDescriptor(it1, "r")
                        if (pfd != null) {
                            val bitmap =
                                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                            photoIv.setImageBitmap(bitmap)
                        }

                    }

                }

                override fun takeFailure(ex: TakeException?) {
                    if (ex != null) {
                        Toast.makeText(this@MainActivity, ex.message, Toast.LENGTH_LONG)
                            .show()
                    }
                }


                override fun takeCancel() {
                }

            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==100&&resultCode== Activity.RESULT_OK){
            Toast.makeText(this,"授权成功",Toast.LENGTH_SHORT).show()
        }
    }
}
