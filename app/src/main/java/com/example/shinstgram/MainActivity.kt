package com.example.shinstgram

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.shinstgram.navigation.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*

// Bottom Navigation View 의 item selected listener 를 implements 한다
class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when(item.itemId) {
            R.id.action_home -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            R.id.action_search -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment).commit()
                return true
            }

            R.id.action_add_photo -> {
                // 외부 저장소 권한 허가 받았는지 체크
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 사진 선택 activity 로 이동
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }
                return true
            }
            R.id.action_favorite_alarm -> {
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment).commit()
                return true
            }
            R.id.action_account -> {
                val userFragment = UserFragment()
                // uid 데이터를 bundle 에 넣어서 user fragment 에 보낸다.
                val bundle = Bundle()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }
        }
        return false
    }
    fun setToolbarDefault() {
        toolbar_username.visibility = View.GONE
        toolbar_btn_back.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }
    // token 생성 메소드
    fun registerPushToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
        task ->
            val token = task.result?.token
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String,Any>()
            map["pushToken"] = token!!

            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)

        }
//        FirebaseInstallations.getInstance().
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Bottom Navigation view selected listener
        bottom_navigation.setOnNavigationItemSelectedListener(this)
        // 외부 저장소 사용 권한 요청
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        // set default screen
        bottom_navigation.selectedItemId = R.id.action_home
        // token 생성 메소드
        registerPushToken()
    }

    // 프로필 이미지 업데이트 메소드  / 2021.02.11
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == UserFragment.PICKER_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK ) {
            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            // FirebaseStorage(서버 저장소) 변수 초기화
            // 이미지를 서버에 저장
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            // 서버에 이미지파일 저장 성공하면, Firebase Firestore (DB)에 추가
            }.addOnSuccessListener {uri ->
                var map = HashMap<String, Any> ()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }

}

