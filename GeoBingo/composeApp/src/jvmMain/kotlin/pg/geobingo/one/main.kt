package pg.geobingo.one

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val icon = BitmapPainter(loadImageBitmap(Thread.currentThread().contextClassLoader!!.getResourceAsStream("icon.png")!!))
    Window(
        onCloseRequest = ::exitApplication,
        title = "KatchIt!",
        icon = icon,
    ) {
        App()
    }
}