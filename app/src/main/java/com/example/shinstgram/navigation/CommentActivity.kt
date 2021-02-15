package com.example.shinstgram.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.bumptech.glide.request.RequestOptions
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.AlarmDTO
import com.example.shinstgram.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import kotlinx.android.synthetic.main.activity_add_photo.view.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import kotlinx.android.synthetic.main.item_detail.view.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class CommentActivity : AppCompatActivity() {

    var contentUid : String? = null
    var destinationUid : String? = null
    var imageUrl : String? = null
    var writer : String? = null
    var explain : String? = null
    var profileUrl : String? = null
    var storage : FirebaseStorage? = null
    var firestore : FirebaseFirestore? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    var likeCount : Int? = 0
    var commentCount : Int? = 0
    var item : ContentDTO? = null
    var PICK_IMAGE_FROM_ALBUM = 0
    var photoUri : Uri? = null
    var dialogView : View? = null

    companion object {
        val TAG : String = "comment Activity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        // firestore 변수 초기화
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // 현재 App 사용자 정보
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        // 댓글 작성자 정보
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        // 게시글 데이터 세팅 메소드
        // 게시글 정보 서버로부터 받아오기
        setDataFromServer()
        // 게시글 작성자 프로필 이미지 가져오기
        getWriterProfileImage(destinationUid)
        // 댓글창에 사용할 현재 사용자의 프로필 이미지 가져오기
        getCurrentUserProfileImage()

        // 댓글 리사이클러뷰 세팅  / 2021.02.11
        comment_recyclerview.adapter = CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        // 수정 삭제 버튼 / 2021.02.15
        // 게시글 작성자와 현재 사용자의 uid 가 같을 때만 보이도록 처리
        if (currentUserUid != destinationUid) {
            article_menu_button.visibility = View.GONE
        }

        // 수정 버튼 이벤트 추가 / 2021.02.15
        article_menu_button.setOnClickListener {
            // pop up 메뉴 생성
            showPopup(article_menu_button)
        }

       // 좋아요 버튼 클릭 리스너
        detailviewitem_favorite_imageview.setOnClickListener {
            Log.d(TAG, "좋아요 클릭 됨")
            // 좋아요 기능 메소드
            favoriteEvent(contentUid)
        }
//        // 좋아요 count 와 빈 하트가 색칠 되도록 처리
//        if(item?.favorites != null ){
//            // 현재 사용자의 uid 가 좋아요 누른 사람 목록에 포함되어 있다면
//            if(item!!.favorites.containsKey(currentUserUid)) {
//                // 좋아요 버튼 클릭한 경우
//                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_orange)
//            } else {
//            // 포함되어 있지 않다면
//                // 좋아요 버튼 클릭하지 않은 경우
//                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_gray)
//            }
//        }

        // 댓글 추가 버튼 클릭 이벤트 / 2021.02.11
        comment_btn_send?.setOnClickListener {
            // 현재 시간 얻기
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val formatted = current.format(formatter)
            // Content DTO 의 comment 클래스 생성
            // 유저의 정보, 댓글 내용을 매개변수로 담는다.
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = comment_edit_message.text.toString()
            comment.uploadTime = formatted
            comment.timestamp = System.currentTimeMillis()
            // 가공한 데이터를 Firestore 에 보낸다.
            // 저장할 위치는 "images" 라는 collection
            // images 라는 collection 내부에 폴더(contentUid)를 만든다
            // 그 폴더 안에 Comments 라는 collection 을 생성한다.
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .document()
                .set(comment)
            Log.d("댓글", "개수 추가")
            // comment count 업데이트
            val tsDoc = FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
            FirebaseFirestore.getInstance().runTransaction {transaction ->
                val contentDTO = transaction.get(tsDoc).toObject(ContentDTO::class.java)
                // 댓글이 아예 없을 경우
                contentDTO?.commentCount = contentDTO?.commentCount?.plus(1)!!
                transaction.set(tsDoc,contentDTO)
                Log.d("댓글", contentDTO.commentCount.toString())
                // view 에 댓글 개수 업데이트
                detailviewitem_commentcounter_textview.text = contentDTO.commentCount.toString()
            }
            // 댓글 알림 이벤트 메소드
            commentAlarm(destinationUid!!, comment_edit_message.text.toString())
            // 댓글 업로드 완료하고 edit text 초기화
            comment_edit_message.setText("")
        }
    }
    // Pop up 메뉴 생성 메소드 / 2021.02.15
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    fun showPopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.article_popup_menu)

        popup.setOnMenuItemClickListener (PopupMenu.OnMenuItemClickListener{ item: MenuItem? ->

            when (item!!.itemId) {
                R.id.modify -> {
                    Log.d(TAG, "수정 버튼 clicked")

                    // 수정 dialog 생성
                    val builder = AlertDialog.Builder(this)
                    dialogView = LayoutInflater.from(this).inflate(R.layout.activity_add_photo, null)
                    val dialogTextView = view.addphoto_edit_explain
                    // dialog view 에 데이터 binding
                    dialogView?.btn_upload?.visibility = View.GONE
                    dialogView?.addphoto_edit_explain?.setText(explain)
                    // 게시글 Image
                    dialogView?.addphoto_image?.let {
                        Glide.with(this)
                            .load(imageUrl)
                            .into(it)
                    }

                    // 이미지 클릭 이벤트
                    dialogView?.addphoto_image?.setOnClickListener {
                        val photoPickerIntent = Intent(Intent.ACTION_PICK)
                        photoPickerIntent.type = "image/*"
                        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
                    }

                    builder.setView(dialogView)
                        .setPositiveButton("수정") {
                            dialogInterface, i ->
                            // 사용자가 수정한 게시글 내용이 들어있는 변수
                            val editedExplain = dialogView?.addphoto_edit_explain?.text.toString()
                            // 이미지를 변경했냐 안했냐 구분 해야됨
                            if (photoUri != null) {
                                // A. 이미지를 변경했을 때

                                // 현재 시간 얻기
                                val current = LocalDateTime.now()
                                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                val formatted = current.format(formatter)
                                // 새로운 파일 이름 생성
                                var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                                var imageFileName = "IMAGE_" + timestamp + "_.png"

                                // Firebase Storage 에 저장할 위치 지정
                                var storageRef = storage?.reference?.child("images")?.child(imageFileName)

                                // 새로운 이미지 Fire Storage 에 저장 명령
                                storageRef?.putFile(photoUri!!)?.continueWithTask {
                                    task: Task<UploadTask.TaskSnapshot> ->
                                    return@continueWithTask storageRef.downloadUrl
                                }?.addOnSuccessListener {
                                    uri ->
                                    firestore?.collection("images")?.document(contentUid!!)?.update("explain", editedExplain, "imageUrl", uri.toString())
//                                    firestore?.collection("images")?.document(contentUid!!)?.update("imageUrl", photoUri)
                                    Log.d(TAG, "이미지 + 내용 수정 완료")
                                    // 수정된 데이터로 activity 의 view binding
                                    setDataFromServer()
                                }

                            } else {
                                // B. 이미지를 변경하지 않았을 때 수정 코드
                                // 서버 통신으로 db 업데이트 하기
                                val content = contentUid?.let { firestore?.collection("images")?.document(it) }
                                content?.update("explain", editedExplain)
                                    ?.addOnCompleteListener {
                                        if(it.isSuccessful) {
                                            Log.d("수정 activity", "수정 성공")
                                            setDataFromServer()
                                            popup.dismiss()
                                        } else {
                                            Log.d("수정 activity", "수정 실패")
                                        }
                                    }
                            }
                        }
                        .setNegativeButton("취소") { dialogInterface, i->
                            Log.d(TAG, "다이얼로그 취소 버튼 clicked")
                        }
                        .show()

                }
                R.id.delete -> {
                    Log.d(TAG, "삭제 버튼 clicked")
                    // 삭제 dialog 생성
                }
            }
            true
        })
        popup.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGE_FROM_ALBUM) {
            if(resultCode == Activity.RESULT_OK) {
                // this is path to the selected image
                photoUri = data?.data
                dialogView?.addphoto_image?.setImageURI(photoUri)

            } else {
                // 사진선택 취소 했을 때
                finish();
            }
        }
    }
    fun contentUpload() {
        // 현재 시간 얻기
//        val current = LocalDateTime.now()
//        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
//        val formatted = current.format(formatter)
////        println("Current: $formatted")
//        Log.d("시간", formatted)
//        // make filename
//        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        var imageFileName = "IMAGE_" + timestamp + "_.png"
//        // 업로드할 이미지 name 선언
//        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
//
//        // 수정하려면
//        // content uid 를 사용해서 폴더 접근
//        // 내용물 set을 바꾼다.
//        // 게시글 정보 서버로부터 받아오기
//        var map = mutableMapOf<String, Any>()
//        map["explain"] = addphoto_edit_explain.text
        val editedExplain = addphoto_edit_explain.text.toString()
        val content = contentUid?.let { firestore?.collection("images")?.document(it) }
        content?.update("explain", editedExplain)
            ?.addOnCompleteListener {
                if(it.isSuccessful) {
                    Log.d("수정 activity", "수정 성공")
                } else {
                    Log.d("수정 activity", "수정 실패")
                }
            }
        val intent = Intent(this, CommentActivity::class.java)
        // 게시글 번호
        intent.putExtra("contentUid", contentUid)
        // 게시글 작성자 uid
        intent.putExtra("destinationUid", destinationUid)
        startActivity(intent)

//        // 1. promise
//        storageRef?.putFile(photoUri!!)?.continueWithTask {
//                task: Task<UploadTask.TaskSnapshot> ->
//            return@continueWithTask storageRef.downloadUrl
//        }?.addOnSuccessListener {
//                uri ->
//            var contentDTO = ContentDTO()
//            // Insert downloadUrl of Image
//            contentDTO.imageUrl = uri.toString()
//            // Insert uid of user
//            contentDTO.uid = auth?.currentUser?.uid
//            contentDTO.userId = auth?.currentUser?.email
//            contentDTO.explain = addphoto_edit_explain.text.toString()
//            contentDTO.uploadTime = formatted
//            contentDTO.timestamp = System.currentTimeMillis()
//            // 데이터를 모아서 firestore 에 추가
//            firestore?.collection("images")?.document()?.set(contentDTO)
//            // 정상적으로 종료한다는 flag
//            setResult(Activity.RESULT_OK)
//            finish()
//        }
    }

    // 작성자의 프로필 이미지 가져오는 메소드 / 2021.02.14
    fun getWriterProfileImage (writerUid : String?) {
        // collection 은 firestore 의 세부 폴더 개념인 것 같음.
        firestore?.collection("profileImages")?.document(writerUid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            // 실시간으로 체크하기 위해서 snapshot을 쓴다?
            // snapshot 이 null 이면 전단계로 빠져나오는 return?
            if(documentSnapshot == null) return@addSnapshotListener
            // snapshot 에 url 데이터가 들어 있다면
            if(documentSnapshot.data != null) {
                // url 변수에 넣어서 glide 로 사진 출력
                var url = documentSnapshot?.data!!["image"]
                Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(detailviewitem_profile_image)
            }
        }
    }

    // 서버에서 받아온 데이터를 view 에 뿌리는 함수 / 2021.02.14
    fun setDataFromServer () {
//        // 게시글 작성자 id 세팅
//        detailviewitem_profile_textview.text = writer
//        // 게시글 이미지 세팅
//        Glide.with(this)
//            .load(imageUrl)
//            .into(detailviewitem_imageview_content)
//        // 게시글 내용
//        detailviewitem_explain_textview.text = explain
//        // 좋아요 개수
//        detailviewitem_favoritecounter_textview.text = likeCount.toString()
//        // 댓글 개수
//        detailviewitem_commentcounter_textview.text = commentCount.toString()

        // 게시글 정보 서버로부터 받아오기
        val content = contentUid?.let { firestore?.collection("images")?.document(it) }
        content?.get()
            ?.addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${documentSnapshot}")
                    // 데이터 가져온 것을 view 에 뿌려주면 끝
                    // 받아온 데이터를 contentDTO 에 넣는다. 어떻게?
                    item = documentSnapshot.toObject(ContentDTO::class.java)

                    // 게시글 작성자 id 세팅
                    writer = item?.userId
                    detailviewitem_profile_textview.text = writer

                    // 게시글 이미지 세팅
                    imageUrl = item?.imageUrl
                    Glide.with(this)
                        .load(imageUrl)
                        .into(detailviewitem_imageview_content)

                    // 게시글 내용
                    explain = item?.explain
                    detailviewitem_explain_textview.text = explain

                    // 좋아요 개수
                    likeCount = item?.favoriteCount
                    detailviewitem_favoritecounter_textview.text = likeCount.toString()

                    // 댓글 개수
                    commentCount = item?.commentCount
                    detailviewitem_commentcounter_textview.text = commentCount.toString()

                    // 좋아요 count 와 빈 하트가 색칠 되도록 처리
                    if(item?.favorites != null ){
                        // 현재 사용자의 uid 가 좋아요 누른 사람 목록에 포함되어 있다면
                        if(item!!.favorites.containsKey(currentUserUid)) {
                            // 좋아요 버튼 클릭한 경우
                            detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_orange)
                        } else {
                            // 포함되어 있지 않다면
                            // 좋아요 버튼 클릭하지 않은 경우
                            detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_gray)
                        }
                    }

                } else {
                    Log.d(TAG, "No such document")
                }
            }
            ?.addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }
    // 좋아요 이벤트 메소드 / 2021.02.14
    fun favoriteEvent(contentIndex: String?) {
        // content idx 를 key 값으로 사용하여 db의 조회할 데이터 위치를
        // Document Reference 형식의 변수 tsDoc 에 담는다.
        val tsDoc = contentIndex?.let { firestore?.collection("images")?.document(it) }
        // 담은 db 주소를 사용하여 db 조회 요청
        firestore?.runTransaction { transaction ->
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
            val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

            // 좋아요가 이미 클릭되어 있는 경우, 아닌 경우
            if(contentDTO!!.favorites.containsKey(currentUserUid)){
                // 좋아요 개수 변경
                contentDTO?.favoriteCount = contentDTO?.favoriteCount -1
                // 좋아요 누른사람 정보에서 현재 사용자의 uid 를 제거
                contentDTO?.favorites.remove(currentUserUid)

                // view 업데이트
                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_gray)
                detailviewitem_favoritecounter_textview.text = contentDTO?.favoriteCount.toString()

            } else {
                Log.d("좋아요 로그", "0 에서 클릭됨")
                // 눌려있지 않다
                contentDTO?.favoriteCount = contentDTO?.favoriteCount +1
                contentDTO?.favorites[currentUserUid!!] = true
                destinationUid?.let { favoriteAlarm(it) }

                // view 업데이트
                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_orange)
                detailviewitem_favoritecounter_textview.text = contentDTO.favoriteCount.toString()
            }
            transaction.set(tsDoc, contentDTO)
            Log.d("좋아요 서버 업로드", "완료")
            Log.d("좋아요 서버 업로드", contentDTO.toString())

