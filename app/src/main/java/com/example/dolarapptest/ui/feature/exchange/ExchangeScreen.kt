package com.example.dolarapptest.ui.feature.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dolarapptest.R

private val ColorBackground = Color(0xFFF8F8F8)
private val ColorCard = Color(0xFFFFFFFF)
private val ColorBrandGreen = Color(0xFF22D081)
private val ColorPrimaryText = Color(0xFF2C2C2E)
private val ColorGray = Color(0xFF949494)
private val ColorSeparator = Color(0xFFF4F4F4)

@Composable
fun ExchangeScreen(
    modifier: Modifier = Modifier,
    viewModel: ExchangeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        containerColor = ColorBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val state = uiState
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorBrandGreen)
                }
            }
            state.error != null -> {
                LaunchedEffect(state.error) {
                    val result = snackbarHostState.showSnackbar(
                        message = state.error,
                        actionLabel = "Retry"
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(ExchangeIntent.RetryClicked)
                    }
                }
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorBrandGreen)
                }
            }
            state.ticker != null -> {
                ExchangeContent(
                    state = state,
                    onIntent = viewModel::onIntent,
                    modifier = Modifier.padding(padding)
                )
                if (state.showBottomSheet) {
                    CurrencyPickerBottomSheet(
                        currencies = state.availableCurrencies,
                        selectedCurrency = state.bottomCurrency,
                        onCurrencySelected = { viewModel.onIntent(ExchangeIntent.CurrencySelected(it)) },
                        onDismiss = { viewModel.onIntent(ExchangeIntent.BottomSheetDismissed) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExchangeContent(
    state: ExchangeUiState,
    onIntent: (ExchangeIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Exchange",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = ColorPrimaryText,
            letterSpacing = (-0.02).sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = state.rateLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorBrandGreen,
            letterSpacing = 0.02.sp
        )

        Spacer(Modifier.height(24.dp))

        Box {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MoneyInputField(
                    currency = state.topCurrency,
                    amount = state.topAmount,
                    isActive = state.activeField == ActiveField.TOP,
                    isSelectable = false,
                    onAmountChanged = { onIntent(ExchangeIntent.AmountChanged(ActiveField.TOP, it)) },
                    onFocused = { onIntent(ExchangeIntent.FieldFocused(ActiveField.TOP)) }
                )
                MoneyInputField(
                    currency = state.bottomCurrency,
                    amount = state.bottomAmount,
                    isActive = state.activeField == ActiveField.BOTTOM,
                    isSelectable = true,
                    onAmountChanged = { onIntent(ExchangeIntent.AmountChanged(ActiveField.BOTTOM, it)) },
                    onFocused = { onIntent(ExchangeIntent.FieldFocused(ActiveField.BOTTOM)) },
                    onCurrencyTapped = { onIntent(ExchangeIntent.BottomCurrencyTapped) }
                )
            }

            SwapButton(
                onClick = { onIntent(ExchangeIntent.SwapClicked) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MoneyInputField(
    currency: String,
    amount: String,
    isActive: Boolean,
    isSelectable: Boolean,
    onAmountChanged: (String) -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onCurrencyTapped: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(91.dp)
                .then(if (isSelectable && onCurrencyTapped != null)
                    Modifier.clickable { onCurrencyTapped() } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currency,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorPrimaryText,
                letterSpacing = 0.02.sp
            )
            if (isSelectable) {
                Text(
                    text = "▾",
                    fontSize = 12.sp,
                    color = ColorPrimaryText
                )
            }
        }

        BasicTextField(
            value = amount,
            onValueChange = { onAmountChanged(it.filter { c -> c.isDigit() || c == '.' }) },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { if (it.isFocused) onFocused() },
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPrimaryText,
                textAlign = TextAlign.End,
                letterSpacing = 0.02.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(if (isActive) Color(0xFF2E7DF6) else Color.Transparent),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (amount.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorGray,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SwapButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(ColorSeparator)
            .padding(3.dp)
            .clip(CircleShape)
            .background(ColorBrandGreen)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↕",
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    currencies: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorBackground,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Text(
            text = "Select currency",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorPrimaryText,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ColorCard)
        ) {
            items(currencies) { currency ->
                CurrencyCell(
                    currency = currency,
                    isSelected = currency == selectedCurrency,
                    onClick = { onCurrencySelected(currency) }
                )
                if (currency != currencies.last()) {
                    HorizontalDivider(color = ColorSeparator, thickness = 0.5.dp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CurrencyCell(
    currency: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorPrimaryText,
            letterSpacing = 0.02.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) ColorBrandGreen else Color.Transparent)
                .then(
                    if (!isSelected) Modifier.background(
                        Color.Transparent,
                        CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .then(Modifier.background(Color(0xFFD4D4D4).copy(alpha = 0f)))
                )
                RadioButton(
                    selected = false,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(unselectedColor = Color(0xFFD4D4D4))
                )
            }
        }
    }
}
