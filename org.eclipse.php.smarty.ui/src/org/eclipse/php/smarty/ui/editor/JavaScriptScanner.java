/*******************************************************************************
 * Copyright (c) 2024 SmartyPDT contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.php.smarty.ui.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A small, dependency free scanner used to colour the body of &lt;script&gt;
 * elements inside Smarty templates. It understands JavaScript comments, string
 * literals, numbers and keywords, plus the Smarty constructs that are relevant
 * inside a script block:
 * <ul>
 * <li><code>{$variable}</code> is reported as a Smarty variable, unless it is
 * inside a <code>{literal}</code> block,</li>
 * <li><code>{literal}</code> and <code>{/literal}</code> are reported as Smarty
 * tags, and everything between them is treated as plain JavaScript.</li>
 * </ul>
 */
public final class JavaScriptScanner {

	/** Plain text, nothing to colour. */
	public static final int TEXT = 0;
	/** A JavaScript comment. */
	public static final int COMMENT = 1;
	/** A JavaScript string literal. */
	public static final int STRING = 2;
	/** A JavaScript number. */
	public static final int NUMBER = 3;
	/** A JavaScript keyword. */
	public static final int KEYWORD = 4;
	/** A Smarty variable reference such as <code>{$name}</code>. */
	public static final int SMARTY_VARIABLE = 5;
	/** A Smarty tag, currently <code>{literal}</code> and <code>{/literal}</code>. */
	public static final int SMARTY_TAG = 6;

	private static final Set<String> KEYWORDS = new HashSet<>();

	static {
		String[] keywords = { "async", "await", "break", "case", "catch", "class", "const", "continue", "debugger",
				"default", "delete", "do", "else", "export", "extends", "false", "finally", "for", "function", "if",
				"import", "in", "instanceof", "let", "new", "null", "of", "return", "static", "super", "switch", "this",
				"throw", "true", "try", "typeof", "undefined", "var", "void", "while", "with", "yield" };
		for (String keyword : keywords) {
			KEYWORDS.add(keyword);
		}
	}

	/** One scanned range of the source text. */
	public static final class Token {
		public final int type;
		public final int start;
		public final int end;

		Token(int type, int start, int end) {
			this.type = type;
			this.start = start;
			this.end = end;
		}
	}

	private JavaScriptScanner() {
	}

	/**
	 * Scans the given text and returns the ranges that need a special colour.
	 * Plain text is not reported.
	 */
	public static List<Token> scan(String text) {
		List<Token> tokens = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return tokens;
		}
		int i = 0;
		int length = text.length();
		boolean inLiteral = false;

		while (i < length) {
			char c = text.charAt(i);

			if (c == '{') {
				if (!inLiteral && matchSmartyTag(text, i, "literal")) {
					int end = smartyTagEnd(text, i);
					tokens.add(new Token(SMARTY_TAG, i, end));
					inLiteral = true;
					i = end;
					continue;
				}
				if (matchSmartyTag(text, i, "/literal")) {
					int end = smartyTagEnd(text, i);
					tokens.add(new Token(SMARTY_TAG, i, end));
					inLiteral = false;
					i = end;
					continue;
				}
				if (!inLiteral && i + 2 < length && text.charAt(i + 1) == '$') {
					int end = text.indexOf('}', i);
					if (end < 0) {
						end = length - 1;
					}
					tokens.add(new Token(SMARTY_VARIABLE, i, end + 1));
					i = end + 1;
					continue;
				}
				i++;
				continue;
			}

			if (c == '/' && i + 1 < length) {
				char next = text.charAt(i + 1);
				if (next == '/') {
					int end = i + 2;
					while (end < length && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
						end++;
					}
					tokens.add(new Token(COMMENT, i, end));
					i = end;
					continue;
				}
				if (next == '*') {
					int end = text.indexOf("*/", i + 2);
					end = end < 0 ? length : end + 2;
					tokens.add(new Token(COMMENT, i, end));
					i = end;
					continue;
				}
			}

			if (c == '\'' || c == '"' || c == '`') {
				int end = i + 1;
				while (end < length) {
					char current = text.charAt(end);
					if (current == '\\') {
						end += 2;
						continue;
					}
					if (current == c) {
						end++;
						break;
					}
					if (c != '`' && (current == '\n' || current == '\r')) {
						break;
					}
					end++;
				}
				if (end > length) {
					end = length;
				}
				tokens.add(new Token(STRING, i, end));
				i = end;
				continue;
			}

			if (c >= '0' && c <= '9') {
				int end = i + 1;
				while (end < length) {
					char current = text.charAt(end);
					if ((current >= '0' && current <= '9') || (current >= 'a' && current <= 'f')
							|| (current >= 'A' && current <= 'F') || current == 'x' || current == 'X'
							|| current == '.' || current == '_') {
						end++;
						continue;
					}
					if ((current == '+' || current == '-') && end > i + 1) {
						char previous = text.charAt(end - 1);
						if (previous == 'e' || previous == 'E') {
							end++;
							continue;
						}
					}
					break;
				}
				tokens.add(new Token(NUMBER, i, end));
				i = end;
				continue;
			}

			if (isIdentifierStart(c)) {
				int end = i + 1;
				while (end < length && isIdentifierPart(text.charAt(end))) {
					end++;
				}
				if (KEYWORDS.contains(text.substring(i, end))) {
					tokens.add(new Token(KEYWORD, i, end));
				}
				i = end;
				continue;
			}

			i++;
		}
		return tokens;
	}

	private static boolean matchSmartyTag(String text, int offset, String name) {
		int index = offset + 1;
		int length = text.length();
		if (index + name.length() > length) {
			return false;
		}
		if (!text.regionMatches(index, name, 0, name.length())) {
			return false;
		}
		index += name.length();
		while (index < length && (text.charAt(index) == ' ' || text.charAt(index) == '\t')) {
			index++;
		}
		return index < length && text.charAt(index) == '}';
	}

	private static int smartyTagEnd(String text, int offset) {
		int end = text.indexOf('}', offset);
		return end < 0 ? text.length() : end + 1;
	}

	private static boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '$';
	}

	private static boolean isIdentifierPart(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	}
}
