package com.cafarovceyxun.anamuslim.compose.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.screens.hadith.FormTextField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_feature
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.strLabelSend
import com.cafarovceyxun.anamuslim.resources.suggestionsBodyHint
import com.cafarovceyxun.anamuslim.resources.suggestionsBodyLabel
import com.cafarovceyxun.anamuslim.resources.suggestionsCategoryLabel
import com.cafarovceyxun.anamuslim.resources.suggestionsErrorCooldown
import com.cafarovceyxun.anamuslim.resources.suggestionsErrorFailed
import com.cafarovceyxun.anamuslim.resources.suggestionsErrorRateLimited
import com.cafarovceyxun.anamuslim.resources.suggestionsErrorTooShort
import com.cafarovceyxun.anamuslim.resources.suggestionsModerationNote
import com.cafarovceyxun.anamuslim.resources.suggestionsPrivacyNote
import com.cafarovceyxun.anamuslim.resources.suggestionsSent
import com.cafarovceyxun.anamuslim.resources.suggestionsSubmit
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionCategory
import com.cafarovceyxun.anamuslim.viewModels.SuggestionSubmitError
import com.cafarovceyxun.anamuslim.viewModels.SuggestionSubmitState
import com.cafarovceyxun.anamuslim.viewModels.SuggestionsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Təklif göndərmə vərəqi.
 *
 * View model-i **xaricdən** alır: siyahı ekranı ilə eyni nüsxə olmalıdır ki, göndərişdən sonra
 * «mənim təkliflərim» siyahısı özü yenilənsin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionSubmitSheet(
    isOpen: Boolean,
    viewModel: SuggestionsViewModel,
    onDismiss: () -> Unit,
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        SheetContent(viewModel = viewModel, onDismiss = onDismiss)
    }
}

@Composable
private fun SheetContent(
    viewModel: SuggestionsViewModel,
    onDismiss: () -> Unit,
) {
    val submitState by viewModel.submitState.collectAsState()

    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SuggestionCategory.FEATURE) }

    val sentMsg = stringResource(Res.string.suggestionsSent)
    val tooShortMsg = stringResource(Res.string.suggestionsErrorTooShort)
    val cooldownMsg = stringResource(Res.string.suggestionsErrorCooldown)
    val rateLimitedMsg = stringResource(Res.string.suggestionsErrorRateLimited)
    val failedMsg = stringResource(Res.string.suggestionsErrorFailed)

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is SuggestionSubmitState.Success -> {
                PlatformUtils.showLongToast(sentMsg)
                viewModel.resetSubmitState()
                onDismiss()
            }

            is SuggestionSubmitState.Error -> {
                PlatformUtils.showLongToast(
                    when (state.reason) {
                        SuggestionSubmitError.TooShort -> tooShortMsg
                        SuggestionSubmitError.Cooldown -> cooldownMsg
                        SuggestionSubmitError.RateLimited -> rateLimitedMsg
                        SuggestionSubmitError.Failed -> failedMsg
                    }
                )
                viewModel.resetSubmitState()
            }

            else -> Unit
        }
    }

    val isSubmitting = submitState is SuggestionSubmitState.Submitting
    val canSubmit = body.trim().length >= SuggestionsViewModel.MIN_BODY_LENGTH && !isSubmitting

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom)),
    ) {
        BottomSheetHeader(
            icon = Res.drawable.dr_icon_feature,
            title = stringResource(Res.string.suggestionsSubmit),
            hasDragHandle = true,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.suggestionsCategoryLabel),
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionCategory.ALL.forEach { value ->
                Chip(
                    selected = category == value,
                    onClick = { category = value },
                    label = {
                        Text(
                            text = suggestionCategoryLabel(value),
                            style = typography.labelMedium,
                            maxLines = 1,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        FormTextField(
            value = body,
            onValueChange = { if (it.length <= SuggestionsViewModel.MAX_BODY_LENGTH) body = it },
            label = stringResource(Res.string.suggestionsBodyLabel),
            placeholder = stringResource(Res.string.suggestionsBodyHint),
            icon = Res.drawable.dr_icon_edit,
            minLines = 4,
            maxLines = 8,
            supportingText = "${body.trim().length}/${SuggestionsViewModel.MAX_BODY_LENGTH}",
        )

        Spacer(Modifier.height(12.dp))

        InfoLine(stringResource(Res.string.suggestionsPrivacyNote))
        Spacer(Modifier.height(6.dp))
        InfoLine(stringResource(Res.string.suggestionsModerationNote))

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.submit(body, category) },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }

            Text(text = stringResource(Res.string.strLabelSend), style = typography.labelLarge)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun InfoLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(Res.drawable.dr_icon_info),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
        )
    }
}
