package com.google.firebase.goloco

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import java.io.File
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val config = Configuration.getInstance()
        config.userAgentValue = BuildConfig.APPLICATION_ID
        config.osmdroidBasePath = File(getExternalFilesDir(null), "osmdroid")
        config.osmdroidTileCache = File(config.osmdroidBasePath, "tile")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        Navigation.findNavController(this, R.id.nav_host_fragment)
            .setGraph(R.navigation.nav_graph)
    }
}