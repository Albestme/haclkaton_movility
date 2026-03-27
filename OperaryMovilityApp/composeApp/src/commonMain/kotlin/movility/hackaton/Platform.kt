package movility.hackaton

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform