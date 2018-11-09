/*
* Copyright 2018 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package androidx.ui.painting

import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.TextAffinity
import androidx.ui.engine.text.TextPosition
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.gestures.recognizer.GestureRecognizer
import androidx.ui.painting.basictypes.RenderComparison
import java.lang.StringBuilder

/**
 * A [TextSpan] object can be styled using its [style] property.
 * The style will be applied to the [text] and the [children].
 *
 * For the object to be useful, at least one of [text] or [children] should be set.
 *
 * * `style`: The style to apply to the [text] and the [children].
 *
 * * `text`: The text contained in the span. If both [text] and [children] are non-null, the text
 *   will precede the children.
 *
 * * `children`: Additional spans to include as children. If both [text] and [children] are
 *   non-null, the text will precede the children. The list must not contain any nulls.
 *
 * * `recognizer`: A gesture recognizer that will receive events that hit this text span.
 */

// TODO(Migration/qqd): Figure out a way to implement this @immutable.
// @immutable
data class TextSpan(
    val style: TextStyle? = null,
    val text: String? = null,
    val children: List<TextSpan>? = null,
    val recognizer: GestureRecognizer? = null
) : DiagnosticableTree {

    /**
     * Apply the [style], [text], and [children] of this object to the given [ParagraphBuilder],
     * from which a [Paragraph] can be obtained.
     * [Paragraph] objects can be drawn on [Canvas] objects.
     *
     * Rather than using this directly, it's simpler to use the [TextPainter] class to paint
     * [TextSpan] objects onto [Canvas] objects.
     */
    fun build(builder: ParagraphBuilder, textScaleFactor: Double = 1.0) {
        assert(debugAssertIsValid())
        val hasStyle: Boolean = style != null
        if (hasStyle) {
            builder.pushStyle(style!!.getTextStyle(textScaleFactor))
        }
        if (text != null) {
            builder.addText(text)
        }
        if (children != null) {
            for (child in children) {
                assert(child != null)
                child.build(builder, textScaleFactor)
            }
        }
        if (hasStyle) {
            builder.pop()
        }
    }

    /**
     * Walks this text span and its descendants in pre-order and calls [visitor]
     * for each span that has text.
     */
    fun visitTextSpan(visitor: (span: TextSpan) -> Boolean): Boolean {
        if (text != null) {
            if (!visitor(this)) {
                return false
            }
        }
        if (children != null) {
            for (child in children) {
                if (!child.visitTextSpan(visitor)) {
                    return false
                }
            }
        }
        return true
    }

    /** Returns the text span that contains the given position in the text. */
    fun getSpanForPosition(position: TextPosition): TextSpan? {
        assert(debugAssertIsValid())
        val affinity: TextAffinity = position.affinity
        val targetOffset: Int = position.offset
        var offset = 0
        var result: TextSpan? = null
        visitTextSpan {
                span: TextSpan ->
            assert(result == null)
            val endOffset: Int = offset + span.text!!.length
            if (targetOffset == offset && affinity == TextAffinity.downstream ||
                targetOffset > offset && targetOffset < endOffset ||
                targetOffset == endOffset && affinity == TextAffinity.upstream) {
                result = span
                false
            }
            offset = endOffset
            true
        }
        return result
    }

    /**
     * Flattens the [TextSpan] tree into a single string.
     *
     * Styles are not honored in this process.
     */
    fun toPlainText(): String {
        assert(debugAssertIsValid())
        val buffer = StringBuilder()
        visitTextSpan {
                span: TextSpan ->
            buffer.append(span.text)
            true
        }
        return buffer.toString()
    }

    /**
     * Returns the UTF-16 code unit at the given index in the flattened string.
     *
     * Returns null if the index is out of bounds.
     */
    // TODO(Migration/qqd): VERY IMPORTANT the name is weird, and what it does is also weird.
//    fun codeUnitAt(index: Int): Int? {
//        if (index < 0)
//            return null
//        var offset: Int? = 0
//        var result: Int? = null
//        visitTextSpan {
//                span: TextSpan ->
//            if (index - offset!! < span.text!!.length) {
//                // Flutter only considered BMP (Basic Multilingual Plane or Plane 0), so we only
//                // return the high-surrogate (index 0) in the UTF-16 representation array resulting
//                // from Character.toChars.
//                val codePoint = span.text[index - offset]
//                val utf16Array = Character.toChars(codePoint.toInt())
//                result = utf16Array[0].toInt()
//                false
//            }
//            offset += span.text?.length
//            true
//        }
//        return result
//    }

    /**
     * In checked mode, throws an exception if the object is not in a valid configuration.
     * Otherwise, returns true.
     */
    fun debugAssertIsValid(): Boolean {
        assert(visitTextSpan { span: TextSpan ->
            if (span.children != null) {
                for (child in span.children) {
                    if (child == null) false
                }
            }
            true
        }) {
            "TextSpan contains a null child.\n" +
                    "A TextSpan object with a non-null child list should not have any nulls in " +
                    "its child list.\n" +
                    "The full text in question was:\n" +
                    "${toStringDeep(prefixLineOne = "  ")}"
        }
        return true
    }

    /**
     * Describe the difference between this text span and another, in terms ofhow much damage it
     * will make to the rendering. The comparison is deep.
     */
    fun compareTo(other: TextSpan): RenderComparison {
        if (this === other) {
            return RenderComparison.IDENTICAL
        }
        if (other.text != text ||
            children?.size != other.children?.size ||
            (style == null) != (other.style == null)) {
            return RenderComparison.LAYOUT
        }
        var result: RenderComparison =
            if (recognizer == other.recognizer) RenderComparison.IDENTICAL
            else RenderComparison.METADATA
        if (style != null) {
            val candidate: RenderComparison = style.compareTo(other.style!!)
            if (candidate.ordinal > result.ordinal) {
                result = candidate
            }
            if (result == RenderComparison.LAYOUT) {
                return result
            }
        }
        if (children != null) {
            for (index in children.indices) {
                val candidate: RenderComparison = children[index].compareTo(other.children!![index])
                if (candidate.ordinal > result.ordinal) {
                    result = candidate
                }
                if (result == RenderComparison.LAYOUT) {
                    return result
                }
            }
        }
        return result
    }

    // TODO(Migration/qqd): Implement toStringShort.
    override fun toStringShort(): String {
        TODO()
    }
//    @override
//    String toStringShort() => '$runtimeType';

    // TODO(Migration/qqd): Implement debugFillProperties.
    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        TODO()
    }
//    @override
//    void debugFillProperties(DiagnosticPropertiesBuilder properties) {
//        super.debugFillProperties(properties);
//        properties.defaultDiagnosticsTreeStyle = DiagnosticsTreeStyle.whitespace;
//        // Properties on style are added as if they were properties directly on
//        // this TextSpan.
//        if (style != null)
//            style.debugFillProperties(properties);
//
//        properties.add(new DiagnosticsProperty<GestureRecognizer>(
//            'recognizer', recognizer,
//            description: recognizer?.runtimeType?.toString(),
//            defaultValue: null,
//        ));
//
//        properties.add(new StringProperty('text', text, showName: false, defaultValue: null));
//        if (style == null && text == null && children == null)
//            properties.add(new DiagnosticsNode.message('(empty)'));
//    }

    override fun debugDescribeChildren(): List<DiagnosticsNode> {
        if (children == null) {
            val defaultList: List<DiagnosticsNode> = emptyList()
            return defaultList
        }
        return children.map { child: TextSpan ->
            if (child != null) {
                child.toDiagnosticsNode()
            } else {
                DiagnosticsNode.message("<null child>")
            }
        }.toList()
    }
}
