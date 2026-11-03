package com.brainrot.rebuilt

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainrot.rebuilt.ui.components.MascotView
import com.brainrot.rebuilt.ui.theme.BrainPalTheme
import com.brainrot.rebuilt.ui.theme.CoralPrimary
import com.brainrot.rebuilt.ui.theme.GreenSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra("TARGET_PACKAGE") ?: "Social App"
        val app = BrainPalApp.instance
        val isStrictMode = intent.getBooleanExtra("IS_STRICT_MODE", app.prefsRepository.isStrictMode())
        val roastLang = app.prefsRepository.getRoastLanguage()
        val meme = app.memeRepository.getRandomMeme(roastLang)

        setContent {
            BrainPalTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.94f)
                ) {
                    BlockOverlayContent(
                        targetPackage = targetPackage,
                        memeCaption = meme.caption,
                        memeUrl = meme.url,
                        passesLeft = app.prefsRepository.getEmergencyPasses(),
                        isStrictMode = isStrictMode,
                        onExitApp = {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        },
                        onUsePass = {
                            if (app.prefsRepository.useEmergencyPass()) {
                                finish()
                            }
                        },
                        onBypassFriction = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun BlockOverlayContent(
    targetPackage: String,
    memeCaption: String,
    memeUrl: String,
    passesLeft: Int,
    isStrictMode: Boolean,
    onExitApp: () -> Unit,
    onUsePass: () -> Unit,
    onBypassFriction: () -> Unit
) {
    var countdown by remember { mutableStateOf(10) }
    var canContinue by remember { mutableStateOf(false) }
    var memeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Friction countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        canContinue = true
    }

    // DEF-06 FIX: Load meme image/GIF in background if URL is provided
    LaunchedEffect(memeUrl) {
        if (memeUrl.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(memeUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 3500
                    conn.readTimeout = 3500
                    conn.doInput = true
                    conn.connect()
                    val input = conn.inputStream
                    val bmp = BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        memeBitmap = bmp
                    }
                } catch (e: Exception) {
                    // Fall back to mascot drawable
                    withContext(Dispatchers.Main) {
                        memeBitmap = null
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(
                text = "FOCUS INTERVENTION",
                style = MaterialTheme.typography.labelLarge,
                color = CoralPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hold up! You hit your limit.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = targetPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )
        }

        // Mascot & Meme Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (memeBitmap != null) {
                    Image(
                        bitmap = memeBitmap!!.asImageBitmap(),
                        contentDescription = "Roast Meme",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    MascotView(
                        resId = R.drawable.br_brain_standing_5,
                        contentDescription = "Roasted Brain Mascot",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                // Roast Caption
                Text(
                    text = "\"$memeCaption\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CoralPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exit App Button
            Button(
                onClick = onExitApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exit App & Protect Streak", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Emergency Pass Button
            if (!isStrictMode) {
                OutlinedButton(
                    onClick = onUsePass,
                    enabled = passesLeft > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (passesLeft > 0) "Use 5-Min Pass ($passesLeft left today)" else "No passes left today",
                        color = if (passesLeft > 0) Color.White else Color.Gray
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Strict Mode Active: Emergency passes disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DEF-05 FIX: Friction Timer & Bypass Button
            if (!canContinue) {
                Text(
                    text = "Bypass unlocks in $countdown seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            } else {
                if (!isStrictMode) {
                    FilledTonalButton(
                        onClick = onBypassFriction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Text("Continue to App (Friction Passed)", color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text(
                        text = "Strict Mode: Bypasses are locked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralPrimary
                    )
                }
            }
        }
    }
}
