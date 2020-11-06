package ru.kiryanov.circleview.presentation.acticity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_custom_view.*
import ru.kiryanov.circleview.R
import ru.kiryanov.circleview.presentation.ui.SectorInfo

class CustomViewFragment : Fragment() {
    companion object {
        const val ID = "custom_view_fragment"
        fun newInstance() = CustomViewFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.activity_custom_view, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInfoBtn.setOnClickListener {
            circle.setSectorsInfo(initCircleView())
        }
    }


    private fun initCircleView() =
        mutableListOf<SectorInfo>()
            .apply {
                add(
                    SectorInfo(
                        android.R.color.holo_blue_dark,
                        android.R.color.darker_gray,
                        drawableId = R.drawable.ic_baseline_account_circle_1
                    )
                )

                add(
                    SectorInfo(
                        android.R.color.darker_gray,
                        android.R.color.holo_blue_dark,
                        drawableId = R.drawable.ic_baseline_account_circle_2
                    )
                )

                add(
                    SectorInfo(
                        android.R.color.holo_blue_light,
                        android.R.color.holo_red_dark,
                        drawableId = R.drawable.ic_baseline_account_circle_3
                    )
                )

                add(
                    SectorInfo(
                        android.R.color.holo_red_dark,
                        drawableId = R.drawable.ic_baseline_account_circle_4
                    )
                )

                add(
                    SectorInfo(
                        android.R.color.holo_green_light,
                        android.R.color.darker_gray,
                        drawableId = R.drawable.ic_baseline_account_circle_5
                    )
                )

                add(
                    SectorInfo(
                        android.R.color.holo_red_light,
                        android.R.color.holo_orange_light,
                        drawableId = R.drawable.ic_baseline_account_circle_6
                    )
                )
                add(
                    SectorInfo(
                        android.R.color.holo_orange_light,
                        drawableId = R.drawable.ic_baseline_account_circle_3
                    )
                )
                add(
                    SectorInfo(
                        android.R.color.holo_blue_bright,
                        android.R.color.darker_gray,
                        drawableId = R.drawable.ic_baseline_account_circle_2
                    )
                )
                add(
                    SectorInfo(
                        android.R.color.holo_green_light,
                        android.R.color.darker_gray,
                        drawableId = R.drawable.ic_baseline_account_circle_1
                    )
                )
                add(
                    SectorInfo(
                        android.R.color.holo_red_light,
                        android.R.color.holo_blue_bright,
                        drawableId = R.drawable.ic_baseline_account_circle_5
                    )
                )
            }
}