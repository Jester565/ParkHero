package com.dis.ajcra.distest2.voice

class StringComp {
    companion object {
        fun compareStrings(str1: String, str2: String): Double {
            var pairs1 = wordLetterPairs(str1.toUpperCase())
            var pairs2 = wordLetterPairs(str2.toUpperCase())
            var intersection = 0
            var union = pairs1.size + pairs2.size
            for (p1 in pairs1) {
                var j = 0
                while (j < pairs2.size) {
                    var p2 = pairs2.get(j)
                    if (p1 == p2) {
                        intersection++
                        pairs2.removeAt(j)
                        break
                    }
                    j++
                }
            }
            return (2.0 * intersection)/union
        }

        fun wordLetterPairs(str: String): ArrayList<String> {
            var allPairs = ArrayList<String>()
            var words = str.split("\\s")
            for (word in words) {
                var pairsInWord = letterPairs(word)
                for (p in pairsInWord) {
                    allPairs.add(p)
                }
            }
            return allPairs
        }

        fun letterPairs(str: String): Array<String> {
            var numPairs = str.length - 1
            var pairs = Array<String>(numPairs, { i ->
                str.substring(i, i+ 2)
            })
            return pairs
        }
    }
}