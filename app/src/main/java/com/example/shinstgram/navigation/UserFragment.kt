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
import com.example.shinstgram.navigation.model.AlarmDTO
import com.example.shinstgram.navigation.model.ContentDTO
import com.example.shinstgram.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment(), RecyclerViewInterface {
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    var followingListenerRegistration: ListenerRegistration? = null
    var followListenerRegistration: ListenerRegistration? = null
    // static 과 비슷한 역할
    companion object {
        var PICKER_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view 세팅
//        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user,container, false)
        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)
        // 이전 view 에서 받아온 데이터 활용
        uid = arguments?.getString("destinationUid")
        Log.d("유아이디2", uid.toString())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid
        if(uid == currentUserUid) {
            Log.d("UID 비교", "current: $currentUserUid uid:$uid")
            //mypage
//            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.text = "로그아웃"
            Log.d("버튼3", fragmentView!!.account_btn_follow_signout?.text as String)
            Log.d("버튼1", fragmentView!!.account_btn_follow_signout.toString())
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                Log.d("버튼2", fragmentView!!.account_btn_follow_signout.toString())
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
//            mainactivity?.toolbar_title_image?.visibility = View.GONE
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.toolbar_btn_back?.visibility = View.VISIBLE
            // follow 기능 메소드 2021.02.11
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }
        // 어뎁터 세팅
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter(this)
        // 레이아웃 세팅
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3)
        // 프로필 버튼 클릭 이벤트 / 2021.02.11
        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICKER_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
