package com.example.shinstgram.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.circleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.shinstgram.LoginActivity
import com.example.shinstgram.MainActivity
import com.example.shinstgram.R
import com.example.shinstgram.navigation.model.ContentDTO
import com.example.shinstgram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    // static 과 비슷한 역할
    companion object {
        var PICKER_PROFILE_FROM_ALBUM = 10
    }
    var imageprofileListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view 세팅
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user,container, false)
        // 이전 view 에서 받아온 데이터 활용
        uid = arguments?.getString("destinationUid")
        uid?.let { Log.d("유아이디", it) }
        Log.d("유아이디2", uid.toString())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid
        if(uid == currentUserUid) {
            //mypage
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            // others page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")
            mainactivity?.toolbar_btn_back.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainactivity?.toolbar_title_image?.visibility = View.GONE
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility = View.VISIBLE
            // follow 기능 메소드 2021.02.11
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }
        // 어뎁터 세팅
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        // 레이아웃 세팅
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)
        // 프로필 버튼 클릭 이벤트 / 2021.02.11
        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICKER_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }
    // follow, follow 취소 버튼 활성화 메소드 / 2021.02.11
    fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            // firestore 에서 받아온 snapshot 을 follow DTO 객체로 가공
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null) {
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null) {
                fragmentView?.account_tv_follow_count?.text = followDTO?.followerCount?.toString()
                if(followDTO?.followers?.containsKey(currentUserUid!!)) {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(
                        ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                } else {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    if(uid != currentUserUid) {
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }

                }
            }
        }
    }
    // follow 클릭했을 때의 메소드 2021.02.11
    fun requestFollow() {
        // 다른 사람이 나를 follow 할 때의 경우
        // 나를 follow 한사람의 uid 정보를 db에 저장
        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            // 아직 아무도 follow 누르지 않았을 때의 경우
            if(followDTO == null) {
                // follow 정보를 저장할 dto 생성
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followers[uid!!] = true
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            // 해당 유저가 이미 follow 를 눌렀었던 경우 (follow 취소)
            if(followDTO?.followings?.containsKey(uid)!!) {
                // follow 취소
                followDTO?.followingCount = followDTO?.followingCount -1
//                followDTO?.followingCount -= 1
                // follow 리스트에서 uid 제거
                followDTO?.followers.remove(uid)
            }else {
                // follow 시작
                followDTO?.followingCount = followDTO?.followingCount +1
                // follow 리스트에 uid 추가
                followDTO!!.followers[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        // 내가 다른 사람을 follow 할 경우
        // firestore 에서 해당 유저의 follower 데이터를 가져온다
        var tsDocFollower = firestore!!.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            // 받아온 데이터를 followDTO type 으로 가공한다.
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            // 해당 유저의 follower 데이터가 존재하지 않을 경우
            if(followDTO == null) {
                // 새로 만든다
                var followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                // 나의 uid 를 상대방(내가 follower 한 유저) follower 데이터에 추가
                followDTO!!.followers[currentUserUid!!] = true
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            // 내가 상대방을 follower 취소, 시작 할 때.
            if(followDTO!!.followers?.containsKey(currentUserUid!!)!!) {
                // follower 취소
                followDTO!!.followerCount = followDTO!!.followerCount -1
//                followDTO!!.followerCount -= 1
                // follow 리스트에서 uid 제거
                followDTO!!.followers.remove(currentUserUid)
            } else {
                followDTO!!.followerCount += 1
//                followDTO!!.followerCount = followDTO!!.followerCount +1
                followDTO!!.followers[currentUserUid!!] = true
            }
            // db 종료
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }

    }

    // 서버 저장소에 있는 프로필 이미지를 view에 뿌려주는 메소드 / 2021.02.11
    fun getProfileImage() {
        // collection 은 firestore 의 세부 폴더 개념인 것 같음.
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            // 실시간으로 체크하기 위해서 snapshot을 쓴다?
            // snapshot 이 null 이면 전단계로 빠져나오는 return?
            if(documentSnapshot == null) return@addSnapshotListener
            // snapshot 에 url 데이터가 들어 있다면
            if(documentSnapshot.data != null) {
                // url 변수에 넣어서 glide 로 사진 출력
                var url = documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection ("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                // 스냅샷이 null 이면 종료
                if(querySnapshot == null) return@addSnapshotListener

                // Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
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
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}