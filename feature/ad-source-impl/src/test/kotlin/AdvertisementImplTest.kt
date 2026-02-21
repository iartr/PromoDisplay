import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.offerfactory.promodisplay.ad.source.impl.AdvertisementImpl
import ru.offerfactory.promodisplay.ad.source.impl.data.local.AdStorageManager
import ru.offerfactory.promodisplay.ad.source.impl.data.remote.AdFileDownloader
import ru.offerfactory.promodisplay.settings.ConfigManager
import ru.offerfactory.promodisplay.settings.domain.model.AdItem
import ru.offerfactory.promodisplay.settings.domain.model.ConfigModel
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AdvertisementImplTest {

    private lateinit var dispatcher: TestDispatcher
    private lateinit var advertisement: AdvertisementImpl
    private lateinit var configManager: ConfigManager
    private lateinit var storage: AdStorageManager
    private lateinit var downloader: AdFileDownloader

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)

        configManager = mockk()
        storage = mockk(relaxed = true)
        downloader = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `sorts by priority and repeats in cycle if files exist`() = runTest {

        val configFlow = MutableStateFlow(
            ConfigModel(
                version = 1,
                generatedAt = Instant.now(),
                serverTime = Instant.now(),
                pollInterval = 60,
                items = listOf(
                    remote("a", 10, 2),
                    remote("b", 30, 1),
                    remote("c", 20, 3)
                )
            )
        )

        coEvery { configManager.getConfig() } returns configFlow

        advertisement = AdvertisementImpl(
            configManager = configManager,
            downloader = downloader,
            storage = storage,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        configFlow.value.items.forEach { remote ->
            val file = mockk<File> {
                every { exists() } returns true
                every { length() } returns remote.sizeBytes
                every { absolutePath } returns "/mock/${remote.id}.mp4"
            }
            every { storage.getFileForId(remote.id) } returns file
        }

        every { storage.cleanupOldFiles(any()) } just Runs

        advanceUntilIdle()
        advanceUntilIdle()

        val clips = advertisement.getClips().first()

        assertEquals(
            listOf("b", "c", "c", "c", "a", "a"),
            clips.map { it.id }
        )

        assertEquals(6, clips.size, "Общее количество клипов должно быть 6")
        assertEquals(1, clips.count { it.id == "b" })
        assertEquals(3, clips.count { it.id == "c" })
        assertEquals(2, clips.count { it.id == "a" })
    }


    @Test
    fun `the first available file must be the first in the clip queue`() = runTest {

        val items = listOf(
            remote("low", 10, 1),
            remote("high", 80, 1),
            remote("medium", 40, 2)
        )

        val configFlow = MutableStateFlow(
            ConfigModel(
                version = 1,
                generatedAt = Instant.now(),
                serverTime = Instant.now(),
                pollInterval = 60,
                items = items
            )
        )

        coEvery { configManager.getConfig() } returns configFlow


        items.forEach { item ->
            val file = mockk<File> {
                every { exists() } returns true
                every { length() } returns item.sizeBytes
                every { absolutePath } returns "/ads/${item.id}.mp4"
            }
            every { storage.getFileForId(item.id) } returns file
        }

        advertisement = AdvertisementImpl(
            configManager = configManager,
            downloader = downloader,
            storage = storage,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        advanceUntilIdle()
        advanceUntilIdle()

        val clips = advertisement.getClips().first()

        assertEquals(
            listOf("high", "medium", "medium", "low"),
            clips.map { it.id },
            "Ожидаемый порядок: высокий приоритет → повторяющийся средний → низкий"
        )

        assertEquals(
            "high",
            clips.firstOrNull()?.id,
            "Первый клип, который уйдёт плееру — самый приоритетный"
        )
    }

    @Test
    fun `the queue of clips is passed to the player and maintains the expected cycle length`() = runTest {

        val items = listOf(
            remote("A", 90, 1),
            remote("B", 60, 3),
            remote("C", 30, 2)
        )

        val configFlow = MutableStateFlow(
            ConfigModel(
                version = 1,
                generatedAt = Instant.now(),
                serverTime = Instant.now(),
                pollInterval = 60,
                items = items
            )
        )

        coEvery { configManager.getConfig() } returns configFlow

        advertisement = AdvertisementImpl(
            configManager = configManager,
            downloader = downloader,
            storage = storage,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        items.forEach { item ->
            every { storage.getFileForId(item.id) } returns mockk<File> {
                every { exists() } returns true
                every { length() } returns item.sizeBytes
                every { absolutePath } returns "/mock/${item.id}.mp4"
            }
        }

        advanceUntilIdle()
        advanceUntilIdle()

        val clips = advertisement.getClips().first()

        assertTrue(clips.isNotEmpty(), "Очередь клипов для плеера не должна быть пустой")

        val expectedTotalRepeats = items.sumOf { it.repeatInCycle }
        assertEquals(
            expectedTotalRepeats,
            clips.size,
            "Длина цикла должна равняться сумме всех repeatInCycle"
        )

        assertEquals(1, clips.count { it.id == "A" })
        assertEquals(3, clips.count { it.id == "B" })
        assertEquals(2, clips.count { it.id == "C" })

        assertEquals("A", clips[0].id, "Первым должен идти самый приоритетный")
        assertTrue(
            clips.subList(0, 4).any { it.id == "B" },
            "Элементы B должны появляться в первой части цикла"
        )

        val secondEmission = advertisement.getClips().first()
        assertEquals(
            clips,
            secondEmission,
            "Повторный вызов first() должен давать ту же очередь (если нет изменений)"
        )
    }

    @Test
    fun `If there are no files, it returns an empty list`() = runTest {
        val configFlow = MutableStateFlow(
            ConfigModel(
                version = 1,
                generatedAt = Instant.now(),
                serverTime = Instant.now(),
                pollInterval = 60,
                items = listOf(
                    remote("a", 10, 2),
                    remote("b", 30, 1)
                )
            )
        )

        coEvery { configManager.getConfig() } returns configFlow

        advertisement = AdvertisementImpl(
            configManager = configManager,
            downloader = downloader,
            storage = storage,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        configFlow.value.items.forEach { remote ->
            val file = mockk<File> {
                every { exists() } returns false
                every { length() } returns 0L
                every { absolutePath } returns "/invalid/${remote.id}"
            }
            every { storage.getFileForId(remote.id) } returns file
        }

        advanceUntilIdle()
        advanceUntilIdle()

        val clips = advertisement.getClips().first()

        assertEquals(emptyList(), clips, "Если нет доступных файлов → список должен быть пустым")
    }

    @Test
    fun `one element with repeatInCycle = 5 should be repeated 5 times`() = runTest {
        val configFlow = MutableStateFlow(
            ConfigModel(
                version = 1,
                generatedAt = Instant.now(),
                serverTime = Instant.now(),
                pollInterval = 60,
                items = listOf(
                    remote("solo", 50, 5)
                )
            )
        )

        coEvery { configManager.getConfig() } returns configFlow

        advertisement = AdvertisementImpl(
            configManager = configManager,
            downloader = downloader,
            storage = storage,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        val file = mockk<File> {
            every { exists() } returns true
            every { length() } returns 100L
            every { absolutePath } returns "/mock/solo.mp4"
        }
        every { storage.getFileForId("solo") } returns file

        advanceUntilIdle()
        advanceUntilIdle()

        val clips = advertisement.getClips().first()

        assertEquals(5, clips.size)
        assertTrue(clips.all { it.id == "solo" }, "Все клипы должны быть одним и тем же элементом")
        assertEquals(
            listOf("solo", "solo", "solo", "solo", "solo"),
            clips.map { it.id }
        )
    }

    @Test
    fun `When changing the config, the list of clips should be updated`() = runTest {
        val initialItems = listOf(remote("old", 10, 1))
        val newItems = listOf(remote("new1", 20, 2), remote("new2", 30, 1))

        val configFlow = MutableStateFlow(
            ConfigModel(
                version = 1,
                generatedAt = Instant.now(),
                serverTime = Instant.now(),
                pollInterval = 60,
                items = initialItems
            )
        )

        coEvery { configManager.getConfig() } returns configFlow

        advertisement = AdvertisementImpl(
            configManager = configManager,
            downloader = downloader,
            storage = storage,
            scope = CoroutineScope(dispatcher + SupervisorJob())
        )

        every { storage.getFileForId("old") } returns mockk<File> {
            every { exists() } returns true
            every { length() } returns 100L
            every { absolutePath } returns "/mock/old.mp4"
        }

        advanceUntilIdle()
        advanceUntilIdle()

        val initialClips = advertisement.getClips().first()
        assertEquals(listOf("old"), initialClips.map { it.id })

        configFlow.value = ConfigModel(
            version = 2,
            generatedAt = Instant.now(),
            serverTime = Instant.now(),
            pollInterval = 60,
            items = newItems
        )

        newItems.forEach { item ->
            every { storage.getFileForId(item.id) } returns mockk<File> {
                every { exists() } returns true
                every { length() } returns item.sizeBytes
                every { absolutePath } returns "/mock/${item.id}.mp4"
            }
        }

        advanceUntilIdle()
        advanceUntilIdle()

        val updatedClips = advertisement.getClips().first()

        assertEquals(
            listOf("new2", "new1", "new1"),
            updatedClips.map { it.id },
            "После обновления конфига должен отобразиться новый порядок и новые повторения"
        )
    }

    @Test
    fun `elements with the same priority must maintain the relative order from the config`() =
        runTest {
            val configFlow = MutableStateFlow(
                ConfigModel(
                    version = 1,
                    generatedAt = Instant.now(),
                    serverTime = Instant.now(),
                    pollInterval = 60,
                    items = listOf(
                        remote("x", 100, 1),
                        remote("y", 100, 1),
                        remote("z", 50, 1)
                    )
                )
            )

            coEvery { configManager.getConfig() } returns configFlow

            advertisement = AdvertisementImpl(
                configManager = configManager,
                downloader = downloader,
                storage = storage,
                scope = CoroutineScope(dispatcher + SupervisorJob())
            )

            configFlow.value.items.forEach { item ->
                every { storage.getFileForId(item.id) } returns mockk<File> {
                    every { exists() } returns true
                    every { length() } returns item.sizeBytes
                    every { absolutePath } returns "/mock/${item.id}.mp4"
                }
            }

            advanceUntilIdle()
            advanceUntilIdle()

            val clips = advertisement.getClips().first()

            assertEquals(listOf("x", "y", "z"), clips.map { it.id })
        }


    private fun remote(
        id: String,
        priority: Int,
        repeat: Int
    ) = AdItem(
        id = id,
        priority = priority,
        repeatInCycle = repeat,
        sizeBytes = 100L,
        sha256 = "00",
        mimeType = "",
        durationMs = 123L,
        updatedAt = Instant.now(),
        supportsRange = true
    )
}