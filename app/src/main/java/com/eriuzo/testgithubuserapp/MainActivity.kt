package com.eriuzo.testgithubuserapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.commit
import com.eriuzo.testgithubuserapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        title = "Github User Finder"
        supportFragmentManager.commit {
            replace(binding.root.id, UsersFragment.newInstance())
        }
    }
}