package id.kosku.pengingat

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderWorker.schedule(this)
        setContent { KosTheme { App() } }
    }
}

/* ---------- Tema ---------- */

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F6B57), onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBE1), onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFFE9A51E), onSecondary = Color(0xFF2A1C00),
    background = Color(0xFFEAF0EC), onBackground = Color(0xFF14302A),
    surface = Color(0xFFFBFDFB), onSurface = Color(0xFF14302A),
    surfaceVariant = Color(0xFFDCE6E0), onSurfaceVariant = Color(0xFF5D716B),
    outline = Color(0xFF8FA39C), outlineVariant = Color(0xFFCFDAD4),
    error = Color(0xFFB4282E), errorContainer = Color(0xFFFBE1E1), onErrorContainer = Color(0xFF7A1418),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3FB596), onPrimary = Color(0xFF06211A),
    primaryContainer = Color(0xFF1B4D40), onPrimaryContainer = Color(0xFFCDEBE1),
    secondary = Color(0xFFF0B743), onSecondary = Color(0xFF2A1C00),
    background = Color(0xFF0E1A17), onBackground = Color(0xFFE4EEEA),
    surface = Color(0xFF152521), onSurface = Color(0xFFE4EEEA),
    surfaceVariant = Color(0xFF223630), onSurfaceVariant = Color(0xFF93A8A1),
    outline = Color(0xFF5E746D), outlineVariant = Color(0xFF2A3E39),
    error = Color(0xFFFF8A8E), errorContainer = Color(0xFF40191C), onErrorContainer = Color(0xFFFFDADB),
)

@Composable
fun KosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

/* ---------- Navigasi sederhana ---------- */

enum class Screen { HOME, SETTINGS, PAY }

@Composable
fun App() {
    val ctx = LocalContext.current
    val repo = remember { Repo(ctx) }
    var state by remember { mutableStateOf(repo.load()) }
    var screen by remember { mutableStateOf(if (state.cfg == null) Screen.SETTINGS else Screen.HOME) }

    fun update(s: AppState) {
        state = s
        repo.save(s)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notif.bolehNotif(ctx)) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = screen != Screen.HOME && state.cfg != null) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(
            state = state,
            onSettings = { screen = Screen.SETTINGS },
            onPay = { screen = Screen.PAY },
            onDelete = { id -> update(state.copy(bayar = state.bayar.filterNot { it.id == id })) },
        )
        Screen.SETTINGS -> SettingsScreen(
            cfg = state.cfg,
            onCancel = { screen = Screen.HOME },
            onSave = { cfg ->
                update(state.copy(cfg = cfg))
                screen = Screen.HOME
                toast(ctx, "Data kos disimpan")
            },
        )
        Screen.PAY -> PayScreen(
            state = state,
            onCancel = { screen = Screen.HOME },
            onSave = { b ->
                update(state.copy(bayar = state.bayar + b))
                screen = Screen.HOME
                toast(ctx, "Pembayaran ${bulan(b.periode)} dicatat")
            },
        )
    }
}

