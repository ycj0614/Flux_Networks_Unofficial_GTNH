package com.ycj0614.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.FontRenderer;

import com.ycj0614.fluxnetworks.client.gui.basic.GuiTextField;
import com.ycj0614.fluxnetworks.client.gui.basic.ITextBoxButton;

/**
 * 왼쪽에 설명 글자가 붙은 텍스트 상자. 숫자 전용·16진수 전용·비밀번호(가림) 모드가 있다.
 */
public class TextboxButton extends GuiTextField {

    private String origin;
    private final String extraText;
    private final int textWidth;
    private boolean digitsOnly;
    private boolean hexOnly;
    private final ITextBoxButton gui;

    private int outlineColor = 0xffb4b4b4;
    private static final int boxColor = 0x20000000;

    public TextboxButton(ITextBoxButton gui, String text, int componentId, FontRenderer fontRenderer, int x, int y,
        int par5Width, int par6Height, int width) {
        super(componentId, fontRenderer, x + width, y, par5Width - width, par6Height);
        this.extraText = text;
        this.fontRenderer = fontRenderer;
        this.textWidth = width;
        this.gui = gui;
    }

    public static TextboxButton create(ITextBoxButton gui, String text, int id, FontRenderer fontRenderer, int x, int y,
        int width, int height) {
        return new TextboxButton(gui, text, id, fontRenderer, x, y, width, height, fontRenderer.getStringWidth(text));
    }

    public int getIntegerFromText(boolean nonNegative) {
        if (nonNegative) {
            return Math.max(Integer.parseInt(getText()), 0);
        }
        return Integer.parseInt(getText());
    }

    public long getLongFromText(boolean nonNegative) {
        if (nonNegative) {
            return Math.max(Long.parseLong(getText()), 0);
        }
        return Long.parseLong(getText());
    }

    public int getIntegerFromHex() {
        return Integer.parseInt(getText(), 16);
    }

    @Override
    public void drawTextBox() {
        this.setEnableBackgroundDrawing(true);
        if (this.getVisible()) {
            if (this.getEnableBackgroundDrawing()) {
                drawRect(this.x - textWidth - 1, this.y - 1, this.x + this.width + 1, this.y, outlineColor);
                drawRect(
                    this.x - textWidth - 1,
                    this.y + this.height,
                    this.x + this.width + 1,
                    this.y + this.height + 1,
                    outlineColor);
                drawRect(this.x - textWidth - 1, this.y, this.x - textWidth, this.y + this.height, outlineColor);
                drawRect(this.x + width, this.y, this.x + this.width + 1, this.y + this.height, outlineColor);
                drawRect(this.x - textWidth, this.y, this.x + this.width, this.y + this.height, boxColor);
            }
        }
        this.setEnableBackgroundDrawing(false);
        x += 4;
        y += (this.height - 8) / 2;
        super.drawTextBox();
        fontRenderer.drawString(extraText, x - textWidth, y, outlineColor);
        x -= 4;
        y -= (this.height - 8) / 2;
    }

    @Override
    public void writeText(String textToWrite) {
        if (digitsOnly) {
            for (int i = 0; i < textToWrite.length(); i++) {
                char c = textToWrite.charAt(i);
                if (!Character.isDigit(c)) {
                    if (getText().isEmpty()) {
                        if (c != '-') {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }
        if (hexOnly) {
            for (int i = 0; i < textToWrite.length(); i++) {
                char c = textToWrite.charAt(i);
                if (c == '-') {
                    return;
                }
            }
            String origin = getText();
            super.writeText(textToWrite);
            try {
                Integer.parseInt(getText(), 16);
            } catch (final NumberFormatException ignored) {
                setText(origin);
            }
            return;
        }
        super.writeText(textToWrite);
    }

    /** 숫자 전용이면 포커스를 잃을 때 숫자가 아닌 값을 되돌리고, 포커스를 잃으면 GUI에 알린다 */
    @Override
    protected void switchFocused(boolean isFocusedIn) {
        if (digitsOnly) {
            if (isFocusedIn) {
                origin = getText();
            } else {
                if (getText().isEmpty() || getText().equals("-")) {
                    setText("0");
                    return;
                }
                try {
                    Long.parseLong(getText());
                } catch (final NumberFormatException ignored) {
                    setText(origin);
                }
            }
        }
        if (!isFocusedIn) {
            gui.onTextBoxChanged(this);
        }
    }

    public TextboxButton setOutlineColor(int color) {
        this.outlineColor = color;
        return this;
    }

    public TextboxButton setTextInvisible() {
        isTextInvisible = true;
        return this;
    }

    public TextboxButton setDigitsOnly() {
        digitsOnly = true;
        return this;
    }

    public TextboxButton setHexOnly() {
        hexOnly = true;
        return this;
    }
}
