package com.topseven.fakty.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.topseven.fakty.data.models.GlossaryEntry
import com.topseven.fakty.ui.theme.PrimaryAccent

@Composable
fun GlossaryText(
    text: String,
    glossary: Map<String, GlossaryEntry>?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    onTermClick: (String, String) -> Unit
) {
    if (glossary.isNullOrEmpty()) {
        androidx.compose.material3.Text(
            text = text,
            modifier = modifier,
            color = color,
            textAlign = textAlign,
            style = style.copy(lineHeight = lineHeight)
        )
        return
    }

    val regexCache = androidx.compose.runtime.remember(glossary) {
        glossary.keys.associateWith { term ->
            val escapedTerm = Regex.escape(term)
            Regex("(?iu)(?<=\\s|^|\\p{Punct})$escapedTerm(?=\\s|$|\\p{Punct})") // case insensitive, Unicode aware
        }
    }

    // Znajdź wszystkie wystąpienia terminów
    val matches = mutableListOf<MatchResult>()
    regexCache.forEach { (term, regex) ->
        regex.findAll(text).forEach { match ->
            matches.add(MatchResult(match.range.first, match.range.last + 1, term, glossary[term]!!))
        }
    }

    // Sortuj mecze po indeksie startowym
    matches.sortBy { it.start }

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        for (match in matches) {
            // Jeśli match jest przed currentIndex (np. nakładające się słowa), pomiń
            if (match.start < currentIndex) continue

            // Dodaj tekst przed dopasowaniem
            append(text.substring(currentIndex, match.start))

            // Zapisz pozycję i dodaj adnotację dla klikalnego tekstu
            val listener = androidx.compose.ui.text.LinkInteractionListener { _ ->
                val entry = glossary[match.term]
                if (entry != null) {
                    onTermClick(entry.title, entry.definition)
                }
            }
            withLink(LinkAnnotation.Clickable(tag = match.term, linkInteractionListener = listener)) {
                withStyle(style = SpanStyle(color = PrimaryAccent, fontWeight = FontWeight.Bold)) {
                    append(text.substring(match.start, match.end))
                }
            }

            currentIndex = match.end
        }

        // Dodaj pozostały tekst
        if (currentIndex < text.length) {
            append(text.substring(currentIndex, text.length))
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

private data class MatchResult(val start: Int, val end: Int, val term: String, val entry: GlossaryEntry)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun GlossaryTextPreview() {
    MaterialTheme {
        GlossaryText(
            text = "To jest testowy tekst zawierający słowo fizyka.",
            glossary = mapOf("fizyka" to GlossaryEntry("fizyka", "Nauka przyrodnicza")),
            onTermClick = { _, _ -> }
        )
    }
}
