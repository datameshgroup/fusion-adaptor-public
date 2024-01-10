package au.com.dmg.fusionadaptor.screens.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import au.com.dmg.fusion.Message
import au.com.dmg.fusion.MessageHeader
import au.com.dmg.fusion.data.MessageCategory
import au.com.dmg.fusion.data.MessageClass
import au.com.dmg.fusion.data.MessageType
import au.com.dmg.fusion.data.SaleCapability
import au.com.dmg.fusion.data.TerminalEnvironment
import au.com.dmg.fusion.request.SaleTerminalData
import au.com.dmg.fusion.request.SaleToPOIRequest
import au.com.dmg.fusion.request.loginrequest.LoginRequest
import au.com.dmg.fusion.request.loginrequest.SaleSoftware
import au.com.dmg.fusionadaptor.R
import au.com.dmg.fusionadaptor.databinding.FragmentStatusBinding
import au.com.dmg.fusionadaptor.datastore.Configuration
import au.com.dmg.fusionadaptor.screens.PairingActivity
import au.com.dmg.fusionadaptor.screens.TransactionProgressActivity
import au.com.dmg.fusionadaptor.utils.ConnectionHelper
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logError
import au.com.dmg.fusionadaptor.utils.DatameshUtils.Companion.logInfo
import au.com.dmg.fusionadaptor.utils.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID


class StatusFragment : Fragment() {
    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!
    private lateinit var startForResult:ActivityResultLauncher<Intent>
    private val TAG = "StatusFragment"

    private var currentPoiId = ""
    private var saleToPOIRequest: SaleToPOIRequest?=null

    override fun onResume() {
        super.onResume()
        loadScreen()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        startForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            logInfo(TAG, "Received Result")
            Logger.logIntentsResultReceived("StatusFragment", result.resultCode, result.data)
            if (result.resultCode == RESULT_OK) {
                logInfo(TAG, "Initial login after QR pairing successful\nJSON:  ${result.data?.toString()}")
            } else {
                logError(TAG, "Initial login for QR fairing failed")
            }
        }
        _binding = FragmentStatusBinding.inflate(layoutInflater, container, false)

