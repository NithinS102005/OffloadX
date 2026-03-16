package com.example.offload

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.content.Context
import android.os.BatteryManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.offload.databinding.FragmentUploadBinding


class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SharedViewModel by activityViewModels()

    private var selectedMode: String = "COMPOSITE"
    private var selectedUri: Uri? = null
    private var actualFileSizeMB: Double = 0.0

    // 1. Gallery Launcher (Composite)
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUri = uris.first()
            analyzeFile(selectedUri!!)
        }
    }

    // 2. File Explorer Launcher (Complex)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            analyzeFile(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Automatically generate a Task ID when the screen opens

        // Automatically generate a Task ID when the screen opens
        binding.etTaskId.setText("TASK_${System.currentTimeMillis() / 1000}")
        binding.etProcessId.setText("PROC_${java.util.UUID.randomUUID().toString().take(8)}")

        // --- DIRECT REDIRECTION: Listen for shared files from outside the app ---
        viewModel.sharedUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                selectedMode = "DIRECT REDIRECTION"
                selectedUri = uri
                binding.tvFileStatus.text = "Mode: Direct Redirection (Shared File)"
                analyzeFile(uri)
                // Consume the intent so it doesn't trigger again on rotation
                viewModel.setSharedUri(null)
            }
        }

        // --- MUTUAL EXCLUSION CHECKBOX LOGIC ---
        binding.cbLocal.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.cbCloud.isChecked = false // Deselect Cloud
            }
        }

        binding.cbCloud.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.cbLocal.isChecked = false // Deselect Local
            }
        }

        // --- MODE SELECTION ---
        binding.btnModeComposite.setOnClickListener {
            selectedMode = "COMPOSITE"
            binding.tvFileStatus.text = "Select File for Upload"
            
            binding.btnModeComposite.backgroundTintList = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.RED))
            binding.btnModeComposite.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE))
            
            binding.btnModeComplex.backgroundTintList = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, android.graphics.Color.GRAY))
            binding.btnModeComplex.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, android.graphics.Color.DKGRAY))
        }

        binding.btnModeComplex.setOnClickListener {
            selectedMode = "COMPLEX"
            binding.tvFileStatus.text = "Select File for Upload"
            
            binding.btnModeComplex.backgroundTintList = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, android.graphics.Color.RED))
            binding.btnModeComplex.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE))
            
            binding.btnModeComposite.backgroundTintList = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, android.graphics.Color.GRAY))
            binding.btnModeComposite.setTextColor(com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, android.graphics.Color.DKGRAY))
        }

        // --- MAGNIFYING GLASS LOGIC ---
        binding.btnSearchFile.setOnClickListener {
            if (selectedMode == "COMPOSITE") {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            } else {
                filePickerLauncher.launch(arrayOf("*/*"))
            }
        }

        // --- UPLOAD BUTTON ---
        binding.btnFinalUpload.setOnClickListener {
            if (selectedUri == null) {
                Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = binding.etTaskTitle.text.toString()
            if (title.isEmpty()) {
                binding.etTaskTitle.error = "Title Required"
                return@setOnClickListener
            }

            val ipAddress = "192.168.1.100:8000" // Default or retrieved elsewhere if needed

            // Real-time System Check Before Passing to Engine
            val battery = getBatteryPercentage()
            val netType = getNetworkType()

            val engine = DecisionEngine()
            val deviceId = "android_device_1"
            val fileToUpload = selectedUri!!
            
            binding.btnFinalUpload.isEnabled = false
            binding.btnFinalUpload.text = "Processing..."
            
            lifecycleScope.launch(Dispatchers.Default) {
                val start = System.currentTimeMillis()
                
                // --- Pass REAL System State to Decision Engine ---
                val decision = engine.decide(
                    taskName = title,
                    dataSizeMB = actualFileSizeMB,
                    batteryPercent = battery,
                    networkType = netType,
                    isCloudBackupNeeded = binding.cbCloud.isChecked
                )
                
                var routeDuration = System.currentTimeMillis() - start
                var executionStatusMsg = "Decided Route"
                var finalProcessedUrl = ""

                if (decision == ExecutionRoute.HUB || decision == ExecutionRoute.CLOUD) {
                    val client = HubOffloadClient(requireContext())
                    // Task Type is HARDCODED TO IMAGE_GRAYSCALE mapped to backend processor for composite images
                    val offloadResponse = client.offloadToHub(ipAddress, deviceId, "IMAGE_GRAYSCALE", fileToUpload)
                    
                    routeDuration = System.currentTimeMillis() - start
                    
                    if (offloadResponse.success) {
                        executionStatusMsg = "[Success] Backend Offload ms: $routeDuration"
                        finalProcessedUrl = offloadResponse.resultMsg ?: ""
                    } else {
                        executionStatusMsg = "[Failed] ${offloadResponse.errorMessage}"
                    }
                } else {
                    // Simulating local processing wait
                    Thread.sleep((actualFileSizeMB * 100).toLong().coerceAtLeast(300))
                    routeDuration = System.currentTimeMillis() - start
                    executionStatusMsg = "[Success] Fake Local Execution ms: $routeDuration"
                }

                // Switch back to Main Thread to update UI
                withContext(Dispatchers.Main) {
                    binding.btnFinalUpload.isEnabled = true
                    binding.btnFinalUpload.text = "Start Upload Process"

                    val logStr = "Mode: $selectedMode | Route: $decision | " +
                                 "Size: ${"%.2f".format(actualFileSizeMB)}MB\n$executionStatusMsg"
                    
                    viewModel.addTask(title, logStr)
                    
                    // --- DYNAMICALLY ADD TO DOWNLOADS TAB ---
                    val mimeType = requireContext().contentResolver.getType(selectedUri!!) ?: ""
                    
                    // Since Video Analytics generates a Peak Insight JPG overlay, force it down to Image type
                    var prettyFileType = "doc"
                    var extStr = ""
                    
                    if (mimeType.contains("video") || mimeType.contains("image")) {
                        prettyFileType = "image"
                        extStr = ".jpg"
                    }
                    
                    val today = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    viewModel.addFile(
                        FileModel(
                            fileName = "$title (Processed)$extStr",
                            fileSize = "${"%.2f".format(actualFileSizeMB)} MB",
                            fileDate = today,
                            fileType = prettyFileType,
                            processedUrl = finalProcessedUrl
                        )
                    )
                    
                    if (decision == ExecutionRoute.HUB || decision == ExecutionRoute.CLOUD) {
                         binding.cbCloud.isChecked = true
                    } else {
                         binding.cbLocal.isChecked = true
                    }
                    
                    Toast.makeText(context, executionStatusMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- HELPER EXTRACTORS ---
    
    private fun analyzeFile(uri: Uri) {
        // 1. Basic checks
        val mimeType = requireContext().contentResolver.getType(uri) ?: ""

        // 2. Safely extract file size
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                val sizeBytes = cursor.getLong(sizeIndex)
                actualFileSizeMB = sizeBytes / (1024.0 * 1024.0)
                
                val fileName = cursor.getString(nameIndex) ?: "Unknown"
                
                // Prevent OOM by blocking extreme files purely loading into Local Queue
                if (actualFileSizeMB > 200.0) {
                     Toast.makeText(context, "File massive (>$200MB): $fileName. Stream Processing needed.", Toast.LENGTH_LONG).show()
                }
                
                binding.tvFileStatus.text = "File: $fileName\nSize: ${"%.2f".format(actualFileSizeMB)} MB"
            }
        }
    }

    private fun getBatteryPercentage(): Int {
        val bm = requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getNetworkType(): NetworkType {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return NetworkType.OFFLINE
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkType.OFFLINE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.OFFLINE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}