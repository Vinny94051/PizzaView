package ru.kiryanov.circleview.presentation.acticity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_custom_view.*
import ru.kiryanov.circleview.R
import ru.kiryanov.circleview.presentation.ui.CircleView
import ru.kiryanov.circleview.presentation.ui.SectorModel

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
        circle.sectorsInfo = initCircleView()
    }


    private fun initCircleView() =
        mutableListOf<SectorModel.Data>()
            .apply {
                add(
                    SectorModel.Data(
                        android.R.color.holo_blue_dark,
                        R.drawable.ic_baseline_account_circle_1
                    )
                )

                add(
                    SectorModel.Data(
                        android.R.color.darker_gray,
                        R.drawable.ic_baseline_account_circle_2
                    )
                )

                add(
                    SectorModel.Data(
                        android.R.color.holo_blue_light,
                        R.drawable.ic_baseline_account_circle_3
                    )
                )

                add(
                    SectorModel.Data(
                        android.R.color.holo_red_dark,
                        R.drawable.ic_baseline_account_circle_4
                    )
                )

                add(
                    SectorModel.Data(
                        android.R.color.holo_green_light,
                        R.drawable.ic_baseline_account_circle_5
                    )
                )

                add(
                    SectorModel.Data(
                        android.R.color.holo_red_light,
                        R.drawable.ic_baseline_account_circle_6
                    )
                )
                add(
                    SectorModel.Data(
                        android.R.color.holo_orange_light,
                        R.drawable.ic_baseline_account_circle_3
                    )
                )
                add(
                    SectorModel.Data(
                        android.R.color.holo_blue_bright,
                        R.drawable.ic_baseline_account_circle_2
                    )
                )
                add(
                    SectorModel.Data(
                        android.R.color.holo_green_light,
                        R.drawable.ic_baseline_account_circle_1
                    )
                )
                add(
                    SectorModel.Data(
                        android.R.color.holo_red_light,
                        R.drawable.ic_baseline_account_circle_5
                    )
                )
            }
}