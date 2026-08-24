package br.com.rmf.kmp.cryptoview.ui.model

import androidx.compose.ui.graphics.Color
import br.com.rmf.kmp.cryptoview.ui.theme.CryptoOrange

data class MockExchangeBadge(
    val name: String,
    val shortName: String,
    val color: Color,
)

data class MockCoin(
    val id: Int,
    val name: String,
    val symbol: String,
    val glyph: String,
    val brandColor: Color,
    val price: String,
    val priceValue: Double,
    val variation24h: Double,
    val marketCapRank: Int,
    val minimum: String,
    val maximum: String,
    val volume24h: String,
    val exchanges: List<MockExchangeBadge>,
    val additionalExchanges: Int,
    val chart: List<Float>,
)

data class MockExchange(
    val id: Int,
    val name: String,
    val shortName: String,
    val color: Color,
    val rank: Int,
    val volume24h: String,
    val coinCount: Int,
    val launchDate: String,
    val description: String,
    val makerFee: String,
    val takerFee: String,
    val website: String,
    val assets: List<MockAsset>,
)

data class MockAsset(
    val name: String,
    val symbol: String,
    val amount: String,
    val value: String,
)

val Binance = MockExchangeBadge("Binance", "BN", Color(0xFFF3BA2F))
val Coinbase = MockExchangeBadge("Coinbase", "C", Color(0xFF1752F0))
val Kraken = MockExchangeBadge("Kraken", "K", Color(0xFF6555C6))
val Kucoin = MockExchangeBadge("KuCoin", "K", Color(0xFF18A887))

val MockCoins = listOf(
    MockCoin(
        id = 1,
        name = "Bitcoin",
        symbol = "BTC",
        glyph = "₿",
        brandColor = Color(0xFFF7931A),
        price = "$ 81.187,93",
        priceValue = 81_187.93,
        variation24h = 0.53,
        marketCapRank = 1,
        minimum = "$ 78.942",
        maximum = "$ 82.104",
        volume24h = "$ 29,9 bi",
        exchanges = listOf(Binance, Coinbase, Kraken),
        additionalExchanges = 12,
        chart = listOf(.58f, .68f, .61f, .64f, .48f, .55f, .42f, .36f, .51f, .45f, .31f, .38f, .55f, .60f, .72f, .66f, .57f, .63f, .52f, .68f, .62f, .75f, .88f, .79f, .93f),
    ),
    MockCoin(
        id = 1027,
        name = "Ethereum",
        symbol = "ETH",
        glyph = "◆",
        brandColor = Color(0xFF627EEA),
        price = "$ 2.489,12",
        priceValue = 2_489.12,
        variation24h = 1.31,
        marketCapRank = 2,
        minimum = "$ 2.410",
        maximum = "$ 2.515",
        volume24h = "$ 12,8 bi",
        exchanges = listOf(Binance, Coinbase, MockExchangeBadge("OKX", "O", Color.Black)),
        additionalExchanges = 8,
        chart = listOf(.35f, .38f, .42f, .41f, .48f, .47f, .53f, .50f, .58f, .61f, .59f, .67f),
    ),
    MockCoin(
        id = 5426,
        name = "Solana",
        symbol = "SOL",
        glyph = "≋",
        brandColor = Color(0xFF151515),
        price = "$ 94,73",
        priceValue = 94.73,
        variation24h = -0.58,
        marketCapRank = 3,
        minimum = "$ 92,20",
        maximum = "$ 98,45",
        volume24h = "$ 3,1 bi",
        exchanges = listOf(Binance, Coinbase, Kucoin),
        additionalExchanges = 6,
        chart = listOf(.72f, .68f, .70f, .61f, .56f, .58f, .51f, .47f, .49f, .43f, .45f, .40f),
    ),
    MockCoin(
        id = 825,
        name = "Tether USDt",
        symbol = "USDT",
        glyph = "₮",
        brandColor = Color(0xFF26A17B),
        price = "$ 1,00",
        priceValue = 1.0,
        variation24h = 0.02,
        marketCapRank = 4,
        minimum = "$ 0,99",
        maximum = "$ 1,01",
        volume24h = "$ 64,2 bi",
        exchanges = listOf(Binance, Kraken, Kucoin),
        additionalExchanges = 21,
        chart = listOf(.49f, .50f, .48f, .51f, .49f, .50f, .51f, .50f),
    ),
)

val MockExchanges = listOf(
    MockExchange(
        id = 270,
        name = "Binance",
        shortName = "BN",
        color = Color(0xFFF3BA2F),
        rank = 1,
        volume24h = "$ 18,4 bi",
        coinCount = 412,
        launchDate = "Julho de 2017",
        description = "Plataforma global de ativos digitais com ampla oferta de mercados e alta liquidez.",
        makerFee = "0,10%",
        takerFee = "0,10%",
        website = "binance.com",
        assets = listOf(
            MockAsset("Bitcoin", "BTC", "18.420 BTC", "$ 1,49 bi"),
            MockAsset("Tether USDt", "USDT", "842 mi USDT", "$ 842 mi"),
            MockAsset("Ethereum", "ETH", "215 mil ETH", "$ 535 mi"),
        ),
    ),
    MockExchange(
        id = 89,
        name = "Coinbase Exchange",
        shortName = "C",
        color = Color(0xFF1752F0),
        rank = 2,
        volume24h = "$ 3,1 bi",
        coinCount = 286,
        launchDate = "Junho de 2012",
        description = "Exchange de criptoativos com foco em segurança, conformidade e experiência simples.",
        makerFee = "0,40%",
        takerFee = "0,60%",
        website = "exchange.coinbase.com",
        assets = listOf(
            MockAsset("Bitcoin", "BTC", "9.840 BTC", "$ 798 mi"),
            MockAsset("Ethereum", "ETH", "172 mil ETH", "$ 428 mi"),
            MockAsset("USD Coin", "USDC", "305 mi USDC", "$ 305 mi"),
        ),
    ),
    MockExchange(
        id = 24,
        name = "Kraken",
        shortName = "K",
        color = Color(0xFF6555C6),
        rank = 3,
        volume24h = "$ 1,2 bi",
        coinCount = 241,
        launchDate = "Julho de 2011",
        description = "Uma das exchanges mais antigas do mercado, com negociação spot e serviços institucionais.",
        makerFee = "0,25%",
        takerFee = "0,40%",
        website = "kraken.com",
        assets = listOf(
            MockAsset("Bitcoin", "BTC", "5.120 BTC", "$ 415 mi"),
            MockAsset("Ethereum", "ETH", "98 mil ETH", "$ 244 mi"),
            MockAsset("Solana", "SOL", "1,4 mi SOL", "$ 132 mi"),
        ),
    ),
)

val ChartOrange = CryptoOrange
