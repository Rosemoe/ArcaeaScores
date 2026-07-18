package io.github.rosemoe.arcaeaScores.arc

import org.junit.Assert.assertEquals
import org.junit.Test

class SongListTest {

    private val titles = ArcaeaTitles(
        """
            {
              "songs": [
                {
                  "id": "example",
                  "title_localized": { "en": "Song title" },
                  "difficulties": [
                    { "ratingClass": 2 },
                    {
                      "ratingClass": 3,
                      "title_localized": { "en": "Chart title" }
                    }
                  ]
                }
              ]
            }
        """.trimIndent().byteInputStream()
    )

    @Test
    fun queryForSongId_returnsSongTitle() {
        assertEquals("Song title", titles.queryForSongId("example"))
    }

    @Test
    fun queryForChart_prefersChartTitleAndFallsBackToSongTitle() {
        assertEquals("Chart title", titles.queryForChart("example", 3))
        assertEquals("Song title", titles.queryForChart("example", 2))
    }

    @Test
    fun queriesFallBackToSongIdWhenSongIsUnknown() {
        assertEquals("unknown", titles.queryForSongId("unknown"))
        assertEquals("unknown", titles.queryForChart("unknown", 3))
    }
}
