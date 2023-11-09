# PizzaView


**View represents circle with bunch of section. You can set color for each section separatly. Also view supports different icons for each section.**

### How it looks

![pizza_view](https://github.com/Vinny94051/CircleView/assets/37775244/93e1f4e3-4d6c-4c6d-94be-807eb36f87fd)

### How to use

#### 1. Add view into your layout
```
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:circle="http://schemas.android.com/apk/res-auto"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ru.kiryanov.circleview.presentation.ui.CircleView
        android:id="@+id/circle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true"
        android:layout_gravity="center"
        circle:animation_duration="400"
        circle:circle_radius="100dp"
        />

</RelativeLayout>
```

#### 2. Init view with sectors list in your code

```
 override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInfoBtn.setOnClickListener {
            circle.setSectorsInfo(getSectorsInfo())
            circle.setOnCircleClickListener { isActive, info ->
                // do something, when sector clicked
            }
        }
    }
```

```
   // add secotors as much as you need
   private fun getSectorsInfo(): List<SectorInfo> {
       return mutableListOf<SectorInfo>()
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
                        android.R.color.holo_red_light,
                        android.R.color.holo_blue_bright,
                        drawableId = R.drawable.ic_baseline_account_circle_5
                    )
                )
            }
  }
```


