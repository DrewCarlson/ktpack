import coingecko.CoinGeckoClient

suspend fun main() {
    println("Hello!")
    println(CoinGeckoClient().ping().toString())
}
