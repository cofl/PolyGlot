/*
 * Copyright (c) 2016, draque
 * All rights reserved.
 *
 * Licensed under: Creative Commons Attribution-NonCommercial 4.0 International Public License
 *  See LICENSE.TXT included with this code to read the full license agreement.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package PolyGlot.CustomControls;

import PolyGlot.DictCore;
import PolyGlot.ClipboardHandler;
import PolyGlot.Nodes.ImageNode;
import PolyGlot.PGTUtil;
import PolyGlot.PGTools;
import PolyGlot.WebInterface;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;

/**
 *
 * @author draque
 */
public class PTextPane extends JTextPane {

    private SwingWorker worker = null;
    private final String defText;
    private final DictCore core;
    private final boolean overrideFont;

    public PTextPane(DictCore _core, boolean _overideFont, String _defText) {
        core = _core;
        defText = _defText;
        overrideFont = _overideFont;
        this.setContentType("text/html");
        setupRightClickMenu();

        setupListeners();
        setText(defText);
        setForeground(Color.lightGray);
        if (!overrideFont) {
            setFont(core.getPropertiesManager().getFontCon());
        } else {
            setFont(core.getPropertiesManager().getCharisUnicodeFont());
        }
    }

    @Override
    public final void setFont(Font _font) {
        super.setFont(_font);
    }

    @Override
    public final void setForeground(Color _color) {
        super.setForeground(_color);
    }
    
    /**
     * Captures images in override, passes everything else on to super.
     */
    @Override
    public void paste() {
        // might handle more types in the future
        if (ClipboardHandler.isClipboardImage()) {
            try {
                BufferedImage image = (BufferedImage) ClipboardHandler.getClipboardImage();
                ImageNode imageNode = core.getImageCollection().getFromBufferedImage(image);
                addImage(imageNode);
            } catch (Exception e) {
                InfoBox.error("Paste Error", "Unable to paste: " + e.getLocalizedMessage(), null);
            }
        } else if (ClipboardHandler.isClipboardString()) {
            super.paste();
        } else {
            super.paste();
        }
    }

    @Override
    public final void setText(String t) {
        try {
            if (t.equals("") && !this.hasFocus()) {
                super.setText(defText);
            } else {
                super.setText(t);
            }
        } catch (Exception e) {
            InfoBox.error("Set text error", "Could not set text component: " + e.getLocalizedMessage(), null);
        }

        if (isDefaultText()) {
            setForeground(Color.lightGray);
        } else {
            setForeground(Color.black);
        }
    }

    /**
     * makes this component flash. If already flashing, does nothing.
     *
     * @param _flashColor color to flash
     * @param isBack whether display color is background (rather than
     * foreground)
     */
    public void makeFlash(Color _flashColor, boolean isBack) {
        if (worker == null || worker.isDone()) {
            worker = PGTools.getFlashWorker(this, _flashColor, isBack);
            worker.execute();
        }
    }

    /**
     * gets default value string of text
     *
     * @return default text
     */
    public String getDefaultValue() {
        return defText;
    }

    @Override
    public String getToolTipText() {
        String ret = super.getToolTipText();
        
        if (ret != null) {
            ret += " (right click to insert images)";
        }
        
        return ret;
    }
    
    /**
     * Exposes super's set text to menu items
     * @param text 
     */
    private void superSetText(String text) {
        super.setText(text);
    }

