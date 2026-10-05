package com.example.multiprofile

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var store: ProfileStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProfileStore(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        root.addView(Button(this).apply {
            text = "+ Profil Baru"
            setOnClickListener {
                store.add(FingerprintGenerator.generate(System.currentTimeMillis()))
                recreate()
            }
        })
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            addView(listBox)
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0
        ).apply { weight = 1f })

        store.list().forEach { fp ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 20, 0, 20)
            }
            row.addView(TextView(this).apply {
                text = "${fp.name}\nUA: ${fp.userAgent.take(48)}...\n${fp.width}x${fp.height} • ${fp.timezone}"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                text = "Buka"
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, BrowserActivity::class.java).putExtra("id", fp.id))
                }
            })
            row.addView(Button(this).apply {
                text = "X"
                setOnClickListener {
                    store.remove(fp.id)
                    recreate()
                }
            })
            listBox.addView(row)
        }
        setContentView(root)
    }
}
