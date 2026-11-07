package app.lucent.firefly.core.common

import app.cash.turbine.test
import app.lucent.firefly.core.common.base.UIContentState
import app.lucent.firefly.core.common.base.UiText
import app.lucent.firefly.core.common.base.asUIContentState
import app.lucent.firefly.core.common.base.fold
import app.lucent.firefly.core.common.base.getOrElse
import app.lucent.firefly.core.common.base.getOrNull
import app.lucent.firefly.core.common.base.mapSuccess
import app.lucent.firefly.core.common.base.onError
import app.lucent.firefly.core.common.base.onLoading
import app.lucent.firefly.core.common.base.onNone
import app.lucent.firefly.core.common.base.onSuccess
import app.lucent.firefly.core.common.base.toUIContentState
import app.lucent.firefly.core.common.base.unwrap
import app.lucent.firefly.core.common.result.AppError
import app.lucent.firefly.core.common.result.Result
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UIContentStateTest {

    @Test
    fun successStatePropertiesAndAccessors() {
        val state: UIContentState<String> = UIContentState.Success("accounts")

        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertFalse(state.isNone)
        assertEquals("accounts", state.unwrap())
        assertEquals("accounts", state.getOrNull())
        assertEquals("accounts", state.getOrElse { "default" })
    }

    @Test
    fun loadingStateProperties() {
        val state: UIContentState<String> = UIContentState.Loading

        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertFalse(state.isNone)
        assertNull(state.getOrNull())
        assertEquals("fallback", state.getOrElse { "fallback" })
    }

    @Test
    fun noneStateProperties() {
        val state: UIContentState<String> = UIContentState.None

        assertTrue(state.isNone)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertNull(state.getOrNull())
    }

    @Test
    fun errorStateConstructorsAndProperties() {
        val errorFromUiText = UIContentState.Error(UiText.DynamicString("Error 1"))
        assertEquals("Error 1", (errorFromUiText.message as UiText.DynamicString).value)

        val errorFromString = UIContentState.Error("Error 2")
        assertEquals("Error 2", (errorFromString.message as UiText.DynamicString).value)

        val errorFromAppError = UIContentState.Error(AppError.ApiKeyError)
        assertEquals("API key error", (errorFromAppError.message as UiText.DynamicString).value)

        assertTrue(errorFromString.isError)
        assertFalse(errorFromString.isSuccess)
        assertFalse(errorFromString.isLoading)
        assertFalse(errorFromString.isNone)
    }

    @Test
    fun mapSuccessTransformsOnlySuccess() {
        val successState: UIContentState<Int> = UIContentState.Success(10)
        val mapped = successState.mapSuccess { it * 3 }

        assertTrue(mapped is UIContentState.Success)
        assertEquals(30, (mapped as UIContentState.Success).data)

        val loadingState: UIContentState<Int> = UIContentState.Loading
        val mappedLoading = loadingState.mapSuccess { it * 3 }
        assertEquals(UIContentState.Loading, mappedLoading)

        val noneState: UIContentState<Int> = UIContentState.None
        val mappedNone = noneState.mapSuccess { it * 3 }
        assertEquals(UIContentState.None, mappedNone)
    }

    @Test
    fun chainingCallbacksTriggerAppropriately() {
        var successData: String? = null
        var errorMessage: String? = null
        var loadingTriggered = false
        var noneTriggered = false

        UIContentState.Success("loaded")
            .onSuccess { successData = it }
            .onError { errorMessage = (it as? UiText.DynamicString)?.value }
            .onLoading { loadingTriggered = true }
            .onNone { noneTriggered = true }

        assertEquals("loaded", successData)
        assertNull(errorMessage)
        assertFalse(loadingTriggered)
        assertFalse(noneTriggered)

        UIContentState.None
            .onNone { noneTriggered = true }
        assertTrue(noneTriggered)
    }

    @Test
    fun foldHandlesAllBranches() {
        val success: UIContentState<Int> = UIContentState.Success(42)
        val foldedSuccess = success.fold(
            onSuccess = { "Value: $it" },
            onError = { "Error: ${(it as UiText.DynamicString).value}" },
            onLoading = { "Loading" },
            onNone = { "None" },
        )
        assertEquals("Value: 42", foldedSuccess)

        val loading: UIContentState<Int> = UIContentState.Loading
        val foldedLoading = loading.fold(
            onSuccess = { "Value: $it" },
            onError = { "Error" },
            onLoading = { "Loading" },
            onNone = { "None" },
        )
        assertEquals("Loading", foldedLoading)
    }

    @Test
    fun resultToUIContentStateMapping() {
        val successResult = Result.success(listOf("item1", "item2"))
        val contentState = successResult.toUIContentState()

        assertTrue(contentState is UIContentState.Success)
        assertEquals(listOf("item1", "item2"), (contentState as UIContentState.Success).data)

        val errorResult: Result<String> = Result.error(AppError.ApiKeyError)
        val errorContentState = errorResult.toUIContentState()

        assertTrue(errorContentState is UIContentState.Error)
        val message = (errorContentState as UIContentState.Error).message
        assertTrue(message is UiText.DynamicString)
        assertEquals("API key error", (message as UiText.DynamicString).value)
    }

    @Test
    fun flowAsUIContentStateEmitsLoadingThenSuccess() = runTest {
        val flow = flowOf("item")

        flow.asUIContentState().test {
            assertEquals(UIContentState.Loading, awaitItem())
            assertEquals(UIContentState.Success("item"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun flowAsUIContentStateEmitsLoadingThenError() = runTest {
        val flow = flow<String> {
            throw RuntimeException("Network down")
        }

        flow.asUIContentState().test {
            assertEquals(UIContentState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is UIContentState.Error)
            assertEquals("Network down", ((error as UIContentState.Error).message as UiText.DynamicString).value)
            awaitComplete()
        }
    }
}
