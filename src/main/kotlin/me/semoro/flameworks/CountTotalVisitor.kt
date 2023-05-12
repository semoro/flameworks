package me.semoro.flameworks

class CountTotalVisitor(val values: IntArray) : NodeVisitor() {
    var totalNodes = 0
    var totalValue = 0
    override fun enter(idx: Int) {
        totalNodes++
        totalValue += values[idx]
    }

    override fun exit(idx: Int) {
    }

    override fun toString(): String {
        return "CountTotalVisitor(totalNodes=$totalNodes, totalValue=$totalValue)"
    }

}