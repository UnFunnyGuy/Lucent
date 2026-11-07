package app.lucent.firefly.core.common

import app.cash.turbine.test
import app.lucent.firefly.core.common.base.BaseViewModel
import app.lucent.firefly.core.common.base.UIAction
import app.lucent.firefly.core.common.base.UIEvent
import app.lucent.firefly.core.common.base.UIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

data class TestState(val count: Int = 0) : UIState
sealed interface TestEvent : UIEvent {
    data class Notification(val message: String) : TestEvent
}
sealed interface TestAction : UIAction {
    data object Increment : TestAction
    data class SendMessage(val text: String) : TestAction
}

class TestViewModel : BaseViewModel<TestState, TestEvent, TestAction>() {
    override fun initialState(): TestState = TestState(count = 0)

    override fun onActionEvent(action: TestAction) {
        when (action) {
            TestAction.Increment -> update { copy(count = count + 1) }
            is TestAction.SendMessage -> sendEvent(TestEvent.Notification(action.text))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val viewModel = TestViewModel()
        assertEquals(0, viewModel.uiState.value.count)
    }

    @Test
    fun testUpdateStateOnAction() {
        val viewModel = TestViewModel()
        viewModel.onAction(TestAction.Increment)
        assertEquals(1, viewModel.uiState.value.count)
    }

    @Test
    fun testSendEvent() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.uiEvent.test {
            viewModel.onAction(TestAction.SendMessage("Hello World"))
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertEquals(TestEvent.Notification("Hello World"), event)
        }
    }
}
