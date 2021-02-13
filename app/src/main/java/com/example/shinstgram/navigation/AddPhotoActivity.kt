package com.example.shinstgram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AddPhotoActivity : AppCompatActivity() {

    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    // 사용자의 정보를 담을 변수
    var auth : FirebaseAuth? = null
    // DB 변수, 실시간 데이터 베이스
    var firestore : FirebaseFirestore? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("로그", "test")
        setContentView(R.layout.activity_add_photo)

        // Initialize
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
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
    @RequiresApi(Build.VERSION_CODES.O)
    fun contentUpload() {
        // 현재 시간 얻기
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val formatted = current.format(formatter)
//        println("Current: $formatted")
        Log.d("시간", formatted)
        // make filename
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        // 업로드할 이미지 name 선언
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        // 업로드에는 두 가지 방법이 있다

        // 1. promise
        storageRef?.putFile(photoUri!!)?.continueWithTask {
            task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener {
            uri ->
            var contentDTO = ContentDTO()
            // Insert downloadUrl of Image
            contentDTO.imageUrl = uri.toString()
            // Insert uid of user
            contentDTO.uid = auth?.currentUser?.uid
            contentDTO.userId = auth?.currentUser?.email
            contentDTO.explain = addphoto_edit_explain.text.toString()
            contentDTO.uploadTime = formatted
            contentDTO.timestamp = System.currentTimeMillis()
            // 데이터를 모아서 firestore 에 추가
            firestore?.collection("images")?.document()?.set(contentDTO)
            // 정상적으로 종료한다는 flag
            setResult(Activity.RESULT_OK)
            finish()
        }

        // 2. callback 방식식
       //Callback method
//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
//            storageRef.downloadUrl.addOnSuccessListener { uri ->
//                var contentDTO = ContentDTO()
//
//                // Insert downloadUrl of Image
//                contentDTO.imageUrl = uri.toString()
//                // Insert uid of user
//                contentDTO.uid = auth?.currentUser?.uid
//                contentDTO.userId = auth?.currentUser?.email
//                contentDTO.explain = addphoto_edit_explain.text.toString()
//                contentDTO.timestamp = System.currentTimeMillis()
//                // 데이터를 모아서 firestore 에 추가
//                firestore?.collection("images")?.document()?.set(contentDTO)
//                // 정상적으로 종료한다는 flag
//                setResult(Activity.RESULT_OK)
//                finish()
//            }
//        }
    }
}