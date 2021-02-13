package com.example.shinstgram.navigation.model

// Data Transfer Object
data class ContentDTO (
                   var explain : String? = null,
                   var imageUrl : String? = null,
                   var uid : String? = null,
                   var userId : String? = null,
                   var timestamp : Long? = null,
                   var favoriteCount : Int = 0,
                   var commentCount : Int = 0,
                   var uploadTime : String? = null,
                   var favorites : MutableMap<String,Boolean> = HashMap()) {
    data class Comment(var uid : String? = null,
                       var userId : String? = null,
                       var comment : String? = null,
                       var timestamp : Long? = null)

}