    private void setupRightClickMenu() {
        final JPopupMenu ruleMenu = new JPopupMenu();
        final JMenuItem insertImage = new JMenuItem("Insert Image");
        final JMenuItem cut = new JMenuItem("Cut");
        final JMenuItem copy = new JMenuItem("Copy");
        final JMenuItem paste = new JMenuItem("Paste");
        final PTextPane parentPane = this;

        insertImage.setToolTipText("Insert Image into Text");
        insertImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    ImageNode image = core.getImageCollection()
                            .openNewImage((Window)parentPane.getTopLevelAncestor());
                    if (image != null) {
                        // null node means user cancelled process
                        addImage(image);
                    }                    
                } catch (Exception e) {
                    InfoBox.error("Image Import Error", "Unable to import image: " 
                            + e.getLocalizedMessage(), null);
                }
            }
        });
        cut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                cut();
            }
        });
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                copy();
            }
        });
        paste.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if(isDefaultText()) { //removes default text if appropriate
                    superSetText("");
                }
                paste();
                setText(getText()); // ensures text is not left grey
            }
        });

        ruleMenu.add(insertImage);
        ruleMenu.add(cut);
        ruleMenu.add(copy);
        ruleMenu.add(paste);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() && parentPane.isEnabled()) {
                    insertImage.setEnabled(true);
                    cut.setEnabled(true);
                    copy.setEnabled(true);
                    paste.setEnabled(true);
                    ruleMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && parentPane.isEnabled()) {
                    insertImage.setEnabled(true);
                    cut.setEnabled(true);
                    copy.setEnabled(true);
                    paste.setEnabled(true);
                    ruleMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Adds image to text at point of current carat
     * @param image
     * @throws Exception 
     */
    private void addImage(ImageNode image) throws Exception {
        final String placeHold = "-POLYGLOTIMAGE-";
        
        if (isDefaultText()) {
            super.setText("");
        }
        
        ClipboardHandler test = new ClipboardHandler();

        test.cacheClipboard();
        test.setClipboardContents(placeHold);

        super.paste();
        String newText = getRawHTML();
        String imagePath = image.getImagePath();
        imagePath = "<img src=\"file:///" + imagePath + "\">";
        setText(newText.replace(placeHold, imagePath));
        test.restoreClipboard();
    }

    /**
     * Tests whether the current text value is the default value
     *
     * @return
     */
    public boolean isDefaultText() {
        String body = super.getText();
        body = body.substring(0, body.indexOf("</body>"));
        body = body.substring(body.indexOf("<body>") + 6, body.length());
        return body.trim().equals(defText);
    }
    
    /**
     * Returns true if object's body is empty (accounts for HTML elements within
     * body)
     * @return 
     */
    public boolean isEmpty() {
        String body = super.getText();
        body = body.substring(0, body.indexOf("</body>"));
        body = body.substring(body.indexOf("<body>") + 6, body.length());
        return body.trim().equals("");
    }

    /**
     * sets text to default value
     */
    public void setDefault() {
        setText(defText);
    }

    private void setupListeners() {
        // if blank, this field is being used for something more complex: no listeners
        if (!defText.equals("")) {
            FocusListener listener = new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (isDefaultText()) {
                        setText("");
                        setForeground(Color.black);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (isEmpty() && !defText.equals("")) {
                        setText(defText);
                        setForeground(Color.lightGray);
                    }
                }
            };
            
            this.addFocusListener(listener);
        }
    }

    /**
     * Gets raw html content of panel without any postprocessing
     *
     * @return
     */
    public String getRawHTML() {
        return super.getText();
    }

    /**
     * Gets plain text that appears in body (but not HTML) and nothing else
     *
     * @return
     */
    public String getNakedText() {
        String ret;  
        ret = WebInterface.getTextFromHtml(getSuperText()).trim();
  
        if (ret.equals(defText)) {
            ret = "";
        }
        
        return ret;
    }

    @Override
    public String getText() {
        String ret = super.getText().replaceAll(PGTUtil.RTLMarker, "").replaceAll(PGTUtil.LTRMarker, "");

        if (isDefaultText()) {
            ret = "";
        } else {
            ret = core.getPropertiesManager().isEnforceRTL() ? PGTUtil.RTLMarker + ret : ret;
        }

        return ret;
    }

    /**
     * Allows super method to be called in listeners
     *
     * @return
     */
    private String getSuperText() {
        return super.getText().replaceAll(PGTUtil.RTLMarker, "").replaceAll(PGTUtil.LTRMarker, "");
    }
}
