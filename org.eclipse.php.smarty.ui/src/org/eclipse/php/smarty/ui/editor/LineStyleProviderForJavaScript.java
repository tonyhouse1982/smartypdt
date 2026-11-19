/*******************************************************************************
 * Copyright (c) 2024 SmartyPDT contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.php.smarty.ui.editor;

import java.util.Collection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.php.internal.ui.preferences.PreferenceConstants;
import org.eclipse.swt.custom.StyleRange;

/**
 * Colours the body of &lt;script&gt; elements. The Smarty tokenizer reports the
 * whole script body as a single block of text, so the colouring is based on the
 * text itself (see {@link JavaScriptScanner}) instead of on text regions.
 */
public class LineStyleProviderForJavaScript extends LineStyleProviderForSmarty {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean prepareRegions(ITypedRegion typedRegion, int requestStart, int requestLength, Collection holdResults) {
		if (typedRegion == null || getDocument() == null) {
			return false;
		}
		final int partitionStart = typedRegion.getOffset();
		final int partitionLength = typedRegion.getLength();
		if (partitionLength <= 0) {
			return true;
		}
		final int partitionEnd = partitionStart + partitionLength;
		final int requestEnd = Math.min(partitionEnd, requestStart + requestLength);
		if (requestEnd <= requestStart || requestStart >= partitionEnd) {
			return true;
		}

		final String text;
		try {
			text = getDocument().get(partitionStart, partitionLength);
		} catch (BadLocationException e) {
			return false;
		}

		final TextAttribute normalAttribute = getTextAttributeForColor(PreferenceConstants.EDITOR_NORMAL_COLOR);
		for (JavaScriptScanner.Token token : JavaScriptScanner.scan(text)) {
			final TextAttribute attribute = getAttributeFor(token);
			if (attribute == null || attribute.equals(normalAttribute)) {
				continue;
			}
			final int start = Math.max(partitionStart + token.start, requestStart);
			final int end = Math.min(partitionStart + token.end, requestEnd);
			if (end <= start) {
				continue;
			}
			holdResults.add(createStyleRange(start, end - start, attribute));
		}
		return true;
	}

	private StyleRange createStyleRange(int start, int length, TextAttribute attribute) {
		StyleRange range = new StyleRange(start, length, attribute.getForeground(), attribute.getBackground(),
				attribute.getStyle());
		if ((attribute.getStyle() & TextAttribute.UNDERLINE) != 0) {
			range.underline = true;
			range.fontStyle &= ~TextAttribute.UNDERLINE;
		}
		if ((attribute.getStyle() & TextAttribute.STRIKETHROUGH) != 0) {
			range.strikeout = true;
			range.fontStyle &= ~TextAttribute.STRIKETHROUGH;
		}
		return range;
	}

	private TextAttribute getAttributeFor(JavaScriptScanner.Token token) {
		switch (token.type) {
		case JavaScriptScanner.COMMENT:
			return getTextAttributeForColor(PreferenceConstants.EDITOR_COMMENT_COLOR);
		case JavaScriptScanner.STRING:
			return getTextAttributeForColor(PreferenceConstants.EDITOR_STRING_COLOR);
		case JavaScriptScanner.NUMBER:
			return getTextAttributeForColor(PreferenceConstants.EDITOR_NUMBER_COLOR);
		case JavaScriptScanner.KEYWORD:
			return getTextAttributeForColor(PreferenceConstants.EDITOR_KEYWORD_COLOR);
		case JavaScriptScanner.SMARTY_VARIABLE:
			return getTextAttributeForColor(PreferenceConstants.EDITOR_VARIABLE_COLOR);
		case JavaScriptScanner.SMARTY_TAG:
			return getTextAttributeForColor(PreferenceConstants.EDITOR_BOUNDARYMARKER_COLOR);
		default:
			return null;
		}
	}
}
