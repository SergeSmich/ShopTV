package com.shoptv.app.presentation.store

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.shoptv.app.R

class StoreSelectionActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_selection)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.store_selection_fragment, StoreSelectionFragment())
                .commit()
        }
    }
}
