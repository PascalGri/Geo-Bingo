package pg.geobingo.one

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform