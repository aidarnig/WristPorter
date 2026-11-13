package io.github.aidarnig.wristporter.w0

import org.junit.Assert.*
import org.junit.Test

class ProbeSummariesTest {
    @Test fun ancsPacketIsSummarizedWithoutUid() {
        val packet = byteArrayOf(0, 0, 6, 1, 0x12, 0x34, 0x56, 0x78)
        assertEquals("ANCS event=added category=6", ProbeSummaries.ancs(packet))
        assertFalse(ProbeSummaries.ancs(packet).contains("305419896"))
    }

    @Test fun malformedAncsPacketRemainsSafe() {
        assertEquals("ANCS malformed event (bytes=2)", ProbeSummaries.ancs(byteArrayOf(1, 2)))
    }

    @Test fun amsEntityIgnoresSensitiveMetadata() {
        val payload = byteArrayOf(2, 2, 1) + "private track title".toByteArray()
        assertEquals("AMS metadata event entity=2 attribute=2 truncated=true", ProbeSummaries.amsEntity(payload))
    }

    @Test fun emptyAmsCommandListIsNotFabricated() {
        assertEquals("AMS advertised command count=0 (contents omitted)", ProbeSummaries.amsCommands(byteArrayOf()))
    }
}
