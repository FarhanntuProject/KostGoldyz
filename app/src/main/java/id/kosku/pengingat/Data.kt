package id.kosku.pengingat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min

/* ---------- Model ---------- */

data class KosConfig(
    val nama: String,
    val pemilik: String,
    val hp: String,
    val sewa: Long,
    val tgl: Int,          // tanggal jatuh tempo tiap bulan (1–31)
    val mulai: YearMonth,  // bulan tagihan pertama yang dicatat
    val ingat: Int,        // ingatkan H-berapa
    val rek: String,
)

data class Pembayaran(
    val id: String,
    val periode: YearMonth,
    val tgl: LocalDate,
    val jumlah: Long,
    val ket: String,
)

data class AppState(val cfg: KosConfig?, val bayar: List<Pembayaran>)

enum class Status { AMAN, SEGERA, TERLAMBAT }

data class Tagihan(
    val target: YearMonth,        // bulan sewa yang harus dibayar berikutnya
    val due: LocalDate,           // tanggal jatuh temponya
    val days: Long,               // sisa hari (negatif = terlambat)
    val status: Status,
    val overdue: List<YearMonth>, // bulan yang sudah lewat jatuh tempo dan belum dibayar
    val lunasBulanIni: Boolean,
)

/* ---------- Logika tagihan ---------- */

fun dueDate(p: YearMonth, tgl: Int): LocalDate = p.atDay(min(tgl, p.lengthOfMonth()))

fun hitung(cfg: KosConfig, bayar: List<Pembayaran>, today: LocalDate = LocalDate.now()): Tagihan {
    val paid = bayar.map { it.periode }.toSet()
    val cur = YearMonth.from(today)

    val unpaid = mutableListOf<YearMonth>()
    var p = cfg.mulai
    var guard = 0
    while (!p.isAfter(cur) && guard < 240) {
        if (p !in paid) unpaid.add(p)
        p = p.plusMonths(1); guard++
    }
    val overdue = unpaid.filter { dueDate(it, cfg.tgl).isBefore(today) }

    var lunas = false
    var target: YearMonth
    if (unpaid.isNotEmpty()) {
        target = unpaid.first()
    } else {
        lunas = !cfg.mulai.isAfter(cur)
        target = cur.plusMonths(1)
        while (target in paid) target = target.plusMonths(1)
    }
    if (target.isBefore(cfg.mulai)) target = cfg.mulai

    val due = dueDate(target, cfg.tgl)
    val days = ChronoUnit.DAYS.between(today, due)
    val status = when {
        days < 0 -> Status.TERLAMBAT
        days <= cfg.ingat -> Status.SEGERA
        else -> Status.AMAN
    }
    return Tagihan(target, due, days, status, overdue, lunas)
}

/* ---------- Format Indonesia ---------- */

val ID: Locale = Locale.forLanguageTag("id-ID")

fun rp(n: Long): String {
    val f = NumberFormat.getCurrencyInstance(ID)
    f.maximumFractionDigits = 0
    f.minimumFractionDigits = 0
    return f.format(n).replace(" ", " ")
}

fun bulan(p: YearMonth): String =
    p.format(DateTimeFormatter.ofPattern("MMMM yyyy", ID)).replaceFirstChar { it.uppercase() }

fun tanggal(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ID))

fun tanggalPendek(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM yyyy", ID))

fun hari(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE", ID)).replaceFirstChar { it.uppercase() }

fun ribuan(n: Long): String = NumberFormat.getIntegerInstance(ID).format(n)

/** 0812xxxx → 62812xxxx untuk link WhatsApp */
fun waNumber(hp: String): String {
    var d = hp.filter { it.isDigit() }
    if (d.isEmpty()) return ""
    if (d.startsWith("0")) d = "62" + d.drop(1)
    else if (d.startsWith("8")) d = "62$d"
    return d
}

/* ---------- Penyimpanan (SharedPreferences + JSON) ---------- */

class Repo(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("kos", Context.MODE_PRIVATE)

    fun load(): AppState {
        val raw = prefs.getString("state", null) ?: return AppState(null, emptyList())
        return try {
            val o = JSONObject(raw)
            val cfg = o.optJSONObject("cfg")?.let {
                KosConfig(
                    nama = it.optString("nama"),
                    pemilik = it.optString("pemilik"),
                    hp = it.optString("hp"),
                    sewa = it.optLong("sewa"),
                    tgl = it.optInt("tgl", 1),
                    mulai = YearMonth.parse(it.optString("mulai")),
                    ingat = it.optInt("ingat", 3),
                    rek = it.optString("rek"),
                )
            }
            val arr = o.optJSONArray("bayar") ?: JSONArray()
            val list = (0 until arr.length()).map { i ->
                val b = arr.getJSONObject(i)
                Pembayaran(
                    id = b.optString("id"),
                    periode = YearMonth.parse(b.optString("periode")),
                    tgl = LocalDate.parse(b.optString("tgl")),
                    jumlah = b.optLong("jumlah"),
                    ket = b.optString("ket"),
                )
            }
            AppState(cfg, list)
        } catch (e: Exception) {
            AppState(null, emptyList())
        }
    }

    fun save(s: AppState) {
        val o = JSONObject()
        s.cfg?.let { c ->
            o.put("cfg", JSONObject().apply {
                put("nama", c.nama); put("pemilik", c.pemilik); put("hp", c.hp)
                put("sewa", c.sewa); put("tgl", c.tgl); put("mulai", c.mulai.toString())
                put("ingat", c.ingat); put("rek", c.rek)
            })
        }
        val arr = JSONArray()
        s.bayar.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id); put("periode", b.periode.toString()); put("tgl", b.tgl.toString())
                put("jumlah", b.jumlah); put("ket", b.ket)
            })
        }
        o.put("bayar", arr)
        prefs.edit().putString("state", o.toString()).apply()
    }
}
