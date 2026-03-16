package com.example.offload

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.Toast
import android.net.Uri
import androidx.activity.viewModels

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Bottom Navigation
        val navView: BottomNavigationView = findViewById(R.id.bottom_nav)

        // 2. Setup Navigation Host and Controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 3. Link Bottom Nav with Controller
        navView.setupWithNavController(navController)

        // 4. Setup Action Bar (to show titles and the back button)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)

        // 5. Handle Phase 1 Direct Redirection (Intent filters)
        handleIntent(intent)

        Toast.makeText(this, "Diagnostics: App Updated", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
             val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
             if (uri != null) {
                  val viewModel: SharedViewModel by viewModels()
                  viewModel.setSharedUri(uri)
                  
                  // Make sure we end up on the Upload tab if we aren't there
                  navController.navigate(R.id.navigation_upload)
             }
        }
    }



    // Makes the "Back" arrow in the top left work
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}