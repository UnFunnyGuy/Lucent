package app.lucent.firefly.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lucent.firefly.core.designsystem.theme.LucentTheme
import app.lucent.firefly.core.ui.R

@Composable
fun FinancialKeypad(
    onDigitClick: (Char) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val rows =
            listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('.', '0', '⌫'),
            )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { char ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(buttonShape)
                                .background(LucentTheme.colors.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    if (char == '⌫') {
                                        onBackspaceClick()
                                    } else {
                                        onDigitClick(char)
                                    }
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (char == '⌫') {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.core_ui_backspace),
                                tint = LucentTheme.colors.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = char.toString(),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = LucentTheme.colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FinancialKeypadPreview() {
    LucentTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FinancialKeypad(
                onDigitClick = {},
                onBackspaceClick = {},
            )
        }
    }
}
