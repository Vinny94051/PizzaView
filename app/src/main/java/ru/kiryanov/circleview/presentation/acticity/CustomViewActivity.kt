package ru.kiryanov.circleview.presentation.acticity

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_custom_view.*
import ru.kiryanov.circleview.R

class CustomViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_view)
        root.setOnClickListener{
//            hhh.invalidate()
//            hhh.setOnClickListener {
//                Toast.makeText(this, "12", Toast.LENGTH_SHORT).show()
//            }
        }
    }

}