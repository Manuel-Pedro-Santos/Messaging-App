package pt.isel.daw

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseUpdatedChannelEmitterAdapter(
    private val sseEmitter: SseEmitter,
) : UpdatedChannelEmitter {
    override fun emit(signal: UpdatedChannel) {
        val msg =
            when (signal) {
                is UpdatedChannel.Message ->
                    SseEmitter
                        .event()
                        .id(signal.id.toString())
                        .name("message")
                        .data(signal)
                is UpdatedChannel.KeepAlive -> SseEmitter.event().comment(signal.timestamp.epochSeconds.toString())
            }
        sseEmitter.send(msg)
    }

    override fun onCompletion(callback: () -> Unit) {
        sseEmitter.onCompletion(callback)
    }

    override fun onError(callback: (Throwable) -> Unit) {
        sseEmitter.onError(callback)
    }
}
