import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.math.BigInteger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import kotlin.math.*


/**
 * 사기, 팔기 통신 : localhost:8888
 * 이 통신을 VirtualStockFinal과 할 예정
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
    val buyDelay: Int = 10                      // 사는 것이 순간 구매량에 반영되는 지연시간
    val sellDelay: Int = 3                     // 파는 것이 순간 구매량에 반영되는 지연시간
    val buyLife: Int = 70                      // 이 시간을 지난 구매량은 반영되지 않음
    val sellLife: Int = 60                     // 이 시간을 지난 판매량은 반영되지 않음
    val initialPrice: Int = 1000               // 초기 가격
    val packetSize: Int = 100                  // 패킷 사이즈    패킷 : "mode,amount,ip_addr"
    val packetAmount: Int = 3                  // 패킷 안에 포함된 데이터 양
    val updateFast: Int = 1                    // 업데이트 주기, 보여주는 주기
}


fun main(args: Array<String>) {     // input : userid, password, stockid, amount
    var buyHistory: MutableList<History> = mutableListOf<History>()
    var sellHistory: MutableList<History> = mutableListOf<History>()
    var oldHistoryBuy: MutableList<History> = mutableListOf<History>()
    var oldHistorySell: MutableList<History> = mutableListOf<History>()
    val price: Int = parameters.initialPrice
    var money: Int = 0
    var inSijang: Int = 0
    var buySpeed: Int = 0
    var sellSpeed: Int = 0
    var control: Boolean = false
    var gn = 0


    val host = "localhost"
    val port = 8888
    val socket: DatagramSocket = DatagramSocket(port)

    val routine = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            while (control) continue
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
                        History(LocalDateTime.now().plusSeconds(parameters.buyDelay.toLong()), x, message[2])
                    )
                    t.addAll(oldHistoryBuy)
                    oldHistoryBuy = t
                    money += Stock.price.toInt() * x
                    inSijang += x
                    //buySpeed += x
                    if (history.containsKey(message[2])) {
                        history[message[2]]!!.addAll(mutableListOf<History>(History(LocalDateTime.now(), x, "buy")))
                    } else {
                        history.put(message[2], mutableListOf(History(LocalDateTime.now(), x, "buy")))
                    }
                } else if (message[0] == "sell") {
                    val t = mutableListOf<History>(
                        History(LocalDateTime.now().plusSeconds(parameters.sellDelay.toLong()), x, message[2])
                    )
                    t.addAll(oldHistorySell)
                    oldHistorySell = t
                    money -= Stock.price.toInt() * x
                    inSijang -= x
                    //sellSpeed += x
                    if (history.containsKey(message[2])) {
                        history[message[2]]!!.addAll(mutableListOf<History>(History(LocalDateTime.now(), x, "sell")))
                    } else {
                        history.put(message[2], mutableListOf(History(LocalDateTime.now(), x, "sell")))
                    }
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
    val checkOldHistory = CoroutineScope(Dispatchers.IO).launch {
        while(true){
            while(control)continue
            if (oldHistoryBuy.size!=0) if (oldHistoryBuy.last().time < LocalDateTime.now()){
                var old=oldHistoryBuy[oldHistoryBuy.size-1]
                oldHistoryBuy.removeAt(oldHistoryBuy.size-1)
                buySpeed+=old.amount
                val t=mutableListOf<History>(
                    History(old.time.plusSeconds(parameters.buyLife.toLong()), old.amount, old.ipAddr)
                )
                t.addAll(buyHistory)
                buyHistory=t
            }
            if (oldHistorySell.size!=0) if (oldHistorySell.last().time<LocalDateTime.now()){
                var old=oldHistorySell[oldHistorySell.size-1]
                oldHistorySell.removeAt(oldHistorySell.size-1)
                sellSpeed+=old.amount
                val t=mutableListOf<History>(
                    History(old.time.plusSeconds(parameters.sellLife.toLong()), old.amount, old.ipAddr)
                )
                t.addAll(sellHistory)
                sellHistory=t
            }
            print("")
        }
    }
    val checkHistory = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            while (control) continue
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
            sleep(parameters.updateFast)
            while (control) continue
            val x = Stock.update(buySpeed, sellSpeed, inSijang)
            println("$money],")
        }
    }
    // 명령어 컨트롤
    /* pause
     *    - 일시정지
     * Gn add [num]
     *    - 귀신의 집 수익을 추가합니다.
     * Gn sub [num]
     *    - 귀신의 집 수익을 뺍니다.
     * print history all
     *    - 전체 히스토리를 포함하는 파일을 출력합니다.
     * print history [ip_addr]
     *    - 특정 ip_addr을 가진 사람의 히스토리를 포함하는 파일을 출력합니다.
     * print allCoins
     *    - 시장에 얼마나 많은 코인이 있는지 출력합니다.
     * print price
     *    - 현재 가격을 출력합니다.
     * print buySpeed
     *    - 1분 사이의 총 사람들의 코인 구매량을 출력합니다.
     * print sellSpeed
     *    - 1분 사이의 총 사람들의 코인 판매량을 출력합니다.
     * print revenue
     *    - 총 수익을 출력합니다.
     */
    println("start")
    while (true) {
        try {
            val exe = readLine()
            if (exe == null || exe == "") continue
            else if (exe == "pause") {
                control = !control
            }
            val execute = exe.split(' ')
            when (execute[0]) {
                "help" -> {
                    println("pause")
                    println("   - 일시정지")
                    println("Gn add [num]")
                    println("   - 귀신의 집 수익을 추가합니다.")
                    println("Gn sub [num]")
                    println("   - 귀신의 집 수익을 뺍니다.")
                    println("print history all")
                    println("   - 전체 히스토리를 포함하는 파일을 출력합니다.")
                    println("print history [ip_addr]")
                    println("   - 특정 ip_addr을 가진 사람의 히스토리를 포함하는 파일을 출력합니다.")
                    println("print allCoins ")
                    println("   - 시장에 얼마나 많은 코인이 있는지 출력합니다.")
                    println("print price")
                    println("   - 현재 가격을 출력합니다.")
                    println("print buySpeed")
                    println("   - 1분 사이의 총 사람들의 코인 구매량을 출력합니다.")
                    println("print sellSpeed")
                    println("   - 1분 사이의 총 사람들의 코인 판매량을 출력합니다.")
                }

                "Gn" -> {
                    when (execute[1]) {
                        "add" -> {
                            println("귀신의 집 수익 증가 : $gn -> ${gn + execute[2].toInt()}")
                            gn += execute[2].toInt()
                        }

                        "sub" -> {
                            println("귀신의 집 수익 감소 : $gn -> ${gn - execute[2].toInt()}")
                            gn -= execute[2].toInt()
                        }

                        else -> {
                            println("그래서 Gn을 올릴 거냐 내릴 거냐")
                        }
                    }
                }

                "print" -> {
                    when (execute[1]) {
                        "history" -> {
                            val path = "historyView.txt"
                            try {
                                Files.write(Paths.get(path), "".toByteArray(), StandardOpenOption.CREATE)
                                Files.deleteIfExists(Paths.get(path))
                            } catch (e: IOException) {
                                println("error")
                                continue
                            }
                            if (execute[2] == "all") {
                                Files.write(Paths.get(path), run {
                                    var x = ""
                                    for (k in history.keys) {
                                        var buy = 0
                                        var sell = 0
                                        for (h in history[k]!!) {
                                            if (h.ipAddr == "buy") {
                                                buy += h.amount
                                            } else if (h.ipAddr == "sell") {
                                                sell += h.amount
                                            }
                                        }
                                        x += "$k : buy=$buy, sell=$sell\n"
                                    }
                                    x
                                }.toByteArray(), StandardOpenOption.CREATE)
                                println("파일 생성 완료")
                            } else if (history[execute[2]] == null) {
                                println("no person")
                            } else {
                                Files.write(Paths.get(path), run {
                                    var x = ""
                                    for (i in history[execute[2]]!!) {
                                        x += "${i.ipAddr} : ${i.amount} (${i.time})\n"
                                    }
                                    x
                                }.toByteArray(), StandardOpenOption.CREATE)
                                println("파일 생성 완료")
                            }
                        }

                        "allCoins" -> {
                            println(inSijang)
                        }

                        "price" -> {
                            println(Stock.price)
                        }

                        "buySpeed" -> {
                            println(buySpeed)
                        }

                        "sellSpeed" -> {
                            println(sellSpeed)
                        }

                        "revenue" -> {
                            println(money)
                        }

                        else -> {
                            println("뭘 출력하라고")
                        }
                    }
                }

                else -> {
                    println("wrong command")
                }
            }
        } catch (e: Exception) {
            println("wrong command")
        }
    }
}

