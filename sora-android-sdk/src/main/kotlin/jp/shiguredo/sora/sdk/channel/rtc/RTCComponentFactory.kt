package jp.shiguredo.sora.sdk.channel.rtc

import jp.shiguredo.sora.sdk.channel.option.SoraMediaOption
import jp.shiguredo.sora.sdk.util.SoraLogger
import org.webrtc.*


class RTCComponentFactory(private val option: SoraMediaOption) {
    companion object {
        private val TAG = RTCComponentFactory::class.simpleName
    }

    // メインスレッド(UI スレッド)で呼ばれる必要がある。
    // そうでないと Effect の ClassLoader.loadClass で NPE が発生する。
    fun createPeerConnectionFactory(): PeerConnectionFactory {
        val cl = Thread.currentThread().contextClassLoader
        SoraLogger.d(TAG, "createPeerConnectionFactory(): classloader=${cl}")
        val options = PeerConnectionFactory.Options()
        val factoryBuilder = PeerConnectionFactory.builder()
                .setOptions(options)

        val encoderFactory = option.videoEncoderFactory
                ?:
                when (option.videoUpstreamContext) {
                    is EglBase.Context ->
                        DefaultVideoEncoderFactory(option.videoUpstreamContext,
                                true /* enableIntelVp8Encoder */,
                                false /* enableH264HighProfile */)
                    else -> SoftwareVideoEncoderFactory()
                }

        val decoderFactory = option.videoDecoderFactory
                ?:
                when (option.videoDownstreamContext) {
                    is EglBase.Context ->
                        DefaultVideoDecoderFactory(option.videoDownstreamContext)
                    else -> SoftwareVideoDecoderFactory()
                }

        decoderFactory.supportedCodecs.forEach {
            SoraLogger.d(TAG, "decoderFactory supported codec: ${it.name} ${it.params}")
        }
        encoderFactory.supportedCodecs.forEach {
            SoraLogger.d(TAG, "encoderFactory supported codec: ${it.name} ${it.params}")
        }
        factoryBuilder.setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)

        return factoryBuilder.createPeerConnectionFactory()
    }

    fun createSDPConstraints(): MediaConstraints {
        val constraints = MediaConstraints()
        SoraLogger.d(TAG, "createSDPConstraints: ${constraints}")
        return constraints
    }

    fun createVideoManager() : RTCLocalVideoManager {
        val videoManager = option.videoCapturer?.let {
            RTCLocalVideoManagerImpl(it)
        } ?: RTCNullLocalVideoManager()
        SoraLogger.d(TAG, "videoManager created: ${videoManager}")
        return videoManager
    }

    fun createAudioManager(): RTCLocalAudioManager {
        return RTCLocalAudioManager(option.audioUpstreamEnabled)
    }
}
