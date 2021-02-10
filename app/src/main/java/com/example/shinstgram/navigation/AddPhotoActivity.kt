package com.example.shinstgram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.shinstgram.R
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("로그", "test")
        setContentView(R.layout.activity_add_photo)

        // Initialize storage
        storage = FirebaseStorage.getInstance()
        // Open the album
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
        // add image upload event
        btn_upload.setOnClickListener {
            contentUpload()
        }

        addphoto_image.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK) {
                // this is path to the selected image
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)

            } else {
                // 사진선택 취소 했을 때
                finish();
            }
        }
    }
    fun contentUpload() {
        // make filename
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //file upload
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            Toast.makeText(this,getString(R.string.upload_success),Toast.LENGTH_LONG).show()
        }
    }
}