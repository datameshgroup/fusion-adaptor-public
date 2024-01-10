package au.com.dmg.fusionadaptor.screens.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusionadaptor.BuildConfig
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.databinding.FragmentSettingsBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logError
import au.com.dmg.fusionadaptor.utils.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val TAG = "SettingsFragment"
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val  showDatameshValues = false

    override fun onResume() {
        super.onResume()
        getValues()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        _binding = FragmentSettingsBinding.inflate(layoutInflater, container, false)

        getValues()

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getValues()
    }

    // clear the binding in order to avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun getValues() {
        // Retrieve configuration data
        lifecycleScope.launch {
            try {
                val config = Configuration.getConfiguration(requireContext()).first()
                val enableTip = Configuration.getEnableTip(requireContext()).first()
                val showResultScreen = Configuration.getShowResultScreen(requireContext()).first()

                binding.btnClose.setOnClickListener {
                    Logger.logEvent("Settings","Close Button clicked");
                    requireActivity().finish()
                }

                // Update UI with configuration data
                binding.txtPosName.text = config.posName
                binding.txtVersion.text = BuildConfig.VERSION_NAME
                binding.txtProviderIdentification.text = config.providerIdentification
                binding.txtApplicationName.text = config.applicationName
                binding.txtSoftwareVersion.text = config.softwareVersion
                binding.txtCertificationCode.text = config.certificationCode

                binding.toggleTipping.isChecked = enableTip
                binding.toggleTipping.setOnCheckedChangeListener { _, isChecked ->
                    saveEnableTip(isChecked)
                }
                binding.toggleResultScreen.isChecked = showResultScreen
                binding.toggleResultScreen.setOnCheckedChangeListener { _, isChecked ->
                    saveShowResultScreen(isChecked)
                }
                //Hide Datamesh-provided information
                if (showDatameshValues) {
                    binding.layoutDatameshProvidedSettings.visibility = View.VISIBLE
                } else {
                    binding.layoutDatameshProvidedSettings.visibility = View.GONE
                }

            } catch (e: Exception) {
                logError(TAG, e.toString())
                Toast.makeText(context, "Error retrieving configuration", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEnableTip(isChecked: Boolean) {
        lifecycleScope.launch {
            Configuration.setEnableTip(requireContext(), isChecked)
        }
    }

    private fun saveShowResultScreen(isChecked: Boolean) {
        lifecycleScope.launch {
            Configuration.setShowResultScreen(requireContext(), isChecked)
        }
    }
}