fun toast(ctx: android.content.Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

/* ---------- Beranda ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState, onSettings: () -> Unit, onPay: () -> Unit, onDelete: (String) -> Unit) {
    val cfg = state.cfg ?: return
    val ctx = LocalContext.current
    val t = hitung(cfg, state.bayar)
    val list = state.bayar.sortedWith(compareByDescending<Pembayaran> { it.periode }.thenByDescending { it.tgl })
    val wa = waNumber(cfg.hp)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(cfg.nama, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            "Sewa ${rp(cfg.sewa)} · tgl ${cfg.tgl} tiap bulan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Atur kos") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp,
                bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DueCard(cfg, t) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPay, modifier = Modifier.weight(1f)) { Text("Catat pembayaran") }
                    if (wa.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val msg = "Halo ${cfg.pemilik.ifBlank { "Bu/Pak" }}, saya sudah transfer sewa kos " +
                                    "${bulan(t.target)} sebesar ${rp(cfg.sewa)}. Mohon dicek ya, terima kasih."
                                try {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$wa?text=${Uri.encode(msg)}")))
                                } catch (e: ActivityNotFoundException) {
                                    toast(ctx, "WhatsApp atau browser tidak ditemukan")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Kabari pemilik") }
                    }
                }
            }
            item { InfoCard(cfg) }
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Riwayat pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (list.isNotEmpty()) Text("${list.size} kali", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "Belum ada pembayaran. Ketuk \"Catat pembayaran\" setelah transfer.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(list, key = { it.id }) { b -> PayRow(b, cfg, onDelete) }
                item {
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.onSurface)
                    Row(Modifier.padding(top = 8.dp)) {
                        Text("Total dibayar", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(rp(list.sumOf { it.jumlah }), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun statusStyle(s: Status): Triple<Color, Color, String> {
    val dark = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme
    return when (s) {
        Status.TERLAMBAT -> Triple(cs.errorContainer, cs.onErrorContainer, "Terlambat")
        Status.SEGERA -> if (dark) Triple(Color(0xFF3A2E12), Color(0xFFF2C062), "Segera bayar")
                         else Triple(Color(0xFFFCEFCF), Color(0xFF7A4D00), "Segera bayar")
        Status.AMAN -> Triple(cs.primaryContainer, cs.onPrimaryContainer, "Aman")
    }
}

@Composable
fun DueCard(cfg: KosConfig, t: Tagihan) {
    val (bg, fg, label) = statusStyle(t.status)
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CalendarChip(t.due)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(50)) {
                        Text(
                            if (t.days == 0L) "Jatuh tempo hari ini" else label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    when {
                        t.days < 0 -> Countdown("${-t.days}", "hari lewat")
                        t.days == 0L -> Countdown("Hari ini", "")
                        else -> Countdown("${t.days}", "hari lagi")
                    }
                    Text(rp(cfg.sewa), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Sewa ${bulan(t.target)}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            if (t.lunasBulanIni) {
                Text(
                    "LUNAS",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .rotate(-12f)
                        .border(2.dp, cs.error, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = cs.error,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                )
            }
        }
        if (t.overdue.size > 1) {
            Surface(
                color = cs.errorContainer, contentColor = cs.onErrorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp).fillMaxWidth(),
            ) {
                Text(
                    "Tunggakan ${t.overdue.size} bulan · total ${rp(t.overdue.size * cfg.sewa)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun Countdown(big: String, small: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(big, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
        if (small.isNotEmpty()) {
            Text(
                small, modifier = Modifier.padding(bottom = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun CalendarChip(due: LocalDate) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.width(86.dp).clip(shape).border(1.5.dp, cs.onSurface, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().background(cs.primary).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(
                due.format(DateTimeFormatter.ofPattern("MMM", ID)).uppercase().removeSuffix("."),
                color = cs.onPrimary, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            )
        }
        Text("${due.dayOfMonth}", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 44.sp)
        Text(
            hari(due), style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
fun InfoCard(cfg: KosConfig) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Info kos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val onCopy: (String) -> Unit = { v ->
                clip.setText(AnnotatedString(v))
                toast(ctx, "Disalin")
            }
            InfoLine("Pemilik", cfg.pemilik)
            InfoLine("WhatsApp", cfg.hp, cfg.hp, onCopy)
            val rekNum = Regex("\\d[\\d\\s-]{5,}\\d").find(cfg.rek)?.value?.filter { it.isDigit() } ?: cfg.rek
            InfoLine("Rekening", cfg.rek, rekNum, onCopy)
            InfoLine("Nominal", rp(cfg.sewa), cfg.sewa.toString(), onCopy)
            InfoLine("Ingatkan", "H-${cfg.ingat}, jam 09.00")
        }
    }
}

@Composable
fun InfoLine(k: String, v: String, copy: String? = null, onCopy: (String) -> Unit = {}) {
    if (v.isBlank()) return
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(k, color = cs.onSurfaceVariant, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
        Text(v, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (copy != null) TextButton(onClick = { onCopy(copy) }) { Text("Salin") }
    }
}

@Composable
fun PayRow(b: Pembayaran, cfg: KosConfig, onDelete: (String) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val telat = b.tgl.isAfter(dueDate(b.periode, cfg.tgl))
    val cs = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(bulan(b.periode), fontWeight = FontWeight.Bold)
                val meta = buildList {
                    add("Dibayar ${tanggalPendek(b.tgl)}")
                    if (telat) add("telat")
                    if (b.ket.isNotBlank()) add(b.ket)
                }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = if (telat) cs.error else cs.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(rp(b.jumlah), fontWeight = FontWeight.Bold)
                TextButton(onClick = { confirm = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("Hapus", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = cs.outlineVariant)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Hapus pembayaran?") },
            text = { Text("Catatan sewa ${bulan(b.periode)} sebesar ${rp(b.jumlah)} akan dihapus.") },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete(b.id) }) { Text("Hapus", color = cs.error) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } },
        )
    }
}

/* ---------- Komponen form ---------- */

@Composable
fun Stepper(label: String, value: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.fillMaxWidth().border(1.dp, cs.outline, RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPrev) { Text("‹", fontSize = 22.sp) }
            Text(value, Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onNext) { Text("›", fontSize = 22.sp) }
        }
    }
}

