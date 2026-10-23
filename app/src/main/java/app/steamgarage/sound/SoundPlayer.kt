package app.steamgarage.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import app.steamgarage.R
import app.steamgarage.data.Settings
import app.steamgarage.ui.components.Sfx
import app.steamgarage.ui.components.SfxPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * Звуки механизмов через SoundPool: все файлы загружаются заранее, при запуске интерфейса.
 * Играют в потоке «медиа» — его не глушит беззвучный режим, поэтому беззвучный режим проверяем сами:
 * в нём звуков нет, если в настройках не разрешено иное.
 */
class SoundPlayer(context: Context, private val settings: () -> Settings) : SfxPlayer {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val ids: Map<Sfx, Int>

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
        ids = Sfx.entries.associateWith { pool.load(appContext, it.resource(), 1) }
    }

    override fun play(sfx: Sfx) {
        val s = settings()
        if (!s.soundEnabled || !AudioPolicy.mayPlay(audio, s.soundInSilent)) return
        val id = ids[sfx]?.takeIf { it in loaded } ?: return
        val volume = s.soundVolume.coerceIn(0f, 1f)
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    private fun Sfx.resource(): Int = when (this) {
        Sfx.CLICK -> R.raw.sfx_click
        Sfx.LEVER -> R.raw.sfx_lever
        Sfx.STEAM -> R.raw.sfx_steam
        Sfx.CLANG -> R.raw.sfx_clang
        Sfx.DRUM_TICK -> R.raw.sfx_drum_tick
        Sfx.SYNC_START -> R.raw.sfx_sync_start
        Sfx.SYNC_OK -> R.raw.sfx_sync_ok
        Sfx.SYNC_ERROR -> R.raw.sfx_sync_error
    }

    private companion object {
        /** Барабан одометра щёлкает часто — несколько потоков, чтобы щелчки не обрывали друг друга. */
        const val MAX_STREAMS = 4
    }
}

/** Общее правило для звуков и голоса: в беззвучном и вибро-режиме молчим, если не разрешено. */
object AudioPolicy {
    fun mayPlay(audio: AudioManager?, allowedInSilent: Boolean): Boolean {
        if (allowedInSilent || audio == null) return true
        return audio.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
}
