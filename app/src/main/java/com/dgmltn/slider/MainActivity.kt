package com.dgmltn.slider

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.dgmltn.slider.internal.AbsSlider

class MainActivity : AppCompatActivity() {

    //val pin1: PinView by lazy { findViewById(R.id.pin1) as PinView }
    //val slider1: HorizontalSlider by lazy { findViewById(R.id.slider1) as HorizontalSlider }
    //val slider2: ArcSlider by lazy { findViewById(R.id.slider2) as ArcSlider }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //slider1.isEnabled = false;

//        slider2.getChildAt(0).addOnValueChangedListener { pin, oldVal, newVal ->
//            pin.text = Math.round(newVal + 50).toString()
//        }
    }

}
