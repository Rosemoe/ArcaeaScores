package io.github.rosemoe.arcaeaScores.arc

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SongListTest {

    private val titles = SongList(
        """
            {
              "songs": [
                {
                  "id": "example",
                  "title_localized": { "en": "Song title" },
                  "remote_dl": true,
                  "difficulties": [
                    { "ratingClass": 2, "rating": 10, "ratingPlus": true },
                    {
                      "ratingClass": 3,
                      "title_localized": { "en": "Chart title" },
                      "jacketOverride": true
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

    @Test
    fun queryForChartInfo_returnsRatingAndRatingPlus() {
        val chartInfo = titles.queryForChartInfo("example", 2)

        assertEquals(10, chartInfo?.rating)
        assertEquals(true, chartInfo?.ratingPlus)
        assertEquals("10+", chartInfo?.displayRating)
    }

    @Test
    fun queryForJacketPaths_usesDownloadDirectoryAndChartOverride() {
        val paths = titles.queryForJacketPaths(File("songs"), "example", 3)

        assertEquals(
            listOf(
                File("songs/dl_example/1080_3_256.jpg"),
                File("songs/dl_example/3_256.jpg")
            ),
            paths
        )
    }

    @Test
    fun queryForJacketPaths_usesBaseJacketWithoutOverride() {
        val paths = titles.queryForJacketPaths(File("songs"), "example", 2)

        assertEquals(
            listOf(
                File("songs/dl_example/1080_base_256.jpg"),
                File("songs/dl_example/base_256.jpg")
            ),
            paths
        )
    }
}
