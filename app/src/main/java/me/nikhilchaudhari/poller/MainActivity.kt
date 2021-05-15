package me.nikhilchaudhari.poller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import me.nikhilchaudhari.simplepoller.Poller

class MainActivity : AppCompatActivity() {

    private lateinit var poller: Poller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textview = findViewById<TextView>(R.id.textview)

        poller = Poller.Builder()
            .get(url = "https://demo4034250.mockable.io/testpoll")
            .setDispatcher(Dispatchers.Main)
            .onResponse {
                textview.text = it.text
            }.onError {
                textview.text = it?.message
            }.build()

        poller.start()
    }

    override fun onStop() {
        super.onStop()
        poller.stop()
    }
}