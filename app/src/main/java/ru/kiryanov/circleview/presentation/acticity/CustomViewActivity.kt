package ru.kiryanov.circleview.presentation.acticity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ru.kiryanov.circleview.R

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
