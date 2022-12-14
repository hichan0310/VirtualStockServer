import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.time.LocalDateTime


/**
 * 사기, 팔기 통신 : localhost:8888
 * 최대 코인 만들기
 *
 */


fun sleep(time: Int) {
    val nextTime = LocalDateTime.now().plusSeconds(time.toLong())
    while (nextTime >= LocalDateTime.now()) {
    }
}

fun sleep(time: Double) {
    val nextTime = LocalDateTime.now().plusNanos((time * 1000000000).toLong())
    while (nextTime >= LocalDateTime.now()) {
    }
}

object parameters {
    val buyRate: Double = 3.0                  // 가격에 구매량이 반영되는 비율
    val sellRate: Double = 3.0                 // 가격에 판매량이 반영되는 비율
    val bias: Double = (-20).toDouble()        // 가격의 편향
    val buyLife: Int = 8                       // 이 시간을 지난 구매량은 반영되지 않음
    val sellLife: Int = 10                     // 이 시간을 지난 판매량은 반영되지 않음
    val initialPrice: Int = 1000               // 초기 가격
    val packetSize: Int = 100                  // 패킷 사이즈    패킷 : "mode,amount,ip_addr"
    val packetAmount: Int = 3                  // 패킷 안에 포함된 데이터 양
    val showFast: Double = 3.toDouble()        // 보여주는 주기
}

fun main(args: Array<String>) {     // input : userid, password, stockid, amount
    var sellHistory: MutableList<History> = mutableListOf<History>()
    var buyHistory: MutableList<History> = mutableListOf<History>()
    val price: Int = parameters.initialPrice
    var Money: Int = 0
    var inSijang: Int = 0
    var buySpeed: Int = 0
    var sellSpeed: Int = 0


    val host = "localhost"
    val port = 8888
    val socket: DatagramSocket = DatagramSocket(port)

    val Routine = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            val packet: DatagramPacket = DatagramPacket(ByteArray(parameters.packetSize), parameters.packetSize)
            withContext(Dispatchers.IO) {
                socket.receive(packet)
            }
            var _data: List<Byte> = packet.data.toList()
            try {
                var i = 0
                while (_data[i].toInt() != 0) i += 1
                _data = _data.slice(IntRange(0, i - 1))
            } catch (e: Exception) {
                println("wrong packet")
                continue
            }
            val data: ByteArray = _data.toByteArray()
            val message: List<String> = String(data).split(",")
            try {
                val x = message[1].toInt()
                if (message.size != parameters.packetAmount) {
                    println("wrong packet")
                    continue
                }
                if (message[0] == "buy") {
                    val t = mutableListOf<History>(
                        History(LocalDateTime.now().plusSeconds(parameters.buyLife.toLong()), x, message[2])
                    )
                    t.addAll(buyHistory)
                    buyHistory = t
                    Money += price * x
                    inSijang += x
                    buySpeed += x
                } else if (message[0] == "sell") {
                    val t = mutableListOf<History>(
                        History(LocalDateTime.now().plusSeconds(parameters.sellLife.toLong()), x, message[2])
                    )
                    t.addAll(sellHistory)
                    sellHistory = t
                    Money -= price * x
                    inSijang -= x
                    sellSpeed += x
                } else {
                    println("wrong packet")
                    continue
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("wrong packet")
                continue
            }
        }
    }

    val checkHistory = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            if (buyHistory.size != 0) if (buyHistory.last().time < LocalDateTime.now()) {
                buySpeed -= buyHistory[buyHistory.size - 1].amount
                buyHistory.removeAt(buyHistory.size - 1)
            }
            if (sellHistory.size != 0) if (sellHistory.last().time < LocalDateTime.now()) {
                sellSpeed -= sellHistory[sellHistory.size - 1].amount
                sellHistory.removeAt(sellHistory.size - 1)
            }
            print("")
        }
    }
    val updateStockPrice = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            val x = Stock.update(buySpeed, sellSpeed)
            sleep(1)
        }
    }

    println("====== START ======")
    println("구매량,\t 판매량,\t-->\t 가격,\t 순수익,\t 총 시장에 풀린 나히다 코인")
    while (true) {
        println("${buySpeed},\t\t ${sellSpeed},\t\t-->\t ${Stock.price},\t ${Money - Stock.price * inSijang},\t\t ${inSijang}")
        sleep(parameters.showFast)
    }
}

data class History(val time: LocalDateTime, val amount: Int, val ipAddr: String) {}

object Stock {
    val name: String = "나히다 주식"
    var price: Int = parameters.initialPrice

    fun update(buyAmount: Int, sellAmount: Int): Int {
        price = run {
            price + (parameters.buyRate * buyAmount - parameters.sellRate * sellAmount + parameters.bias).toInt()
        }
        return price
    }
}