package com.bypassbd.otp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bypassbd.otp.ui.theme.IvacOtpTheme
import com.bypassbd.otp.ui.theme.Ok
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IvacOtpTheme { AppScreen(vm) } }
    }
}

// Only the permissions the core feature needs. No READ_SMS (we never read the
// inbox), which keeps the app off Play Protect's "reads your messages" radar.
private fun neededPermissions(): Array<String> {
    val list = mutableListOf(Manifest.permission.RECEIVE_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    return list.toTypedArray()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var smsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        smsGranted = result[Manifest.permission.RECEIVE_SMS] == true ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    var phone by rememberSaveable { mutableStateOf("") }
    var pair by rememberSaveable { mutableStateOf("") }
    var manual by rememberSaveable { mutableStateOf("") }
    var seeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings) {
        if (!seeded && (settings.phone.isNotBlank() || settings.pairCode.isNotBlank())) {
            phone = settings.phone; pair = settings.pairCode; seeded = true
        }
    }

    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); vm.clearToast() }
    }

    val ready = settings.isReady && smsGranted

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("IVAC OTP Autofill", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp))
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusCard(ready = ready, smsGranted = smsGranted, paired = settings.isReady)

            if (!smsGranted || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
                PermissionCard { permLauncher.launch(neededPermissions()) }
            }

            PairingCard(
                phone = phone, onPhone = { phone = it },
                pair = pair, onPair = { pair = it.uppercase() },
                onSave = { vm.save(phone, pair) }
            )

            ManualCard(
                text = manual,
                onText = { manual = it },
                busy = busy,
                onForward = { vm.forwardManual(manual) },
                onTest = { vm.sendTest() }
            )

            ActivityCard(log)

            Text(
                "Tip: exclude this app from battery optimisation so Android never stops it " +
                    "from receiving SMS in the background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun StatusCard(ready: Boolean, smsGranted: Boolean, paired: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) Ok.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (ready) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                null,
                tint = if (ready) Ok else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(if (ready) "Ready — listening for OTP" else "Setup needed", fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !smsGranted -> "Grant SMS permission below."
                        !paired -> "Enter phone + client key, then Save."
                        else -> "New IVAC codes are forwarded automatically."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    SectionCard("Permissions", Icons.Filled.Lock) {
        Text(
            "This app needs to receive incoming SMS (to catch the OTP) and, on Android 13+, " +
                "to show status notifications. It never reads your existing messages.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text("GRANT PERMISSIONS") }
    }
}

@Composable
private fun PairingCard(
    phone: String, onPhone: (String) -> Unit,
    pair: String, onPair: (String) -> Unit,
    onSave: () -> Unit
) {
    SectionCard("Pairing", Icons.Filled.Link) {
        OutlinedTextField(
            value = phone, onValueChange = onPhone,
            label = { Text("Phone number (this SIM)") },
            placeholder = { Text("01XXXXXXXXX") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pair, onValueChange = onPair,
            label = { Text("Client key (from the extension)") },
            placeholder = { Text("ABCD-1234") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("SAVE PAIRING") }
    }
}

@Composable
private fun ManualCard(
    text: String, onText: (String) -> Unit,
    busy: Boolean,
    onForward: () -> Unit, onTest: () -> Unit
) {
    SectionCard("Manual / test", Icons.Filled.Send) {
        OutlinedTextField(
            value = text, onValueChange = onText,
            label = { Text("Paste an OTP SMS or a code") },
            placeholder = { Text("Four-Two-Six-One-Four-Seven  or  426147") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
        Button(onClick = onForward, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("FORWARD") }
        TextButton(onClick = onTest, enabled = !busy) { Text("Send a test push (0000)") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ActivityCard(log: List<com.bypassbd.otp.data.Prefs.LogEntry>) {
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()) }
    SectionCard("Recent activity", Icons.Filled.History) {
        if (log.isEmpty()) {
            Text("Nothing yet — forwarded codes will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            log.forEach { e ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(fmt.format(Date(e.at)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace)
                    Text(e.text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (e.text.startsWith("✓")) Ok
                                else if (e.text.startsWith("✗")) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
