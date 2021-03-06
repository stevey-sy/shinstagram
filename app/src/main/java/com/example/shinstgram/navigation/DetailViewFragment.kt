package com.example.shinstgram.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.AlarmDTO
import com.example.shinstgram.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_detail.view.*
import java.text.SimpleDateFormat

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var uid : String? = null
    var profileUrl : String? = null
    var timeConverter : TimeConverter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view 세팅
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    // 외부 클래스를 항상 참조하는 inner class
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        // uid 리스트
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            // DB 에 접근하여 데이터를 가져오는 쿼리문
            firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    if(querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        // 데이터 역순으로 가져오기
                        contentDTOs.add(0, item!!)
                        contentUidList.add(0, snapshot.id)
                        Log.d("콘텐트 UID", snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_detail,
                parent,
                false
            )
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        // 서버에서 넘어온 데이터들을 mapping 시키는 부분
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var viewholder = (holder as CustomViewHolder).itemView

            //User Id
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![position].userId

            // Image uri
            Glide.with(holder.itemView.context)
                .load(contentDTOs!![position].imageUrl)
                .into(viewholder.detailviewitem_imageview_content)

            // explain mapping
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![position].explain

            // likes
            viewholder.detailviewitem_favoritecounter_textview.text =
                contentDTOs!![position].favoriteCount.toString()

            // comment count
            viewholder.detailviewitem_commentcounter_textview.text =
                contentDTOs[position].commentCount?.toString()

            // upload time
            // 불순물 제거
//            val replace_dash: String? = contentDTOs[position].uploadTime?.replace("-", "")
//            val replace_time = replace_dash?.replace(":", "")
//            val pure_date = replace_time?.replace(" ", "")
            if (contentDTOs[position].uploadTime != null) {
                val convertedDate : String? = TimeConverter.CreateDataWithCheck(contentDTOs[position].uploadTime)
                viewholder.detailviewitem_uploadtime_textview.text = convertedDate
            }
            // profile image / 2021.02.13 수정
            // 글 작성자의 uid 를 가져와야 됨.
            firestore?.collection("profileImages")?.document(contentDTOs!![position].uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                // 실시간으로 체크하기 위해서 snapshot을 쓴다?
                // snapshot 이 null 이면 전단계로 빠져나오는 return?
                if(documentSnapshot == null) return@addSnapshotListener
                // snapshot 에 url 데이터가 들어 있다면
                if(documentSnapshot.data != null) {
                    // url 변수에 넣어서 glide 로 사진 출력
                    profileUrl = documentSnapshot?.data!!["image"] as String?
                    Glide.with(holder.itemView.context)
                        .load(profileUrl)
                        .apply(RequestOptions().circleCrop())
                        .into(viewholder.detailviewitem_profile_image)
                }
            }

            // 좋아요 버튼 클릭 이벤트
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }
            // 좋아요 count 와 빈 하트가 색칠 되도록 처리
            // 내 uid 가 좋아요 누른 사람 목록에 포함되어 있다면
            if(contentDTOs!![position].favorites.containsKey(uid)) {
                // 좋아요 버튼 클릭한 경우
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_orange)

            } else {
                // 좋아요 버튼 클릭하지 않은 경우
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_like_gray)
            }
            // 상대가 유저 정보로 오는 코드
            viewholder.detailviewitem_profile_image.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(
                    R.id.main_content,
                    fragment
                )?.commit()
            }
            // 댓글 이미지 버튼 클릭 이벤트
            viewholder.detailviewitem_comment_imageview.setOnClickListener { v ->
                val intent = Intent(v.context, CommentActivity::class.java)
                // 게시글 번호
                intent.putExtra("contentUid", contentUidList[position])
                // 게시글 작성자 uid
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        fun favoriteEvent(position: Int) {
            //
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
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
                    favoriteAlarm(contentDTOs[position].uid!!)
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
    }
}