//            // 좋아요가 이미 클릭되어 있는 경우, 아닌 경우
//            if(item!!.favorites.containsKey(currentUserUid)){
//                // 좋아요 개수 변경
//                item?.favoriteCount = item?.favoriteCount?.minus(1)!!
//                // 좋아요 누른사람 정보에서 현재 사용자의 uid 를 제거
//                item?.favorites?.remove(currentUserUid)
//                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_gray)
//                detailviewitem_favoritecounter_textview.text = item?.favoriteCount.toString()
//            } else {
//                // 눌려있지 않다
//                item?.favoriteCount = item?.favoriteCount?.plus(1)!!
//                item?.favorites?.set(currentUserUid!!, true)
//                destinationUid?.let { favoriteAlarm(it) }
//                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_orange)
//                detailviewitem_favoritecounter_textview.text = item?.favoriteCount.toString()
//            }
//            // 수정된 좋아요 정보를 업로드 한다.
//            item?.let { transaction.set(tsDoc, it) }

        }
    }
    // 좋아요 버튼 이벤트 메소드 / 2021.02.12
    fun favoriteAlarm(destinationUid: String) {
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 0
        alarmDTO.timestamp = System.currentTimeMillis()
        // alarmDTO 에 담은 데이터를 Firestore (db) 에 저장
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
        FcmPush.instance.sendMessage(destinationUid, "Shinstagram", message)
    }
    // 서버 저장소에 있는 프로필 이미지를 view에 뿌려주는 메소드 / 2021.02.11
    fun getCurrentUserProfileImage() {
        // collection 은 firestore 의 세부 폴더 개념인 것 같음.
        firestore?.collection("profileImages")?.document(currentUserUid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            // 실시간으로 체크하기 위해서 snapshot을 쓴다?
            // snapshot 이 null 이면 전단계로 빠져나오는 return?
            if(documentSnapshot == null) return@addSnapshotListener
            // snapshot 에 url 데이터가 들어 있다면
            if(documentSnapshot.data != null) {
                // url 변수에 넣어서 glide 로 사진 출력
                var url = documentSnapshot?.data!!["image"]
                Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(detailviewitem_profile_image_comment)
            }
        }
    }
    // 댓글 알림 메소드 / 2021.02.12
    fun commentAlarm(destinationUid: String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.kind = 1
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        // alarmDTO 를 db에 저장
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        // 댓글 알림 보내기
        var msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid, "Shinstagram", msg)
    }

    // 저장된 댓글들을 불러오는 리사이클러뷰 adapter / 2021.02.11
    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot == null)return@addSnapshotListener

                    for(snapshot in querySnapshot.documents!!) {
                        comments.add(0, snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
            return CustomViewHolder(view)
        }

        override fun getItemCount(): Int {
            return comments.size
        }

        private inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.commentviewitem_textview_comment.text = comments[position].comment
            view.commentviewitem_textview_profile.text = comments[position].userId
            if (comments[position].uploadTime != null) {
                val convertedDate : String? = TimeConverter.CreateDataWithCheck(comments[position].uploadTime)
                view.commentviewitem_textview_regtime.text = convertedDate
            }
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        var url = task.result!!["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(view.commentviewitem_imageview_profile)

                    }
                }

        }
    }
}