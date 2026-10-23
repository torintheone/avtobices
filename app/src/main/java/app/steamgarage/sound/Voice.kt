package app.steamgarage.sound

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import androidx.annotation.ArrayRes
import app.steamgarage.R
import app.steamgarage.data.Settings
import app.steamgarage.data.VoiceMode
import app.steamgarage.domain.Defaults
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** События, которые озвучивает механик (таблица «Голос» SPEC.md). [important] — звучат и в режиме «Только важное». */
enum class VoiceEvent(@ArrayRes val phrases: Int, val important: Boolean) {
    SERVICE(R.array.voice_service, important = true),
    CAR_ADDED(R.array.voice_car_added, important = false),
    MILEAGE(R.array.voice_mileage, important = false),
    SYNC_OK(R.array.voice_sync_ok, important = true),
    SYNC_ERROR(R.array.voice_sync_error, important = true),
}

/** Состояние синтезатора речи. */
enum class VoiceStatus { IDLE, STARTING, READY, NO_RUSSIAN, UNAVAILABLE }

/**
 * Голос механика: системный TextToSpeech, русский язык, тон чуть ниже обычного.
 * Для каждого события несколько фраз, подряд одна и та же не звучит. Все вызовы — из главного потока.
 */
class Voice(context: Context, private val settings: () -> Settings) {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private var tts: TextToSpeech? = null
    private var pending: String? = null
    private val lastIndex = mutableMapOf<Int, Int>()
    private val _status = MutableStateFlow(VoiceStatus.IDLE)
    val status: StateFlow<VoiceStatus> = _status

    /** Запустить синтезатор заранее: первая фраза не опоздает, и сразу видно, есть ли русский голос. */
    fun warmUp() {
        if (tts != null) return
        _status.value = VoiceStatus.STARTING
        tts = try {
            TextToSpeech(appContext) { code -> onInit(code) }
        } catch (_: Exception) {
            _status.value = VoiceStatus.UNAVAILABLE
            null
        }
    }

    /** Заново проверить голос — например, после возвращения с экрана установки голосовых данных. */
    fun restart() {
        shutdown()
        warmUp()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        pending = null
        _status.value = VoiceStatus.IDLE
    }

    private fun onInit(code: Int) {
        val engine = tts ?: return
        if (code != TextToSpeech.SUCCESS) {
            _status.value = VoiceStatus.UNAVAILABLE
            return
        }
        val result = try {
            engine.setLanguage(RUSSIAN)
        } catch (_: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _status.value = VoiceStatus.NO_RUSSIAN
            pending = null
            return
        }
        engine.setPitch(Defaults.VOICE_PITCH)
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        _status.value = VoiceStatus.READY
        pending?.let { speakNow(it) }
        pending = null
    }

    fun say(event: VoiceEvent) {
        if (allowed(event.important)) speak(pick(event.phrases))
    }

    /** «Внимание: моторное масло — пора менять» — при открытии приложения, не чаще раза в день. */
    fun warnOverdue(itemName: String) {
        if (allowed(important = true)) speak(pick(R.array.voice_overdue).format(itemName))
    }

    /** Пробная фраза из настроек: звучит при любом режиме голоса, но тоже молчит в беззвучном режиме. */
    fun test() {
        if (AudioPolicy.mayPlay(audio, settings().soundInSilent)) speak(pick(R.array.voice_test))
    }

    /** Экран установки голосовых данных системы; false — на телефоне такого экрана нет. */
    fun openVoiceInstall(): Boolean = try {
        appContext.startActivity(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun allowed(important: Boolean): Boolean {
        val s = settings()
        val byMode = when (s.voiceMode) {
            VoiceMode.OFF -> false
            VoiceMode.IMPORTANT -> important
            VoiceMode.ALL -> true
        }
        return byMode && AudioPolicy.mayPlay(audio, s.soundInSilent)
    }

    private fun speak(text: String) {
        if (text.isBlank()) return
        when (_status.value) {
            VoiceStatus.READY -> speakNow(text)
            VoiceStatus.IDLE, VoiceStatus.STARTING -> {
                pending = text
                warmUp()
            }
            VoiceStatus.NO_RUSSIAN, VoiceStatus.UNAVAILABLE -> Unit
        }
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "steamgarage-${System.nanoTime()}")
    }

    private fun pick(@ArrayRes res: Int): String {
        val phrases = appContext.resources.getStringArray(res)
        if (phrases.size <= 1) return phrases.firstOrNull().orEmpty()
        var index = Random.nextInt(phrases.size)
        if (index == lastIndex[res]) index = (index + 1) % phrases.size
        lastIndex[res] = index
        return phrases[index]
    }

    private companion object {
        val RUSSIAN: Locale = Locale.forLanguageTag("ru-RU")
    }
}
