package pg.geobingo.one.util

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

object AppLogger {
    var minLevel: LogLevel = LogLevel.WARN

    /** Optional callback for forwarding errors to a crash reporter (e.g. Crashlytics). */
    var crashReporter: ((String, Throwable?) -> Unit)? = null

    fun d(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.INFO, tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        // Always forward errors to crash reporter regardless of minLevel
        if (level == LogLevel.ERROR) {
            crashReporter?.invoke("[$tag] $message", throwable)
        }
        if (level < minLevel) return
        val prefix = when (level) {
            LogLevel.DEBUG -> "[D]"
            LogLevel.INFO -> "[I]"
            LogLevel.WARN -> "[W]"
            LogLevel.ERROR -> "[E]"
        }
        println("$prefix [$tag] $message")
        throwable?.let { println("$prefix [$tag] ${it.stackTraceToString()}") }
    }
}
