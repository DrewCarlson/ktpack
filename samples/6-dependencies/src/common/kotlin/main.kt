import co.touchlab.kermit.Logger
import coingecko.CoinGeckoClient
import kotlinx.coroutines.runBlocking

// TODO: Make this runnable on js
fun main() = runBlocking {
    Logger.d("Hello!")
    Logger.d(CoinGeckoClient().ping().toString())
}
