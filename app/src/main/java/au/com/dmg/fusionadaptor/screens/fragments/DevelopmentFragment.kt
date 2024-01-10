package au.com.dmg.fusionadaptor.screens.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import au.com.dmg.fusionadaptor.databinding.FragmentDevelopmentBinding
import au.com.dmg.fusionadaptor.screens.RequestActivity
import au.com.dmg.fusionadaptor.screens.TestTransactionActivity
import au.com.dmg.fusionadaptor.utils.Logger


class DevelopmentFragment : Fragment() {
    private var _binding: FragmentDevelopmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDevelopmentBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnDiagnosticTest.setOnClickListener {
            Logger.logEvent("DevelopmentFragment", "Diagnostic Test button clicked")
            val intent = Intent(context, TestTransactionActivity::class.java)
            startActivity(intent)
        }
        binding.btnTest.setOnClickListener {
            Logger.logEvent("DevelopmentFragment", "Test button clicked")
            val intent = Intent(context, RequestActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeviceInfo.setOnClickListener {
            Logger.logEvent("DevelopmentFragment", "Device Info button clicked")
            val _OSVERSION = System.getProperty("os.version")
            val _RELEASE = Build.VERSION.RELEASE
            val _DEVICE = Build.DEVICE
            val _MODEL = Build.MODEL
            val _PRODUCT = Build.PRODUCT
            val _BRAND = Build.BRAND
            val _DISPLAY = Build.DISPLAY
            val _CPU_ABI = Build.CPU_ABI
            val _CPU_ABI2 = Build.CPU_ABI2
            val _UNKNOWN = Build.UNKNOWN
            val _HARDWARE = Build.HARDWARE
            val _ID = Build.ID
            val _MANUFACTURER = Build.MANUFACTURER
            val _SERIAL = Build.SERIAL
            val _USER = Build.USER
            val _HOST = Build.HOST

            val deviceInfo = "DeviceInfo:\n" +
                    "_OSVERSION: $_OSVERSION\n" +
                    "_DEVICE: $_DEVICE\n" +
                    "_MODEL: $_MODEL\n" +
                    "_PRODUCT: $_PRODUCT\n" +
                    "_BRAND: $_BRAND\n" +
                    "_ID: $_ID\n" +
                    "_MANUFACTURER: $_MANUFACTURER\n" +
                    "_DISPLAY: $_DISPLAY\n" +
                    "_SERIAL: $_SERIAL\n" +
                    "_HARDWARE: $_HARDWARE\n" +
                    "_HOST: $_HOST\n" +
                    "_USER: $_USER\n"
            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setMessage(deviceInfo)
                .setCancelable(false)
                .setNegativeButton("OK"
                ) { dialog, id -> dialog.cancel() }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}