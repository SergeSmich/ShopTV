package com.shoptv.app.presentation.cart

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.shoptv.app.R

class CartActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.cart_fragment, CartFragment.newInstance())
                .commit()
        }
    }
}
