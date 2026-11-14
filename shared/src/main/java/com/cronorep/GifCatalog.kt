package com.cronorep

/** The gym GIFs. The app owner chose this list. No API call is needed. */
object GifCatalog {
    /** GIPHY ids. */
    val IDS = listOf(
        "HSBsApUGF4erUp7NHV",
        "hWddcO7xQWCqIew5rH",
        "6uPX6ZNoCiPNxVmAUB",
        "7kbEhOfZzxNPTerRRz",
        "lOqNS2HUyN8OJV0CuF",
        "H4TsgO30w3qZZeMmlL",
        "WbsQEnBiIWKIM",
        "cuXLiXhdgOuJi",
        "3oriNZNU6VDrDxM7sI",
        "gElZT8LKDPJeg",
        "zy5evPW7VPCKI",
        "TyuwvnAzEPVFC",
        "0py1N0feFG64kbG2r3",
        "kzqdxYKp2yy31ROpUM",
        "39lwENv9kD5nO",
        "ehsEjVlJeXiqBqEpGc",
        "oShObTfbg3S5G",
        "0YvEGKIctG6nNOwdLd",
        "FSO0qBQ9Dwno4",
        "XMJb1k3GuouMfXSPIJ",
        "VIMDNQUn0rQ0L4ojIr",
        "bdpIDWZFkI67S",
        "XDSJtVcyGNXXrcCDnr",
        "VRGeARH0HUgr6",
        "Nf9kT8LvdkkStAMGEp",
        "3oz8xDnHOUs1Zmgcyk",
        "Y7O3LHmhllEk",
        "KHhaSdqhakNxa884ea",
        "3o6Ztq2Ed2cSSwAg8M",
        "xUPGcKoAYCn5fHK0Zq",
        "7ueSsRAZsOvDST7Q8p",
        "nFyfPbGijyKMZrF65y",
        "shuybsYkLzyUomNI17",
        "IuZ47lDMjN9yXx4C41",
        "h6M6Dfp5eXCjoeBzJC",
        "HQ9dS2mcEiqYw",
        "pPZ0wbiNbwJMDEzFxm",
        "YpZh61QjwuXq15FhV7",
        "KxKU0W6iBy5M88MwnY",
        "tskVbKESeJPSpVgf57",
        "wVKWhHbI3afhx3MW1e",
        "VFDeGtRSHswfe",
        "NJQubFbs35Vug",
    )

    /** 200 px high: sharp enough for the timer background and a small download. */
    fun url(id: String) = "https://media.giphy.com/media/$id/200.gif"

    /** 100 px high still image for the chooser grid. */
    fun thumbUrl(id: String) = "https://media.giphy.com/media/$id/100_s.gif"
}
