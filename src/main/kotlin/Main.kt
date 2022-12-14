import java.time.LocalDateTime
import kotlinx.coroutines.*

fun sleep(time:Int){
    val nextTime = LocalDateTime.now().plusSeconds(time.toLong())
    while (nextTime >= LocalDateTime.now()) {}
}
fun sleep(time:Double){
    val nextTime = LocalDateTime.now().plusNanos((time*1000000000).toLong())
    while (nextTime >= LocalDateTime.now()) {}
}

object parameters{
    val buyRate:Double=3.0                  // 가격에 구매량이 반영되는 비율
    val sellRate:Double=3.0                 // 가격에 판매량이 반영되는 비율
    val bias:Double=(-10).toDouble()        // 가격의 편향
    val buyLife:Int=10                      // 이 시간을 지난 구매량은 반영되지 않음
    val sellLife:Int=10                     // 이 시간을 지난 판매량은 반영되지 않음
    val initialPrice:Int=1000               // 초기 가격
    val buyFast:Double=0.3                  // 자동 구매 주기 (실제 상황에서는 뺄 예정)
    val sellFast:Double=1.toDouble()        // 자동 판매 주기 (실제 상황에서는 뺄 예정)
    val showFast:Double=3.toDouble()        // 보여주는 주기
}












fun main(args: Array<String>) {     // input : userid, password, stockid, amount
    var timeControl = LocalDateTime.now()
    var nextTime = timeControl.plusSeconds(10)
    var sellHistory:MutableList<History> = mutableListOf<History>()
    var buyHistory:MutableList<History> = mutableListOf<History>()







    val buyRoutine_test = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            if (true) {
                val t = mutableListOf<History>(History(LocalDateTime.now().plusSeconds(parameters.buyLife.toLong()), 1))
                t.addAll(buyHistory)
                buyHistory = t
            }
            sleep(parameters.buyFast)
        }
    }
    val sellRoutine_test = CoroutineScope(Dispatchers.IO).launch {
        while(true) {
            if (true) {
                val t =
                    mutableListOf<History>(History(LocalDateTime.now().plusSeconds(parameters.sellLife.toLong()), 1))
                t.addAll(sellHistory)
                sellHistory = t
            }
            sleep(parameters.sellFast)
        }
    }








    val checkHistory = CoroutineScope(Dispatchers.IO).launch {
        while(true) {
            if (buyHistory.size != 0) if (buyHistory.last().time < LocalDateTime.now()) buyHistory.removeAt(buyHistory.size - 1)
            if (sellHistory.size != 0) if (sellHistory.last().time < LocalDateTime.now()) sellHistory.removeAt(sellHistory.size - 1)
        }
    }
    val price:Int=parameters.initialPrice
    val update = CoroutineScope(Dispatchers.IO).launch {
        while(true) {
            val x=Stock.update(buyHistory.size, sellHistory.size)
            sleep(1)
        }
    }

    println("====== START ======")
    println("구매량, 판매량, 가격")
    while (true) {
        println(buyHistory.size.toString()+", "+sellHistory.size.toString()+" -> "+Stock.price)
        sleep(parameters.showFast)
    }
}



















class History(val time:LocalDateTime, val amount:Int){}



object Stock {
    val name: String = "나히다 주식"
    var price: Int = parameters.initialPrice

    fun update(buyAmount:Int, sellAmount:Int):Int{
        price = run{
            price+(parameters.buyRate*buyAmount + parameters.sellRate*sellAmount + parameters.bias).toInt()
        }
        return price
    }
}