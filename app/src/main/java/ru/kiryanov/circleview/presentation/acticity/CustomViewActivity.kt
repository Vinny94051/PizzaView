package ru.kiryanov.circleview.presentation.acticity

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_container.*
import kotlinx.android.synthetic.main.activity_custom_view.*
import ru.kiryanov.circleview.R
import ru.kiryanov.circleview.presentation.ui.SectorModel

class CustomViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)
        Log.e(javaClass.simpleName, "OnCreate")

        supportFragmentManager.beginTransaction()
            .add(
                R.id.fragmentContainer,
                CustomViewFragment.newInstance(),
                CustomViewFragment.ID
            ).commit()

    }
}
