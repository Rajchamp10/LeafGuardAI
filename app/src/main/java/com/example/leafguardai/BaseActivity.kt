package com.example.leafguardai

import android.content.Intent
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.ncorti.slidetoact.SlideToActView

// Notice the word 'open' - this allows other activities to inherit from it
open class BaseActivity : AppCompatActivity() {

    private lateinit var baseDrawerLayout: DrawerLayout

    // We override setContentView to magically wrap your screens inside the drawer!
    override fun setContentView(layoutResID: Int) {
        val baseView = layoutInflater.inflate(R.layout.activity_base, null)
        baseDrawerLayout = baseView.findViewById(R.id.baseDrawerLayout)

        // Inject the specific screen's layout into the empty FrameLayout
        val contentFrame = baseView.findViewById<FrameLayout>(R.id.baseContentFrame)
        layoutInflater.inflate(layoutResID, contentFrame, true)

        super.setContentView(baseView)

        setupToolbarAndMenu()
    }

    private fun setupToolbarAndMenu() {
        val toolbar = findViewById<Toolbar>(R.id.baseToolbar)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener {
            baseDrawerLayout.openDrawer(GravityCompat.START)
        }

        // Navigation Menu Clicks
        findViewById<TextView>(R.id.navScanDetect).setOnClickListener {
            navigateTo(MainActivity::class.java)
        }
        findViewById<TextView>(R.id.navCostCalculator).setOnClickListener {
            navigateTo(CostActivity::class.java)
            baseDrawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.navCalendar).setOnClickListener {
            navigateTo(CalendarActivity::class.java)
            baseDrawerLayout.closeDrawer(GravityCompat.START)
        }
        findViewById<TextView>(R.id.navHistory).setOnClickListener {
            Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show()
            baseDrawerLayout.closeDrawer(GravityCompat.START)
        }

        // Swipe Logout Logic
        val btnSwipeLogout = findViewById<SlideToActView>(R.id.btnSwipeLogout)
        btnSwipeLogout.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this@BaseActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    // Helper function to navigate smoothly without opening duplicates
    private fun navigateTo(activityClass: Class<*>) {
        baseDrawerLayout.closeDrawer(GravityCompat.START)
        if (this::class.java != activityClass) {
            startActivity(Intent(this, activityClass))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}