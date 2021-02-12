package com.example.shinstgram.navigation.model

data class AlarmDTO (
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,
    // kind = 알람 구분 flag 값
    // like = 0
    // comment = 1
    // follow = 2
    var kind : Int? = null,
    var message : String? = null,
    var timestamp : Long? = null
)