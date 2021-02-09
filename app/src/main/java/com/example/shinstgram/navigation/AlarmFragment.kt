package com.example.shinstgram.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.shinstgram.R

class AlarmFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view 세팅
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm,container, false)
        return view
    }
}