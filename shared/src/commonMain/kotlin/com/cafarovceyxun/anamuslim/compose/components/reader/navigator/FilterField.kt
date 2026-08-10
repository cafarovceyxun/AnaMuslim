package com.cafarovceyxun.anamuslim.compose.components.reader.navigator

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.SearchTextField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_search
import org.jetbrains.compose.resources.painterResource

@Composable
fun FilterField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    showLeading: Boolean = true
) {
    SearchTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = hint,
        leadingIcon = if (showLeading) {
            {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_search),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onSurface.alpha(0.5f)
                )
            }
        } else null,
        keyboardType = keyboardType,
        modifier = modifier
    )
}
