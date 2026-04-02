package io.github.romanvht.byedpi.library.data

/**
 * Default bypass strategies for ByeDPI
 */
object DefaultStrategies {
    
    /**
     * All default strategies
     */
    val ALL: List<Strategy> = listOf(
        Strategy("-f-200 -Qr -s3:5+sm -a1 -As -d1 -s4+sm -s8+sh -f-300 -d6+sh -a1 -At,r,s -o2 -f-30 -As -r5 -Mh -r6+sh -f-250 -s2:7+s -s3:6+sm -a1 -At,r,s -s3:5+sm -s6+s -s7:9+s -q30+sm -a1", "Complex Mix 1", "Mixed fragmentation and tampering strategy"),
        Strategy("-d1 -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -r1+s -S -a1 -As -d1 -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -d35+s -S -a1", "Incremental Delay", "Incremental delay and split strategy"),
        Strategy("-q2 -s2 -s3+s -r3 -s4 -r4 -s5+s -r5+s -s6 -s7+s -r8 -s9+s -Qr -Mh,d,r -a1 -At,r -s2+s -r2 -d2 -s3 -r3 -r4 -s4 -d5+s -r5 -d6 -s7+s -d7 -a1", "Split & Repeat", "Split and repeat combination"),
        Strategy("-o1 -d1 -a1 -At,r,s -s1 -d1 -s5+s -s10+s -s15+s -s20+s -r1+s -S -a1 -As -s1 -d1 -s5+s -s10+s -s15+s -s20+s -S -a1", "Offset & Delay", "Offset with delay strategy"),
        Strategy("-n {sni} -Qr -f-204 -s1:5+sm -a1 -As -d1 -s3+s -s5+s -q7 -a1 -As -o2 -f-43 -a1 -As -r5 -Mh -s1:5+s -s3:7+sm -a1", "SNI Fragmentation", "SNI-based fragmentation strategy"),
        Strategy("-n {sni} -Qr -f-205 -a1 -As -s1:3+sm -a1 -As -s5:8+sm -a1 -As -d3 -q7 -o2 -f-43 -f-85 -f-165 -r5 -Mh -a1", "SNI Multi-Frag", "SNI with multiple fragment sizes"),
        Strategy("-d1+s -s50+s -a1 -As -f20 -r2+s -a1 -At -d2 -s1+s -s5+s -s10+s -s15+s -s25+s -s35+s -s50+s -s60+s -a1", "Large Delay", "Large delay values strategy"),
        Strategy("-o1 -a1 -At,r,s -f-1 -a1 -At,r,s -d1:11+sm -S -a1 -At,r,s -n {sni} -Qr -f1 -d1:11+sm -s1:11+sm -S -a1", "Offset Frag", "Offset with fragmentation"),
        Strategy("-d1 -s1 -q1 -Y -a1 -Ar -s5 -o1+s -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -a1", "Desync Mix", "Desync mixing strategy"),
        Strategy("-f1+nme -t6 -a1 -As -n {sni} -Qr -s1:6+sm -a1 -As -s5:12+sm -a1 -As -d3 -q7 -r6 -Mh -a1", "NME Fragment", "NME fragmentation strategy"),
        Strategy("-s1 -o1 -a1 -Y -Ar -s5 -o1+s -a1 -At -f-1 -r1+s -a1 -As -s1 -o1+s -s-1 -a1", "Split Offset", "Split with offset"),
        Strategy("-s1 -d1 -a1 -Y -Ar -d5 -o1+s -a1 -At -f-1 -r1+s -a1 -As -d1 -o1+s -s-1 -a1", "Split Delay", "Split with delay"),
        Strategy("-d1 -s1+s -d3+s -s6+s -d9+s -s12+s -d15+s -s20+s -d25+s -s30+s -d35+s -a1", "Progressive", "Progressive delay and split"),
        Strategy("-s1 -q1 -a1 -Y -Ar -a1 -s5 -o2 -At -f-1 -r1+s -a1 -As -s1 -o1+s -s-1 -a1", "Queue Split", "Queue and split"),
        Strategy("-s1 -q1 -a1 -Ar -s5 -o1+s -a1 -At -f-1 -r1+s -a1 -As -s1 -o1+s -s-1 -a1", "Queue Offset", "Queue with offset"),
        Strategy("-s1 -q1 -a1 -Ar -s5 -o2 -a1 -At -f-1 -r1+s -a1 -As -s1 -o1+s -s-1 -a1", "Queue Offset 2", "Queue with offset variant"),
        Strategy("-d1 -s1+s -d1+s -s3+s -d6+s -s12+s -d14+s -s20+s -d24+s -s30+s -a1", "Mixed Inc", "Mixed incremental strategy"),
        Strategy("-s1 -q1 -a1 -Y -At -a1 -S -f-1 -r1+s -a1 -As -d1+s -O1 -s29+s -a1", "Y-At Split", "Y-At combination"),
        Strategy("-o1 -a1 -At,r,s -f-1 -a1 -Ar,s -o1 -a1 -At -r1+s -f-1 -t6 -a1", "Full Tamper", "Full tampering strategy"),
        Strategy("-d1 -s1+s -s3+s -s6+s -s9+s -s12+s -s15+s -s20+s -s30+s -a1", "Simple Inc", "Simple incremental"),
        Strategy("-f1 -t5 -n {sni} -q3+h -Qr -f2 -q1 -r1+s -t15 -q1 -o2 -a1", "Complex SNI", "Complex SNI strategy"),
        Strategy("-n {sni} -d2:5:2+h -f-3 -r2+sm -o2 -o50+s -r2+s -f-4 -a1", "SNI Delay", "SNI with delay"),
        Strategy("-r-1+s -o20+sm -s3:7+sm -d5:3+sm -f300+s -Qr -Y -f-1 -a1", "Negative Repeat", "Negative repeat strategy"),
        Strategy("-f-1 -Qr -s1+sm -d3+s -s5+sm -o2 -a1 -As -r1+s -d8+s -a1", "Simple Frag", "Simple fragmentation"),
        Strategy("-s25 -r5+s -s25+s -a1 -At,r,s -s50 -r5+s -s50+s -a1", "Large Split", "Large split values"),
        Strategy("-o1 -r-5+se -a1 -At,r,s -d1 -n {sni} -Qr -f-1 -a1", "SNI Offset", "SNI with offset"),
        Strategy("-s1 -d1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1", "Basic Mix", "Basic mixing strategy"),
        Strategy("-s1 -q1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1", "Queue Mix", "Queue mixing strategy"),
        Strategy("-s1 -o1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1", "Offset Mix", "Offset mixing strategy"),
        Strategy("-n {sni} -Qr -f6+nr -d2 -d11 -f9+hm -o3 -t7 -a1", "SNI NR", "SNI with NR fragmentation"),
        Strategy("-s1 -d1 -o1 -a1 -Ar -o3 -a1 -At -f-1 -r1+s -a1", "Triple Mix", "Triple mixing strategy"),
        Strategy("-d9+s -q20+s -s25+s -t5 -a1 -At,r,s -r1+h -a1", "Heavy Delay", "Heavy delay strategy"),
        Strategy("-q1+s -s29+s -s30+s -s14+s -o5+s -f-1 -S -a1", "Complex Queue", "Complex queue strategy"),
        Strategy("-d1 -s1+s -r1+s -e1 -m1 -o1+s -f-1 -t2 -a1", "Extra Mix", "Extra mixing strategy"),
        Strategy("-d9+s -q20+s -s 25+s -t5 -At,r,s -r1+h -a1", "Heavy Delay 2", "Heavy delay variant"),
        Strategy("-s1 -o1 -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1", "Simple Offset", "Simple offset strategy"),
        Strategy("-d1 -o1 -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1", "Delay Offset", "Delay with offset"),
        Strategy("-d1 -s4 -d8 -s1+s -d5+s -s10+s -d20+s -a1", "Alt Delay", "Alternative delay strategy"),
        Strategy("-n {sni} -Qr -d5+sm -f3+sm -o2 -t4 -a1", "SNI DM", "SNI with delay and fragment"),
        Strategy("-o1 -a1 -Ar -q1 -a1 -At -f-1 -r1+s -a1", "Queue Tamper", "Queue tampering strategy"),
        Strategy("-q1 -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1", "Q1 Tamper", "Q1 tampering strategy"),
        Strategy("-s1 -q1 -Y -a1 -At,r,s -f-1 -r1+s -a1", "Y-Queue", "Y with queue"),
        Strategy("-s1 -o1 -Y -a1 -At,r,s -f-1 -r1+s -a1", "Y-Offset", "Y with offset"),
        Strategy("-s1 -d1 -Y -a1 -At,r,s -f-1 -r1+s -a1", "Y-Delay", "Y with delay"),
        Strategy("-n {sni} -Qr -f209 -s1+sm -R1-3 -a1", "SNI R-Range", "SNI with R range"),
        Strategy("-s1 -q1 -a1 -At,r,s -f-1 -r1+s -a1", "Q1 At", "Q1 with At tampering"),
        Strategy("-s1 -o1 -a1 -At,r,s -f-1 -r1+s -a1", "O1 At", "O1 with At tampering"),
        Strategy("-s1 -d1 -a1 -At,r,s -f-1 -r1+s -a1", "D1 At", "D1 with At tampering"),
        Strategy("-s4+sn -r9+s -Qr -n {sni} -S -a1", "SNI SN", "SNI with SN split"),
        Strategy("-n {sni} -Qr -m0x02 -f-1 -d7 -a1", "SNI Hex", "SNI with hex modification"),
        Strategy("-o1 -d1 -r1+s -S -s1+s -d3+s -a1", "O1D1 S", "O1D1 with S"),
        Strategy("-d1 -r1+s -f-1 -S -t8 -o3+s -a1", "D1 Frag", "D1 with fragmentation"),
        Strategy("-q1+s -s29+s -o5+s -f-1 -S -a1", "Q1 Large", "Q1 with large values"),
        Strategy("-d1 -s1+s -r1+s -f-1 -t8 -a1", "D1S1 Frag", "D1S1 with fragmentation"),
        Strategy("-o1 -a1 -An -f1+nme -t6 -a1", "NME An", "NME with An tampering"),
        Strategy("-n {sni} -Qr -f-1 -r1+s -a1", "Simple SNI", "Simple SNI fragmentation"),
        Strategy("-n {sni} -Qr -d1:3 -f-1 -a1", "SNI D1:3", "SNI with D1:3"),
        Strategy("-s1 -d3+s -a1 -At -r1+s -a1", "S1D3", "S1 with D3"),
        Strategy("-o1 -a1 -At,r,s -d1 -a1", "O1 D1", "O1 with D1"),
        Strategy("-q1 -a1 -At,r,s -d1 -a1", "Q1 D1", "Q1 with D1"),
        Strategy("-n {sni} -Qr -d1 -f-1", "Minimal SNI", "Minimal SNI strategy"),
        Strategy("-d1+s -o2 -s5 -r5 -a1", "D1+ O2", "D1+ with O2"),
        Strategy("-r8 -o2 -s7 -q4+s -a1", "R8 O2", "R8 with O2"),
        Strategy("-d6+s -q4+hm -o2 -a1", "D6 HM", "D6 with HM"),
        Strategy("-s1+s -d3+s -a1", "Minimal Inc", "Minimal incremental"),
        Strategy("-s1 -f-1 -S -a1", "Minimal Frag", "Minimal fragmentation"),
        Strategy("-o1+s -d3+s -a1", "O1+ D3", "O1+ with D3"),
        Strategy("-o1 -s4 -s6 -a1", "O1 S4 S6", "O1 with S4 and S6"),
        Strategy("-q1 -r25+s -a1", "Q1 R25", "Q1 with R25"),
        Strategy("-d1 -s3+s -a1", "D1 S3", "D1 with S3"),
        Strategy("-o3 -d7 -a1", "O3 D7", "O3 with D7"),
        Strategy("-d7 -s2 -a1", "D7 S2", "D7 with S2")
    )
    
    /**
     * Get strategies by category
     */
    fun getByCategory(category: StrategyCategory): List<Strategy> {
        return when (category) {
            StrategyCategory.SIMPLE -> ALL.filter { it.name.contains("Simple", ignoreCase = true) || it.command.length < 50 }
            StrategyCategory.COMPLEX -> ALL.filter { it.command.length > 80 }
            StrategyCategory.SNI_BASED -> ALL.filter { it.command.contains("{sni}") }
            StrategyCategory.FRAGMENTATION -> ALL.filter { it.command.contains("-f") }
            StrategyCategory.DELAY -> ALL.filter { it.command.contains("-d") }
            StrategyCategory.ALL_CATEGORIES -> ALL
        }
    }
    
    /**
     * Parse strategies from a custom string (one strategy per line)
     */
    fun fromString(content: String): List<Strategy> {
        return content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapIndexed { index, command ->
                Strategy(
                    command = command,
                    name = "Strategy ${index + 1}",
                    description = "Custom strategy"
                )
            }
    }
}

/**
 * Strategy categories for filtering
 */
enum class StrategyCategory {
    SIMPLE,
    COMPLEX,
    SNI_BASED,
    FRAGMENTATION,
    DELAY,
    ALL_CATEGORIES
}
