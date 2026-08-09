package com.topseven.fakty.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import com.topseven.fakty.data.models.GlossaryEntry
import com.topseven.fakty.ui.theme.PrimaryAccent

/**
 * Tekst faktu z podświetlonymi, klikalnymi pojęciami ze słowniczka.
 *
 * Budowa [AnnotatedString] (skan wszystkimi wyrażeniami regularnymi po całej treści) jest
 * pamiętana dla pary `text`/`glossary`. Wcześniej liczyła się przy każdej rekompozycji, co
 * na ekranie fiszek oznaczało pełny przebieg regeksów w każdej klatce animacji obrotu 3D.
 */
@Composable
fun GlossaryText(
    text: String,
    glossary: Map<String, GlossaryEntry>?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    onTermClick: (String, String) -> Unit
) {
    if (glossary.isNullOrEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            textAlign = textAlign,
            style = style.copy(lineHeight = lineHeight)
        )
        return
    }

    // Callback może się zmieniać między rekompozycjami, a AnnotatedString jest pamiętany -
    // rememberUpdatedState gwarantuje, że klik trafia zawsze w aktualną lambdę.
    val currentOnTermClick by rememberUpdatedState(onTermClick)

    val annotatedString = remember(text, glossary) {
        buildGlossaryAnnotatedString(text, glossary) { term, definition ->
            currentOnTermClick(term, definition)
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(
            color = color,
            textAlign = textAlign,
            lineHeight = lineHeight
        )
    )
}

private fun buildGlossaryAnnotatedString(
    text: String,
    glossary: Map<String, GlossaryEntry>,
    onTermClick: (String, String) -> Unit
): AnnotatedString {
    val matches = glossary.keys
        .flatMap { term ->
            termRegex(term).findAll(text).map { match ->
                GlossaryMatch(match.range.first, match.range.last + 1, term)
            }
        }
        // Przy nakładających się pojęciach wygrywa dłuższe dopasowanie zaczynające się
        // w tym samym miejscu (np. "czarna dziura" zamiast samego "dziura").
        .sortedWith(compareBy<GlossaryMatch> { it.start }.thenByDescending { it.end })

    return buildAnnotatedString {
        var currentIndex = 0
        for (match in matches) {
            if (match.start < currentIndex) continue

            append(text.substring(currentIndex, match.start))

            val entry = glossary[match.term]
            if (entry == null) {
                append(text.substring(match.start, match.end))
                currentIndex = match.end
                continue
            }

            val listener = LinkInteractionListener { onTermClick(entry.title, entry.definition) }
            withLink(LinkAnnotation.Clickable(tag = match.term, linkInteractionListener = listener)) {
                withStyle(style = SpanStyle(color = PrimaryAccent, fontWeight = FontWeight.Bold)) {
                    append(text.substring(match.start, match.end))
                }
            }

            currentIndex = match.end
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

/**
 * Granice wyrazu oparte na literach i cyfrach Unicode zamiast ASCII-owego `\p{Punct}`.
 * Dzięki temu pojęcie otoczone polskimi cudzysłowami, półpauzą czy znakiem stopnia
 * jest nadal wykrywane, a fragment innego słowa - nie.
 */
private fun termRegex(term: String): Regex =
    Regex("(?iu)(?<![\\p{L}\\p{N}])${Regex.escape(term)}(?![\\p{L}\\p{N}])")

private data class GlossaryMatch(val start: Int, val end: Int, val term: String)

@Preview(showBackground = true, backgroundColor = 0xFF0F111A)
@Composable
private fun GlossaryTextPreview() {
    MaterialTheme {
        GlossaryText(
            text = "To jest testowy tekst zawierający słowo fizyka.",
            glossary = mapOf("fizyka" to GlossaryEntry("fizyka", "Nauka przyrodnicza")),
            onTermClick = { _, _ -> }
        )
    }
}