//        getFollowerAndFollowing()
        getFollowing()
        getFollower()
        return fragmentView
    }
    // 리사이클러뷰 아이템 클릭 이벤트
    override fun onItemClicked(contentUid : String?, writerUid : String?) {
        Log.d("로그", "User Fragment - onItemClicked() called")
        // 클릭된 게시글의 detail fragment 로 이동
        // 필요한 것, 게시글의 index 번호, 게시글 등록자의 uid
        val intent = Intent(context, CommentActivity::class.java)
        // 게시글 번호
        intent.putExtra("contentUid", contentUid)
        // 게시글 작성자 uid
        intent.putExtra("destinationUid", writerUid)
        startActivity(intent)
    }
    // follwers = 나를 follow 하고 있는 사람
    // followings = 내가 follow 하고 있는 사람
    // follow, follow 취소 버튼 활성화 메소드 / 2021.02.11

    fun getFollowing() {
        followingListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, frebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView!!.account_tv_following_count.text = followDTO?.followingCount.toString()
        }
    }

    fun getFollower() {

        followListenerRegistration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            val followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            if (followDTO == null) return@addSnapshotListener
            fragmentView?.account_tv_follow_count?.text = followDTO?.followerCount.toString()
            if (followDTO?.followers?.containsKey(currentUserUid)!!) {

//                fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                fragmentView?.account_btn_follow_signout?.setText(R.string.follow_cancel)
//                fragmentView?.account_btn_follow_signout
//                    ?.background
//                    ?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorBlack), PorterDuff.Mode.MULTIPLY)
            } else {

                if (uid != currentUserUid) {

//                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    fragmentView?.account_btn_follow_signout?.setText(R.string.follow)
//                    fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                }
            }
        }

    }

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
                        ContextCompat.getColor(activity!!, R.color.colorBlack), PorterDuff.Mode.MULTIPLY)
                } else {
//                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    if(uid != currentUserUid) {
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
//                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }
    // follow 클릭했을 때의 메소드 2021.02.11
    // follower = 누군가가 -> 나를 follower
    // following = 내가 -> 누군가를 following
    fun requestFollow() {
        // 현재 app 사용자의 user account 접근
        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
        firestore?.runTransaction { transaction ->

            // following = 내가 -> 누군가를 following 하는 경우
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if(followDTO == null) {
                // follow 정보를 저장할 dto 생성
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                // 내가 follow 할 사람의 uid 를 following 리스트에 추가
                followDTO.followings[uid!!] = true
                // 데이터 업로드 명령
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            // 내가 누군가를 following 하는 것을 취소
            // 만약, followings 리스트에 내가 선택한 uid 가 이미 존재한다면,
            if(followDTO?.followings?.containsKey(uid)!!) {
                followDTO?.followingCount = followDTO?.followingCount -1
                // follow 리스트에서 uid 제거
                followDTO?.followings.remove(uid)
            // 만약, following 리스트에 내가 선택한 user 가 없다면
            }else {
                // following 시작
                followDTO?.followingCount = followDTO?.followingCount +1
                // follow 리스트에 uid 추가
                followDTO!!.followings[uid!!] = true
                followAlarm(uid!!)
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // 내가 following 누른 사용자의 입장
        // following 유저의 db 접근
        var tsDocFollower = firestore!!.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            // 받아온 데이터를 followDTO type 으로 가공한다.
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            // 해당 유저의 follower 데이터가 존재하지 않을 경우
            if(followDTO == null) {
                followDTO = FollowDTO()
                // following 당한 유저의 follower count 를 1 추가한다.
                followDTO!!.followerCount = 1
                // following 당한 유저의 follower 리스트에 나를 추가
                followDTO!!.followers[currentUserUid!!] = true
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            // following 받은 유저의 follower 리스트에 이미 존재한다면,
            // follower 를 취소하겠다는 의미
            if(followDTO!!.followers?.containsKey(currentUserUid!!)!!) {
                // follower 취소
                followDTO!!.followerCount = followDTO!!.followerCount -1
//                followDTO!!.followerCount -= 1
                // follow 리스트에서 uid 제거
                followDTO!!.followers.remove(currentUserUid)
            } else {
                // follow 시작할 때
                followDTO!!.followerCount += 1
//                followDTO!!.followerCount = followDTO!!.followerCount +1
                followDTO!!.followers[currentUserUid!!] = true
                // follow 알림 메소드
            }
            // db 종료
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }

    }
    // follow 알림 메소드 / 2021.02.12
    fun followAlarm(destinationUid : String) {
        var alarmDTO = AlarmDTO()
        // 알림을 받을 사용자의 uid
        alarmDTO.destinationUid = destinationUid
        Log.d("*** destinationUid", destinationUid)
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.timestamp = System.currentTimeMillis()
        // 현재 app 사용중인 유저
        alarmDTO.uid = auth?.currentUser?.uid
        Log.d("*** uid", auth?.currentUser?.uid.toString())
        alarmDTO.kind = 2
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        // follow 버튼이 눌리면 push 메세지를 발신
        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "Shinstagram", message)

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
    inner class UserFragmentRecyclerViewAdapter (recyclerViewInterface: RecyclerViewInterface) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var recyclerViewInterface : RecyclerViewInterface? = null
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        // 생성자
        init {
            firestore?.collection ("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentUidList.clear()
                // 스냅샷이 null 이면 종료
                if(querySnapshot == null) return@addSnapshotListener

                // Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(0, snapshot.toObject(ContentDTO::class.java)!!)
                    contentUidList.add(0, snapshot.id)
                }
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
                this.recyclerViewInterface = recyclerViewInterface
            }
        }
        // 뷰 홀더가 생성 되었을 때
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels /3

            val imageview = ImageView(parent.context);
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageview, this.recyclerViewInterface!!)
        }

        inner class CustomViewHolder(var imageview: ImageView, recyclerViewInterface: RecyclerViewInterface)
                                        : RecyclerView.ViewHolder(imageview), View.OnClickListener
        {
            var recyclerViewInterface : RecyclerViewInterface? = null

            init {
                Log.d("로그", "CustomViewHolder - init() called")
                itemView.setOnClickListener(this)
                this.recyclerViewInterface = recyclerViewInterface
            }

            override fun onClick(v: View?) {
                Log.d("로그", "Custom View Holder - onClick() called")
                Log.d("로그", "contentUid: ${contentUidList[adapterPosition]}")
                val contentUid : String? = contentUidList[adapterPosition]
                val writerUid : String? = contentDTOs[adapterPosition].uid
                this.recyclerViewInterface?.onItemClicked(contentUid, writerUid)

                // 어떻게 그 글을 가져올 수 있을까
                // content uid 를 user fragment 에 넘겨야한다.
                // 현재 리사이클러뷰의 포지션에서 content uid 를 가져와야한다.


            }
        }
        // 뷰 홀더가 묶였을 때 (데이터와 view 를 묶는다)
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