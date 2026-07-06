package com.example.shoptv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Для Leanback (Android TV) лучше наследоваться от FragmentActivity
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}


