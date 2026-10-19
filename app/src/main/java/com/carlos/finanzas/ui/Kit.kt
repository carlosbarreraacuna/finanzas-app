package com.carlos.finanzas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.ui.theme.Fin
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ---------- Formato ----------

val Es: Locale = Locale("es", "CO")
private val grouping = DecimalFormat("#,##0", DecimalFormatSymbols(Es).apply { groupingSeparator = '.' })

/** 4790000 -> "$47.900"; negativos con signo menos tipográfico. */
fun formatMoney(cents: Long): String = (if (cents < 0) "−" else "") + "$" + grouping.format(abs(cents) / 100)

/** Igual que [formatMoney] pero con "+" en los positivos (ingresos). */
fun formatSigned(cents: Long): String = if (cents > 0) "+" + formatMoney(cents) else formatMoney(cents)

/** "150000" (pesos) -> 15.000.000 centavos. Ignora lo que no sea dígito. */
fun pesosToCents(text: String): Long = (text.filter(Char::isDigit).toLongOrNull() ?: 0L) * 100

fun centsToPesosText(cents: Long): String = if (cents > 0) (cents / 100).toString() else ""

val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Es)
val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Es)
val dayMonthLong: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Es)
val weekdayDayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Es)
val timeShort: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Es)

fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Es) else it.toString() }

fun monthYearText(date: LocalDate): String = date.format(monthYear)

// ---------- Iconos (trazos del diseño) ----------

private fun strokeIcon(name: String, vararg paths: String, filled: Boolean = false): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    paths.forEach { d ->
        if (filled) {
            b.addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        } else {
            b.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }
    return b.build()
}

object FinIcons {
    val Home = strokeIcon("home", "M4 10.5L12 4l8 6.5V20H14v-6h-4v6H4z")
    val Movements = strokeIcon("movements", "M7 20V4M3 8l4-4 4 4M17 4v16M13 16l4 4 4-4")
    val Budget = strokeIcon("budget", "M12 3a9 9 0 1 0 9 9h-9z", "M15 3.5A9 9 0 0 1 20.5 9H15z")
    val Debts = strokeIcon(
        "debts",
        "M5.5 5h13A2.5 2.5 0 0 1 21 7.5v9a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 16.5v-9A2.5 2.5 0 0 1 5.5 5z",
        "M3 10h18M7 15h4"
    )
    val More = strokeIcon(
        "more",
        "M5 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z",
        "M12 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z",
        "M19 10.2a1.8 1.8 0 1 1 0 3.6a1.8 1.8 0 1 1 0-3.6z",
        filled = true
    )
    val Plus = strokeIcon("plus", "M12 5v14M5 12h14")
    val Back = strokeIcon("back", "M15 5l-7 7 7 7")
    val Chevron = strokeIcon("chevron", "M9 5l7 7-7 7")
}

// ---------- Componentes ----------

/** Superficie blanca (oscura en modo oscuro) sin sombra: el tono la separa del fondo. */
@Composable
fun Sheet(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    color: Color = Fin.colors.card,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radius))
            .background(color)
            .padding(padding),
        content = content
    )
}

@Composable
fun Bar(fraction: Float, color: Color, track: Color = Fin.colors.track, height: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .height(height)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Fin.colors.accent,
    content: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null,
    height: Dp = 48.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, color: Color = Fin.colors.accent, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 44.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Control segmentado: fondo de pista y la opción activa en superficie. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Fin.colors.track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (on) Fin.colors.card else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) Fin.colors.ink else Fin.colors.inkSoft,
                    maxLines = 1
                )
            }
        }
    }
}

/** Opciones en rejilla de 3 columnas (categorías, tipos). */
@Composable
fun ChoiceGrid(options: List<String>, selected: String?, onSelect: (String) -> Unit, columns: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val on = option == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) Fin.colors.accent else Fin.colors.card)
                            .border(1.5.dp, if (on) Fin.colors.accent else Fin.colors.chipBorder, RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { onSelect(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else Fin.colors.ink,
                            maxLines = 1
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Campo relleno con etiqueta encima, sin borde. */
@Composable
fun FinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    big: Boolean = false,
    helper: String? = null,
    fieldColor: Color = Fin.colors.ground
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Fin.colors.inkSoft)
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = if (big) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = fieldColor,
                unfocusedContainerColor = fieldColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Fin.colors.ink,
                unfocusedTextColor = Fin.colors.ink,
                cursorColor = Fin.colors.accent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (helper != null) Text(helper, style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
    }
}

/** Cuadro con la inicial del comercio. */
@Composable
fun Monogram(text: String, bg: Color = Fin.colors.track, fg: Color = Fin.colors.accent) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

/** Título grande de una pestaña con una acción a la derecha. */
@Composable
fun ScreenHeader(title: String, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Fin.colors.ink)
        if (action != null) Row(content = action)
    }
}

/** Barra de una pantalla apilada: "‹ Volver" y una acción opcional. */
@Composable
fun BackBar(backLabel: String, onBack: () -> Unit, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .heightIn(min = 44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(FinIcons.Back, contentDescription = null, tint = Fin.colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(backLabel, style = MaterialTheme.typography.labelLarge, color = Fin.colors.accent)
        }
        if (action != null) Row(content = action)
    }
}

/** Fila con título, subtítulo y flecha; toda la fila es tocable. */
@Composable
fun NavRow(title: String, subtitle: String, onClick: () -> Unit, subtitleColor: Color = Fin.colors.inkSoft) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink)
            Text(subtitle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = subtitleColor)
        }
        Icon(FinIcons.Chevron, contentDescription = null, tint = Fin.colors.inkSoft, modifier = Modifier.size(20.dp))
    }
}

/** Línea de etiqueta pequeña sobre un grupo (ej. "Hoy, 18 de octubre"). */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Fin.colors.inkSoft,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}