@Composable
fun ErrorText(msg: String?) {
    if (msg != null) Text(msg, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
}

/* ---------- Atur data kos ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(cfg: KosConfig?, onCancel: () -> Unit, onSave: (KosConfig) -> Unit) {
    val ctx = LocalContext.current
    val today = LocalDate.now()
    var nama by remember { mutableStateOf(cfg?.nama ?: "") }
    var pemilik by remember { mutableStateOf(cfg?.pemilik ?: "") }
    var hp by remember { mutableStateOf(cfg?.hp ?: "") }
    var sewa by remember { mutableStateOf(cfg?.sewa?.toString() ?: "") }
    var tgl by remember { mutableIntStateOf(cfg?.tgl ?: today.dayOfMonth) }
    var mulai by remember { mutableStateOf(cfg?.mulai ?: YearMonth.from(today)) }
    var ingat by remember { mutableIntStateOf(cfg?.ingat ?: 3) }
    var rek by remember { mutableStateOf(cfg?.rek ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    fun build(): KosConfig? {
        val n = sewa.toLongOrNull() ?: 0L
        err = when {
            nama.isBlank() -> "Isi nama kos."
            n <= 0 -> "Isi nominal sewa per bulan."
            else -> null
        }
        if (err != null) return null
        return KosConfig(nama.trim(), pemilik.trim(), hp.trim(), n, tgl, mulai, ingat, rek.trim())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (cfg == null) "Selamat datang" else "Data kos", fontWeight = FontWeight.Bold) },
                actions = { if (cfg != null) TextButton(onClick = onCancel) { Text("Batal") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (cfg == null) {
                Text(
                    "Isi data kos sekali saja. Setelah itu aplikasi menghitung jatuh tempo dan " +
                        "mengirim notifikasi sebelum waktunya bayar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(nama, { nama = it }, label = { Text("Nama kos") }, placeholder = { Text("Kos Melati Tebet") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pemilik, { pemilik = it }, label = { Text("Pemilik / ibu kos") }, placeholder = { Text("Bu Siti") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                hp, { hp = it }, label = { Text("No. WhatsApp pemilik") }, placeholder = { Text("0812xxxxxxx") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                sewa, { sewa = it.filter(Char::isDigit).take(12) }, label = { Text("Sewa per bulan (Rp)") },
                supportingText = { sewa.toLongOrNull()?.let { Text(rp(it)) } },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
            Stepper(
                "Jatuh tempo tiap tanggal", "Tanggal $tgl",
                onPrev = { tgl = if (tgl <= 1) 31 else tgl - 1 },
                onNext = { tgl = if (tgl >= 31) 1 else tgl + 1 },
            )
            if (tgl > 28) {
                Text(
                    "Di bulan yang lebih pendek, jatuh tempo pindah ke tanggal terakhir bulan itu.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Stepper("Tagihan pertama yang dicatat", bulan(mulai), onPrev = { mulai = mulai.minusMonths(1) }, onNext = { mulai = mulai.plusMonths(1) })
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ingatkan sebelum jatuh tempo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3, 5, 7).forEach { h ->
                        FilterChip(selected = ingat == h, onClick = { ingat = h }, label = { Text("H-$h") })
                    }
                }
            }
            OutlinedTextField(
                rek, { rek = it }, label = { Text("Rekening / e-wallet tujuan") },
                placeholder = { Text("BCA 1234567890 a.n. Siti Aminah") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(err)
            Button(onClick = { build()?.let(onSave) }, modifier = Modifier.fillMaxWidth()) { Text("Simpan") }
            OutlinedButton(
                onClick = {
                    val c = build() ?: return@OutlinedButton
                    if (!Notif.bolehNotif(ctx)) {
                        toast(ctx, "Izinkan notifikasi untuk Pengingat Kos dulu")
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        )
                    } else {
                        val (title, text) = pesanNotifikasi(c, hitung(c, emptyList()))
                        Notif.show(ctx, title, text)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Coba kirim notifikasi") }
            Text(
                "Notifikasi dicek tiap hari sekitar jam 09.00, mulai H-$ingat sampai sewa dicatat lunas.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/* ---------- Catat pembayaran ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(state: AppState, onCancel: () -> Unit, onSave: (Pembayaran) -> Unit) {
    val cfg = state.cfg ?: return
    val t = remember { hitung(cfg, state.bayar) }
    var periode by remember { mutableStateOf(t.target) }
    var tgl by remember { mutableStateOf(LocalDate.now()) }
    var jumlah by remember { mutableStateOf(cfg.sewa.toString()) }
    var ket by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var pickDate by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Catat pembayaran", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = onCancel) { Text("Batal") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Stepper("Sewa untuk bulan", bulan(periode), onPrev = { periode = periode.minusMonths(1) }, onNext = { periode = periode.plusMonths(1) })
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tanggal bayar", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) { Text(tanggal(tgl)) }
            }
            OutlinedTextField(
                jumlah, { jumlah = it.filter(Char::isDigit).take(12) }, label = { Text("Jumlah (Rp)") },
                supportingText = { jumlah.toLongOrNull()?.let { Text(rp(it)) } },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                ket, { ket = it }, label = { Text("Keterangan (opsional)") },
                placeholder = { Text("Transfer BCA, sudah dikonfirmasi") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(err)
            Button(
                onClick = {
                    val n = jumlah.toLongOrNull() ?: 0L
                    err = when {
                        n <= 0 -> "Isi jumlah yang dibayar."
                        state.bayar.any { it.periode == periode } ->
                            "Sewa ${bulan(periode)} sudah tercatat. Hapus catatan lama dulu kalau mau mengganti."
                        else -> null
                    }
                    if (err == null) {
                        onSave(Pembayaran(System.currentTimeMillis().toString(36), periode, tgl, n, ket.trim()))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Simpan") }
        }
    }

    if (pickDate) {
        val dp = rememberDatePickerState(
            initialSelectedDateMillis = tgl.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { tgl = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Batal") } },
        ) { DatePicker(state = dp) }
    }
}
