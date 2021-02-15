package com.example.shinstgram.navigation

import android.annotation.SuppressLint
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AlarmFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view 세팅
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm,container, false)
        view.alarmfragment_recyclerview.adapter = AlarmRecyclerViewAdapter()
        view.alarmfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }
    // 알람 리사이클러뷰 어댑터 / 2021.02.12
    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // 리사이클러 뷰에 넣을 데이터 형식
        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()

        // 어댑터가 생성되면서 곧 바로 실행되는 함수
       init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            // 내 아이디와 작성자의 아이디가 같을 때
            FirebaseFirestore.getInstance().collection("alarms")
                .whereEqualTo("destinationUid", uid)
//                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    alarmDTOList.clear()
                    if(querySnapshot == null) return@addSnapshotListener
                    // snapshot 에 담긴 데이터를 하나씩 나열한다
                    for (snapshot in querySnapshot.documents) {
                        alarmDTOList.add(0, snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            // 아이템 디자인 정의
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
//            view.commentviewitem_textview_regtime.visibility = View.GONE
            // custom view holder 에서 view 를 가공한다.
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView

            // 알림 누른 사람의 프로필 이미지를 가져온다다
           FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener{
                task ->
                if(task.isSuccessful) {
                    val url = task.result!!["image"]
                    Glide.with(view.context)
                        .load(url)
                        .apply(RequestOptions().circleCrop())
                        .into(view.commentviewitem_imageview_profile)
                }
           }

            val format= SimpleDateFormat("yyyyMMddHHmmss")
            val formatTime : String? = format.format(alarmDTOList[position].timestamp)
            val convertedDate : String? = TimeConverter.CreateDataWithCheck(formatTime)
            view.commentviewitem_textview_regtime.text = convertedDate


            when (alarmDTOList[position].kind) {
                // 좋아요 알람 경우
                0 -> {
                    val str = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    view.commentviewitem_textview_comment.text = str
                }
                // 댓글 알람
                1 -> {
                    val str = alarmDTOList[position].userId + " " + getString(R.string.alarm_comment) + " \n\" " + alarmDTOList[position].message +"\""
                    view.commentviewitem_textview_comment.text = str
                }
                // follow 알람
                2 -> {
                    val str = alarmDTOList[position].userId + getString(R.string.alarm_follow)
                    view.commentviewitem_textview_comment.text = str
                }
            }
            view.commentviewitem_textview_profile.visibility = View.GONE
//            view.commentviewitem_textview_comment.visibility = View.INVISIBLE
        }

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        @SuppressLint("SimpleDateFormat")
        private fun getDateTime(longDate: Long?): String {
            try {
                val sdf = SimpleDateFormat("yyyy/MM/dd")
                val netDate = longDate?.times(1000)?.let { Date(it) }
                return sdf.format(netDate)
            } catch (e: Exception) {
                return e.toString()
            }
        }

    }

}