/*******************************************************************************
 * Copyright (c) 2006 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend and IBM - Initial implementation
 *******************************************************************************/
package org.eclipse.php.smarty.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.php.core.PHPToolkitUtil;
import org.eclipse.php.internal.core.preferences.IPreferencesPropagatorListener;
import org.eclipse.php.internal.ui.editor.PHPStructuredEditor;
import org.eclipse.php.smarty.internal.core.documentModel.parser.SmartySourceParser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

public class SmartyStructuredEditor extends PHPStructuredEditor {
	private boolean isExternal;
	private IPreferencesPropagatorListener phpVersionListener;
	private boolean tabIndentationHandlerInstalled;

	public SmartyStructuredEditor() {
	}

	@SuppressWarnings({ "restriction", "unchecked" })
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		IResource resource = null;
		isExternal = false;

		if (input instanceof IFileEditorInput) {
			// This is the existing workspace file
			final IFileEditorInput fileInput = (IFileEditorInput) input;
			resource = fileInput.getFile();
		} else if (input instanceof IStorageEditorInput) {
			// This kind of editor input usually means non-workspace file, like
			// PHP file which comes from include path, remote file which comes
			// from
			// Web server while debugging, file from ZIP archive, etc...
		}

		if (resource instanceof IFile) {
			if (PHPToolkitUtil.isPHPFile((IFile) resource) || "tpl".equals(resource.getFullPath().getFileExtension())) {

				SmartySourceParser.editFile.set(resource);

				super.doSetInput(input);

			} else {
				super.doSetInput(input);
			}
		} else {
			isExternal = true;
			super.doSetInput(input);
		}

		ImageDescriptor imageDescriptor = input.getImageDescriptor();
		if (imageDescriptor != null) {
			setTitleImage(JFaceResources.getResources().createImageWithDefault(imageDescriptor));
		}
		if (isShowingOverrideIndicators()) {
			installOverrideIndicator(true);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		installTabIndentationHandler();
	}

	/**
	 * Installs a key listener that indents or unindents the lines covered by a
	 * multi line selection when Tab or Shift+Tab is pressed. The default
	 * SHIFT_LEFT / SHIFT_RIGHT text editor actions do not always react with the
	 * Smarty content type, so the block indentation is performed here.
	 */
	private void installTabIndentationHandler() {
		if (tabIndentationHandlerInstalled) {
			return;
		}
		ISourceViewer viewer = getSourceViewer();
		if (!(viewer instanceof ITextViewerExtension)) {
			return;
		}
		((ITextViewerExtension) viewer).prependVerifyKeyListener(event -> {
			if (event.character != '\t' || (event.stateMask & ~SWT.SHIFT) != 0) {
				return;
			}
			boolean unindent = (event.stateMask & SWT.SHIFT) != 0;
			if (shiftSelectedLines(unindent)) {
				event.doit = false;
			}
		});
		tabIndentationHandlerInstalled = true;
	}

	/**
	 * Shifts the lines covered by the current selection by one indentation
	 * level.
	 *
	 * @param unindent
	 *            true to remove one indentation level, false to add it
	 * @return true if the key event has been handled and must be consumed
	 */
	private boolean shiftSelectedLines(boolean unindent) {
		ISourceViewer viewer = getSourceViewer();
		if (viewer == null || !viewer.isEditable() || !isEditable()) {
			return false;
		}
		IDocument document = viewer.getDocument();
		if (document == null) {
			return false;
		}
		Point selection = viewer.getSelectedRange();
		int selectionOffset = selection.x;
		int selectionLength = selection.y;
		try {
			int firstLine = document.getLineOfOffset(selectionOffset);
			int lastLine = document.getLineOfOffset(selectionOffset + selectionLength);
			if (!unindent && firstLine == lastLine) {
				// Single line selection: keep the default editor behavior.
				return false;
			}
			String indentPrefix = getIndentPrefix();
			int startDelta = 0;
			int totalDelta = 0;
			for (int line = firstLine; line <= lastLine; line++) {
				int lineOffset = document.getLineOffset(line);
				int delta;
				if (unindent) {
					delta = unindentLine(document, lineOffset);
				} else {
					document.replace(lineOffset, 0, indentPrefix);
					delta = indentPrefix.length();
				}
				totalDelta += delta;
				if (lineOffset <= selectionOffset) {
					startDelta += delta;
				}
			}
			if (totalDelta == 0) {
				return false;
			}
			int newOffset = Math.max(0, selectionOffset + startDelta);
			int newLength = Math.max(0, selectionLength + totalDelta);
			viewer.setSelectedRange(newOffset, newLength);
			return true;
		} catch (BadLocationException e) {
			return false;
		}
	}

	/**
	 * Removes one indentation level from the beginning of the given line.
	 *
	 * @return the number of removed characters (negative or zero)
	 */
	private int unindentLine(IDocument document, int lineOffset) throws BadLocationException {
		if (lineOffset >= document.getLength()) {
			return 0;
		}
		if (document.getChar(lineOffset) == '\t') {
			document.replace(lineOffset, 1, "");
			return -1;
		}
		int indentWidth = getIndentPrefix().length();
		if (indentWidth <= 0) {
			indentWidth = 1;
		}
		int spaces = 0;
		while (spaces < indentWidth && lineOffset + spaces < document.getLength()
				&& document.getChar(lineOffset + spaces) == ' ') {
			spaces++;
		}
		if (spaces == 0) {
			return 0;
		}
		document.replace(lineOffset, spaces, "");
		return -spaces;
	}

	/**
	 * Returns the indentation string used by the editor preferences.
	 */
	private String getIndentPrefix() {
		try {
			SourceViewerConfiguration configuration = getSourceViewerConfiguration();
			ISourceViewer viewer = getSourceViewer();
			if (configuration != null && viewer != null) {
				String[] prefixes = configuration.getIndentPrefixes(viewer, IDocument.DEFAULT_CONTENT_TYPE);
				if (prefixes != null && prefixes.length > 0 && prefixes[0] != null && !prefixes[0].isEmpty()) {
					return prefixes[0];
				}
			}
		} catch (RuntimeException e) {
			// fall back to a tab character
		}
		return "\t";
	}

}
