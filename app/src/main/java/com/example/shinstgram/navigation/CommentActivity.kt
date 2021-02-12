package com.example.shinstgram.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.AlarmDTO
import com.example.shinstgram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentActivity : AppCompatActivity() {
    var contentUid : String? = null
    var destinationUid : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        // 댓글 작성자 정보
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        // 리사이클러뷰 세팅  / 2021.02.11
        comment_recyclerview.adapter = CommentRecyclerviewAdapter()
        comment_recyclerview.layoutManager = LinearLayoutManager(this)

        // 댓글 추가 버튼 클릭 이벤트 / 2021.02.11
        comment_btn_send?.setOnClickListener {
            // Content DTO 의 comment 클래스 생성
            // 유저의 정보, 댓글 내용을 매개변수로 담는다.
            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = comment_edit_message.text.toString()
            comment.timestamp = System.currentTimeMillis()
            // 가공한 데이터를 Firestore 에 보낸다.
            // 저장할 위치는 "images" 라는 collection
            // images 라는 collection 내부에 폴더(contentUid)를 만든다
            // 그 폴더 안에 Comments 라는 collection을 생성한다.
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .document()
                .set(comment)
            // 댓글 알림 이벤트 메소드
            commentAlarm(destinationUid!!, comment_edit_message.text.toString())
            // 댓글 업로드 완료하고 edit text 초기화
            comment_edit_message.setText("")
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
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()
                    if(querySnapshot == null)return@addSnapshotListener

                    for(snapshot in querySnapshot.documents!!) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
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