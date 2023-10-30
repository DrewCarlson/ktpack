import coingecko.CoinGeckoClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("Hello!")
    println(CoinGeckoClient().ping().toString())
}
