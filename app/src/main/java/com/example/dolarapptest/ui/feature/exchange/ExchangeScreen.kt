package com.example.dolarapptest.ui.feature.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.example.dolarapptest.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dolarapptest.ui.components.LoadingScreen
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiIntent
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.ExchangeInputFieldUiState
import com.example.dolarapptest.ui.feature.exchange.state.ExchangeUiState.UiState
import com.example.dolarapptest.ui.feature.exchange.state.FieldPosition

import com.example.dolarapptest.ui.theme.ColorBackground
import com.example.dolarapptest.ui.theme.ColorBrandGreen
import com.example.dolarapptest.ui.theme.ColorCard
import com.example.dolarapptest.ui.theme.ColorGray
import com.example.dolarapptest.ui.theme.ColorPrimaryText
import com.example.dolarapptest.ui.theme.ColorSeparator
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ExchangeScreen(
    viewModel: ExchangeViewModel = hiltViewModel<ExchangeViewModel>(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = ColorBackground) { padding ->
        when (val screenState = uiState.state) {
            UiState.Loading ->
                LoadingScreen(modifier = Modifier.padding(padding))
            is UiState.Error -> {
                //TODO
            }
            is UiState.Success -> ExchangeScreenView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(padding),
                uiState = screenState,
                onIntent = { viewModel.onIntent(it) },
            )
        }
    }
}

@Composable
fun ExchangeScreenView(
    uiState: UiState.Success,
    modifier: Modifier = Modifier,
    onIntent: (ExchangeUiIntent) -> Unit= {},
) {
    Column(modifier) {
        Spacer(Modifier.height(24.dp))

        ExchangeTitleComponent(exchangeRate = uiState.exchangeRate)

        Spacer(Modifier.height(24.dp))

        ExchangeInputComponent(
            onSwapClicked = { onIntent(ExchangeUiIntent.Swap) },
            topExchangeInputFieldUiState = uiState.topExchangeInputFieldUiState,
            bottomExchangeInputFieldUiState = uiState.bottomExchangeInputFieldUiState,
            baseCurrencyField = uiState.baseCurrencyField,
            onBottomExchangeInputFieldChanged = { onIntent(ExchangeUiIntent.BottomAmountChanged(amount = it)) },
            onTopExchangeInputFieldChanged = { onIntent(ExchangeUiIntent.TopAmountChanged(amount = it)) },
            onCurrencyTapped = { onIntent(ExchangeUiIntent.ShowBottomSheet) }
        )

        if (uiState.isShowBottomSheet) {
            val secondaryField = if (uiState.baseCurrencyField == FieldPosition.BOTTOM)
                uiState.topExchangeInputFieldUiState
            else
                uiState.bottomExchangeInputFieldUiState

            CurrencyPickerBottomSheet(
                currencies = uiState.availableCurrencies,
                selectedCurrency = secondaryField.currency,
                onCurrencySelected = { onIntent(ExchangeUiIntent.CurrencySelected(it)) },
                onDismiss = { onIntent(ExchangeUiIntent.HideBottomSheet) }
            )
        }
    }
}

@Composable
fun ExchangeTitleComponent(exchangeRate: String) {
    Text(
        text = "Exchange",
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = ColorPrimaryText,
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = exchangeRate,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = ColorBrandGreen,
    )
}

@Composable
private fun ExchangeInputComponent(
    modifier: Modifier = Modifier,
    onSwapClicked: () -> Unit = {},
    onCurrencyTapped: () -> Unit = {},
    onTopExchangeInputFieldChanged: (String) -> Unit = {},
    onBottomExchangeInputFieldChanged: (String) -> Unit = {},
    topExchangeInputFieldUiState: ExchangeInputFieldUiState = ExchangeInputFieldUiState(),
    bottomExchangeInputFieldUiState: ExchangeInputFieldUiState = ExchangeInputFieldUiState(),
    baseCurrencyField: FieldPosition = FieldPosition.TOP,
) {
    Box(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MoneyInputField(
                currency = topExchangeInputFieldUiState.currency,
                isSelectable = baseCurrencyField != FieldPosition.TOP,
                amount = topExchangeInputFieldUiState.amount,
                onAmountChanged = onTopExchangeInputFieldChanged,
                onCurrencyTapped = onCurrencyTapped
            )
            MoneyInputField(
                currency = bottomExchangeInputFieldUiState.currency,
                isSelectable = baseCurrencyField != FieldPosition.BOTTOM,
                amount = bottomExchangeInputFieldUiState.amount,
                onAmountChanged = onBottomExchangeInputFieldChanged,
                onCurrencyTapped = onCurrencyTapped
            )
        }
        SwapButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = onSwapClicked
        )
    }
}

@Composable
private fun MoneyInputField(
    modifier: Modifier = Modifier,
    currency: String = "BTC",
    isSelectable: Boolean = true,
    onCurrencyTapped: () -> Unit = {},
    amount: String = "",
    onAmountChanged: (String) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ColorCard)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoneyCurrency(
            currency = currency,
            isSelectable = isSelectable,
            onCurrencyTapped = onCurrencyTapped
        )

        MoneyInputTextField(
            modifier = Modifier.weight(1f),
            onAmountChanged = onAmountChanged,
            amount = amount
        )
    }
}

@Composable
fun MoneyCurrency(
    currency: String = "BTC",
    isSelectable: Boolean = false,
    onCurrencyTapped: (() -> Unit) = {},
) {
    Row(
        modifier = Modifier
            .then(
                if (isSelectable)
                    Modifier.clickable {
                        onCurrencyTapped()
                    } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = currency,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorPrimaryText,
        )
        if (isSelectable) {
            Icon(
                painter = painterResource(id = R.drawable.ic_expand_more),
                contentDescription = null,
                tint = ColorPrimaryText,
            )
        }
    }
}

@Composable
fun MoneyInputTextField(
    modifier: Modifier = Modifier,
    onAmountChanged: (String) -> Unit = {},
    amount: String = "",
) {
    BasicTextField(
        modifier = modifier,
        value = amount,
        onValueChange = { onAmountChanged(it) },
        textStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.End,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    currencies: ImmutableList<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
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
            items(currencies, key = { it }) { currency ->
                CurrencyCell(
                    currency = currency,
                    isSelected = currency == selectedCurrency,
                    onSelect = { onCurrencySelected(currency) }
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
private fun SwapButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    //TODO swap focus also
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(ColorSeparator)
            .padding(6.dp)
            .clip(CircleShape)
            .background(ColorBrandGreen)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(id = R.drawable.ic_arrow_downward),
            contentDescription = null,
            tint = Color.White,
        )
    }
}

@Composable
private fun CurrencyCell(
    currency: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { onSelect() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorPrimaryText,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExchangeScreenPreview() {
    ExchangeScreenView(uiState = UiState.Success.preview())
}