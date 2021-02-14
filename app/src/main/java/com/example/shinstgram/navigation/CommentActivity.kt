package com.example.shinstgram.navigation

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.bumptech.glide.request.RequestOptions
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.AlarmDTO
import com.example.shinstgram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import kotlinx.android.synthetic.main.item_detail.view.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommentActivity : AppCompatActivity() {

    var contentUid : String? = null
    var destinationUid : String? = null
    var imageUrl : String? = null
    var userId : String? = null
    var explain : String? = null
    var profileUrl : String? = null
    var firestore : FirebaseFirestore? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null

    companion object {
        val TAG : String = "comment Activity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        // firestore 초기화
        firestore = FirebaseFirestore.getInstance()

        // 현재 App 사용자 정보
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        // 댓글 작성자 정보
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        // 게시글 데이터 세팅 메소드
        setContent ()
        // 게시글 작성자 프로필 이미지 가져오기
        getWriterProfileImage(destinationUid)
        // 댓글창에 사용할 현재 사용자의 프로필 이미지 가져오기
        getCurrentUserProfileImage()

        // 리사이클러뷰 세팅  / 2021.02.11
        comment_recyclerview.adapter = CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

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

    // 서버에서 받아온 데이터를 view에 뿌리는 함수
    fun setContent () {
        // 게시글 정보 서버로부터 받아오기
        val content = contentUid?.let { firestore?.collection("images")?.document(it) }
        content?.get()
            ?.addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${documentSnapshot}")
                    // 데이터 가져온 것을 view 에 뿌려주면 끝
                    // 받아온 데이터를 contentDTO 에 넣는다. 어떻게?
                    val item = documentSnapshot.toObject(ContentDTO::class.java)

                    // 게시글 작성자 id 세팅
                    val writer = item?.userId
                    detailviewitem_profile_textview.text = writer

                    // 게시글 이미지 세팅
                    val imageUrl = item?.imageUrl
                    Glide.with(this)
                        .load(imageUrl)
                        .into(detailviewitem_imageview_content)

                    // 게시글 내용
                    val content = item?.explain
                    detailviewitem_explain_textview.text = content

                    // 좋아요 개수
                    val likeCount = item?.favoriteCount
                    detailviewitem_favoritecounter_textview.text = likeCount.toString()

                    // 댓글 개수
                    val commentCount = item?.commentCount
                    detailviewitem_commentcounter_textview.text = commentCount.toString()

                    // 좋아요 이벤트
                    detailviewitem_favorite_imageview.setOnClickListener {
                        Log.d(TAG, "좋아요 클릭 됨")
//                        favoriteEvent(position)
                    }


                } else {
                    Log.d(TAG, "No such document")
                }
            }
            ?.addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }
    // 좋아요 이벤트 메소드
    fun favoriteEvent(position: Int) {
        //
        val tsDoc = contentUid?.let { firestore?.collection("images")?.document(it) }
        firestore?.runTransaction { transaction ->
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

            // 좋아요가 이미 클릭되어 있는 경우, 아닌 경우
            if(contentDTO!!.favorites.containsKey(uid)){
                // 좋아요 개수 변경
                contentDTO?.favoriteCount = contentDTO?.favoriteCount -1
                // 좋아요 누른사람 정보에서 현재 사용자의 uid 를 제거
                contentDTO?.favorites.remove(uid)
            } else {
                // 눌려있지 않다
                contentDTO?.favoriteCount = contentDTO?.favoriteCount +1
                contentDTO?.favorites[uid!!] = true
//                favoriteAlarm(contentDTOs[position].uid!!)
            }
            // 수정된 좋아요 정보를 업로드 한다.
            transaction.set(tsDoc, contentDTO)

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