data class History(val time: LocalDateTime, val amount: Int, val ipAddr: String) {}


val history = HashMap<String, MutableList<History>>()
// History.ipaddr에 buy, sell 저장
// key에 ipaddr 저장


object Stock {
    var price: BigInteger = parameters.initialPrice.toBigInteger()
    var priceBefore: Int = parameters.initialPrice
    var outRevenue = 1000
    var revenue = 0

    /*
    fun update(buy: Int, sell: Int, time: Int, gnAdd: Int): Int {
        price = run {
            val tmp1: Double =
                (1.001 + 0.00001 * (buy - sell).toDouble() * price / revenue) * (1 / (1 + 1.00396.pow((time.toDouble().pow(0.02)))) + 0.5)
            val tmp2: Double = priceBefore.toDouble() * gnAdd / outRevenue / 20
            val newPrice: Double = (price.toDouble() * tmp1 + tmp2) * ((revenue + (buy - sell) * price) / revenue)
            priceBefore = price
            revenue += (sell - buy) * price
            outRevenue += gnAdd
            println(newPrice)
            newPrice.toInt()
        }
        return price
    }
     */

    fun update(buy: Int, sell: Int, inSijang: Int) {
        print("[$price, ")
        val k1 = 1.0
        val k2 = 25.727
        val newPrice: BigInteger = exp(((buy.toDouble()/2 - sell.toDouble()).toDouble() / 10 + k2) / (exp(k1 - inSijang / 200) + 1)).toInt().toBigInteger()
        print("$inSijang, ")
        price += (newPrice - price) / (3.toBigInteger()) - 3.toBigInteger()
    }
}