        loadScreen()

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadScreen()
    }

    private fun loadScreen(){
        // todo check env

        lifecycleScope.launch {
            currentPoiId = Configuration.getPoiId(requireContext()).first()
        }

        Logger.log("StatusFragment","loadScreen", "LoadScreen called.  Current POI ID = $currentPoiId");

        if(currentPoiId.isNullOrBlank()){

            binding.trHeader.setBackgroundResource(R.drawable.rectangle_header_red)
            binding.pairingStatusImg.setImageResource(R.drawable.ic_unpaired)
            binding.txtPairingStatus.text = getString(R.string.not_paired_with_a_terminal)
            binding.txtPairedPOIID.visibility = View.INVISIBLE
            binding.btnLogin.visibility = View.INVISIBLE

            binding.btnPair.text = getString(R.string.pair_with_terminal)

            binding.btnPair.setOnClickListener {
                Logger.logEvent("StatusFragment", "Pair with Terminal button clicked")
                val intent = Intent(requireActivity(), PairingActivity::class.java)
                startActivity(intent)
            }
            //set btnPair to primary button
            binding.btnPair.setBackgroundResource(R.drawable.button_primary_background)
            binding.btnPair.setTextColor(ContextCompat.getColor(requireContext(), R.color.dmgWhite))
        }else{
            binding.trHeader.setBackgroundResource(R.drawable.rectangle_header_green)
            binding.pairingStatusImg.setImageResource(R.drawable.ic_paired)
            binding.txtPairingStatus.text = getString(R.string.paired_with_a_terminal)
            binding.txtPairedPOIID.text = "POIID: $currentPoiId"
            binding.txtPairedPOIID.visibility = View.VISIBLE
            binding.btnLogin.visibility = View.VISIBLE

            binding.btnPair.text = getString(R.string.unpair_terminal)
            binding.btnPair.setOnClickListener {
                Logger.logEvent("StatusFragment","Unpair Button clicked");
                unpairTerminal()
            }
            //set btnPair to seconday button
            binding.btnPair.setBackgroundResource(R.drawable.button_secondary_background)
            binding.btnPair.setTextColor(ContextCompat.getColor(requireContext(), R.color.dmgGrey)) // Set the text color

            binding.btnLogin.setOnClickListener {
                Logger.logEvent("StatusFragment","Login Button clicked");
                if(ConnectionHelper.isNetworkConnected(requireContext())){
                    val intent = Intent(requireActivity(), TransactionProgressActivity::class.java)
                    val loginRequest = buildLoginRequest()
                    val message = Message(loginRequest)
                    intent.putExtra("SaleToPOIJson", message.toJson())

                    startForResult.launch(intent)
                } else {
                    noNetworkConnection()
                }
            }
        }
        binding.btnClose.setOnClickListener {
            Logger.logEvent("StatusFragment","Close Button clicked");
            requireActivity().finish()
        }

    }

    private fun buildLoginRequest(): SaleToPOIRequest? {
        lifecycleScope.launch {
            val config = Configuration.getConfiguration(requireContext()).first()

            val saleSoftware = SaleSoftware.Builder() //
                .providerIdentification(config.providerIdentification) //
                .applicationName(config.applicationName) //
                .softwareVersion(config.softwareVersion) //
                .certificationCode(config.certificationCode) //
                .build()
            val saleTerminalData = SaleTerminalData.Builder() //
                .terminalEnvironment(TerminalEnvironment.SemiAttended) //
                .saleCapabilities(
                    listOf(
                        SaleCapability.CashierStatus, SaleCapability.CustomerAssistance,
                        SaleCapability.PrinterReceipt
                    )
                ) //
                .build()
            val loginRequest = LoginRequest.Builder() //
                .dateTime(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())) //
                .saleSoftware(saleSoftware) //
                .saleTerminalData(saleTerminalData) //
                .operatorLanguage("en") //
                .build()


            val header = MessageHeader.Builder()
                .protocolVersion("3.1-dmg")
                .messageClass(MessageClass.Service)
                .messageCategory(MessageCategory.Login)
                .messageType(MessageType.Request)
                .serviceID(UUID.randomUUID().toString())
                .saleID(config.saleId)
                .POIID(config.poiId)
                .build()

            saleToPOIRequest = SaleToPOIRequest.Builder()
                .messageHeader(header)
                .request(loginRequest)
                .build()
        }
        return saleToPOIRequest
    }

    private fun unpairTerminal() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.TransparentDialog)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.custom_dialog_content, null)
        builder.setView(dialogView)

        val messageTextView: TextView = dialogView.findViewById(R.id.dialog_message)
        messageTextView.text = "Are you sure you want to unpair from the terminal ($currentPoiId)?"

        val alertDialog: AlertDialog = builder.create()

        val positiveButton: Button = dialogView.findViewById(R.id.positive_button)
        positiveButton.setOnClickListener {
            reloadStatus()
            alertDialog.cancel()
        }

        val negativeButton: Button = dialogView.findViewById(R.id.negative_button)
        negativeButton.setOnClickListener {
            alertDialog.cancel()
        }

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false)

        alertDialog.show()
    }

    private fun noNetworkConnection() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("No network connection")
            .setCancelable(false)
            .setNegativeButton("OK") { dialog, id -> dialog.cancel() }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    //TODO
    private fun reloadStatus() {
        lifecycleScope.launch() {
            Configuration.updatePairingData(requireContext(), Configuration.PairingData("","",""))
            Logger.log("StatusFragment","reloadStatus","Cleared pairing data.");
            currentPoiId = Configuration.getPoiId(requireContext()).first()
            loadScreen()
        }
    }


    //todo clear the binding in order to avoid memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}