package au.com.dmg.fusionadaptor.screens

import android.app.AlertDialog
//import androidx.appcompat.app.AlertDialog;
import android.widget.Button
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.databinding.SettingsActivityBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.screens.adapters.ViewPagerAdapter
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.Logger
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: SettingsActivityBinding
    private val TAG = "SettingsActivity"
    private var isTestEnvironment = false
    //For switching environment
    private lateinit var gestureDetector: GestureDetector
    private var tapCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val tapResetDelayMillis = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        binding.viewPager2.adapter = adapter

        setupTapGesture()
        observeTestEnvironment()
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = "Status"
                1 -> tab.text = "Settings"
                2 -> {
                if (isTestEnvironment) {
                    tab.text = "Development"
                } else {
                    // If isTestEnvironment is false, hide the "Development" tab
                    tab.view.visibility = View.GONE
                }
            }
            }
        }.attach()

        Logger.log("SettingsActivity","onCreate","IsTestEnvironment = $isTestEnvironment")
    }

    private fun observeTestEnvironment() {
        lifecycleScope.launch {
            isTestEnvironment = Configuration.getUseTestEnvironment(applicationContext).first()
            Logger.log("SettingsActivity","observeTestEnvironment","IsTestEnvironment = $isTestEnvironment")
            updateUIVisibility()
        }
    }

    private fun updateUIVisibility() {
        binding.layoutEnvironment.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
        if (isTestEnvironment) {
            binding.layoutEnvironment.visibility = View.VISIBLE
        } else {
            binding.layoutEnvironment.visibility = View.INVISIBLE
        }
    }

    private fun setupTapGesture() {
        gestureDetector = GestureDetector(this, GestureListener()).apply {
            setOnDoubleTapListener(null)
        }
    }
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private var envName = "Production"

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            tapCount++

            // Reset tapCount after a certain delay
            handler.removeCallbacks(resetTapCountRunnable)
            handler.postDelayed(resetTapCountRunnable, tapResetDelayMillis)

            if (tapCount == 5) {
               logInfo(TAG, "Switching Environment...")
                Logger.logEvent("SettingActivity", "Switching Environment")

                // Check if the activity is still running before showing the dialog
                if (!isFinishing && !isDestroyed) {
                    changeEnvironmentConfirmation()
                }
                tapCount = 0
            }
            return super.onSingleTapUp(e)
        }

        private fun changeEnvironmentConfirmation() {
            this.envName = if (isTestEnvironment) "Production" else "Development"
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@SettingsActivity, R.style.TransparentDialog)
            val inflater = LayoutInflater.from(this@SettingsActivity)
            val dialogView = inflater.inflate(R.layout.custom_dialog_content, null)
            builder.setView(dialogView)

            val messageTextView: TextView = dialogView.findViewById(R.id.dialog_message)
            messageTextView.text = "Switch to $envName environment?"

            val alertDialog: AlertDialog = builder.create()

            val positiveButton: Button = dialogView.findViewById(R.id.positive_button)
            positiveButton.setOnClickListener {
                changeEnvironment()
                alertDialog.cancel()
            }

            val negativeButton: Button = dialogView.findViewById(R.id.negative_button)
            negativeButton.setOnClickListener {
                alertDialog.cancel()
            }

            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setCancelable(false)

            if (!isFinishing && !isDestroyed) {
                alertDialog.show()
            }
        }

        private fun changeEnvironment() {
            lifecycleScope.launch {
                Configuration.setUseTestEnvironment(applicationContext, !isTestEnvironment)
                Logger.logEvent("SettingActivity", "Switched to $envName Environment")
                logInfo(TAG, "Switched to $envName Environment")
                recreate()
            }
        }
    }

    private val resetTapCountRunnable = Runnable {
        tapCount = 0
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev) || ev?.let { gestureDetector.onTouchEvent(it) } == true
    }
}
