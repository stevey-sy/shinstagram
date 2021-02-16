package com.example.shinstgram.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_grid.view.*

class GridFragment : Fragment() {
    var firestore : FirebaseFirestore? = null
    var fragmentView : View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view 세팅
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_grid,container, false)
        firestore = FirebaseFirestore.getInstance()
        fragmentView?.gridfragment_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.gridfragment_recyclerview?.layoutManager = GridLayoutManager(activity, 3)
        return fragmentView
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        // uid 리스트
        var contentUidList: ArrayList<String> = arrayListOf()
        init {
            firestore?.collection ("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                // 스냅샷이 null 이면 종료
                if(querySnapshot == null) return@addSnapshotListener
                // Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(0, snapshot.toObject(ContentDTO::class.java)!!)
                    contentUidList.add(0, snapshot.id)
                    contentUidList.add(0, snapshot.id)
                    Log.d("Grid Fragment / 콘텐트 UID", snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels /3

            val imageview = ImageView(parent.context);
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) : RecyclerView.ViewHolder(imageview) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageview)

            // 이미지 클릭하면 article 로 이동
            imageview.setOnClickListener { v ->
                // 필요한 것 = current user's uid
                //          =  content uid
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

    }
}