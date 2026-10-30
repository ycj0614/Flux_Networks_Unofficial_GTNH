package com.ycj0614.fluxnetworks.client.gui.basic;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 바닐라 1.7.10 GuiTextField를 복사하고 원작(1.12 복사본)의 변경점을 넣은 클래스.
 * 원작 변경점: 좌표·크기·폰트 필드 공개, 비밀번호용 isTextInvisible, 포커스가 바뀔 때 switchFocused 호출.
 * 원작과 다름: 1.7.10에 없는 GuiPageButtonList.GuiResponder와 입력 검증기(validator)는 뺐다 (원작에서도 쓰지 않음)
 */
@SideOnly(Side.CLIENT)
public abstract class GuiTextField extends Gui {

    private final int id;
    public FontRenderer fontRenderer;
    public int x;
    public int y;
    public int width;
    public int height;
    private String text = "";
    private int maxStringLength = 32;
    private int cursorCounter;
    private boolean enableBackgroundDrawing = true;
    private boolean canLoseFocus = true;
    protected boolean isFocused;
    private boolean isEnabled = true;
    private int lineScrollOffset;
    private int cursorPosition;
    private int selectionEnd;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;
    private boolean visible = true;

    public boolean isTextInvisible = false;

    public GuiTextField(int componentId, FontRenderer fontRenderer, int x, int y, int width, int height) {
        this.id = componentId;
        this.fontRenderer = fontRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateCursorCounter() {
        ++this.cursorCounter;
    }

    public void setText(String textIn) {
        if (textIn.length() > this.maxStringLength) {
            this.text = textIn.substring(0, this.maxStringLength);
        } else {
            this.text = textIn;
        }
        this.setCursorPositionEnd();
    }

    public String getText() {
        return this.text;
    }

    public String getSelectedText() {
        int i = Math.min(this.cursorPosition, this.selectionEnd);
        int j = Math.max(this.cursorPosition, this.selectionEnd);
        return this.text.substring(i, j);
    }

    /** 선택한 글자를 바꾸거나 커서 위치에 글자를 넣는다 */
    public void writeText(String textToWrite) {
        String s = "";
        String s1 = ChatAllowedCharacters.filerAllowedCharacters(textToWrite);
        int i = Math.min(this.cursorPosition, this.selectionEnd);
        int j = Math.max(this.cursorPosition, this.selectionEnd);
        int k = this.maxStringLength - this.text.length() - (i - j);

        if (!this.text.isEmpty()) {
            s = s + this.text.substring(0, i);
        }

        int l;
        if (k < s1.length()) {
            s = s + s1.substring(0, k);
            l = k;
        } else {
            s = s + s1;
            l = s1.length();
        }

        if (!this.text.isEmpty() && j < this.text.length()) {
            s = s + this.text.substring(j);
        }

        this.text = s;
        this.moveCursorBy(i - this.selectionEnd + l);
    }

    public void deleteWords(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
            }
        }
    }

    public void deleteFromCursor(int num) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.cursorPosition) {
                this.writeText("");
            } else {
                boolean flag = num < 0;
                int i = flag ? this.cursorPosition + num : this.cursorPosition;
                int j = flag ? this.cursorPosition : this.cursorPosition + num;
                String s = "";

                if (i >= 0) {
                    s = this.text.substring(0, i);
                }
                if (j < this.text.length()) {
                    s = s + this.text.substring(j);
                }

                this.text = s;
                if (flag) {
                    this.moveCursorBy(num);
                }
            }
        }
    }

    public int getId() {
        return this.id;
    }

    public int getNthWordFromCursor(int numWords) {
        return this.getNthWordFromPos(numWords, this.getCursorPosition());
    }

    /** 원작(1.12)처럼 pos를 쓴다. 1.7.10 바닐라는 pos 대신 커서 위치를 써서 Shift+Ctrl 선택이 어긋난다 */
    public int getNthWordFromPos(int n, int pos) {
        return this.getNthWordFromPosWS(n, pos, true);
    }

    public int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
        int i = pos;
        boolean flag = n < 0;
        int j = Math.abs(n);

        for (int k = 0; k < j; ++k) {
            if (!flag) {
                int l = this.text.length();
                i = this.text.indexOf(32, i);

                if (i == -1) {
                    i = l;
                } else {
                    while (skipWs && i < l && this.text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while (skipWs && i > 0 && this.text.charAt(i - 1) == ' ') {
                    --i;
                }
                while (i > 0 && this.text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }
        return i;
    }

    public void moveCursorBy(int num) {
        this.setCursorPosition(this.selectionEnd + num);
    }

    public void setCursorPosition(int pos) {
        this.cursorPosition = pos;
        int i = this.text.length();
        this.cursorPosition = Math.max(0, Math.min(this.cursorPosition, i));
        this.setSelectionPos(this.cursorPosition);
    }

    public void setCursorPositionZero() {
        this.setCursorPosition(0);
    }

    public void setCursorPositionEnd() {
        this.setCursorPosition(this.text.length());
    }

    /** 1.7.10 방식: Ctrl+A/C/V/X는 typedChar 1/3/22/24로 들어온다 */
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (!this.isFocused) {
            return false;
        }
        switch (typedChar) {
            case 1:
                this.setCursorPositionEnd();
                this.setSelectionPos(0);
                return true;
            case 3:
                GuiScreen.setClipboardString(this.getSelectedText());
                return true;
            case 22:
                if (this.isEnabled) {
                    this.writeText(GuiScreen.getClipboardString());
                }
                return true;
            case 24:
                GuiScreen.setClipboardString(this.getSelectedText());
                if (this.isEnabled) {
                    this.writeText("");
                }
                return true;
            default:
                switch (keyCode) {
                    case 14: // Backspace
                        if (GuiScreen.isCtrlKeyDown()) {
                            if (this.isEnabled) {
                                this.deleteWords(-1);
                            }
                        } else if (this.isEnabled) {
                            this.deleteFromCursor(-1);
                        }
                        return true;
                    case 199: // Home
                        if (GuiScreen.isShiftKeyDown()) {
                            this.setSelectionPos(0);
                        } else {
                            this.setCursorPositionZero();
                        }
                        return true;
                    case 203: // 왼쪽
                        if (GuiScreen.isShiftKeyDown()) {
                            if (GuiScreen.isCtrlKeyDown()) {
                                this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
                            } else {
                                this.setSelectionPos(this.getSelectionEnd() - 1);
                            }
                        } else if (GuiScreen.isCtrlKeyDown()) {
                            this.setCursorPosition(this.getNthWordFromCursor(-1));
                        } else {
                            this.moveCursorBy(-1);
                        }
                        return true;
                    case 205: // 오른쪽
                        if (GuiScreen.isShiftKeyDown()) {
                            if (GuiScreen.isCtrlKeyDown()) {
                                this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
                            } else {
                                this.setSelectionPos(this.getSelectionEnd() + 1);
                            }
                        } else if (GuiScreen.isCtrlKeyDown()) {
                            this.setCursorPosition(this.getNthWordFromCursor(1));
                        } else {
                            this.moveCursorBy(1);
                        }
                        return true;
                    case 207: // End
                        if (GuiScreen.isShiftKeyDown()) {
                            this.setSelectionPos(this.text.length());
                        } else {
                            this.setCursorPositionEnd();
                        }
                        return true;
                    case 211: // Delete
                        if (GuiScreen.isCtrlKeyDown()) {
                            if (this.isEnabled) {
                                this.deleteWords(1);
                            }
                        } else if (this.isEnabled) {
                            this.deleteFromCursor(1);
                        }
                        return true;
                    default:
                        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                            if (this.isEnabled) {
                                this.writeText(Character.toString(typedChar));
                            }
                            return true;
                        }
                        return false;
                }
        }
    }

    /** 버튼 위가 아니어도 클릭할 때마다 호출된다 */
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean flag = mouseX >= this.x && mouseX < this.x + this.width
            && mouseY >= this.y
            && mouseY < this.y + this.height;

        if (this.canLoseFocus) {
            this.setFocused(flag);
        }

        if (this.isFocused && flag && mouseButton == 0) {
            int i = mouseX - this.x;
            if (this.enableBackgroundDrawing) {
                i -= 4;
            }
            String s = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            this.setCursorPosition(
                this.fontRenderer.trimStringToWidth(s, i)
                    .length() + this.lineScrollOffset);
            return true;
        }
        return false;
    }

    public String getInvisibleText(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append("*");
        }
        return builder.toString();
    }

    public void drawTextBox() {
        if (!this.getVisible()) {
            return;
        }
        if (this.getEnableBackgroundDrawing()) {
            drawRect(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, -6250336);
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, -16777216);
        }
        String text2 = isTextInvisible ? getInvisibleText(text.length()) : text;
        int i = this.isEnabled ? this.enabledColor : this.disabledColor;
        int j = this.cursorPosition - this.lineScrollOffset;
        int k = this.selectionEnd - this.lineScrollOffset;
        String s = this.fontRenderer.trimStringToWidth(text2.substring(this.lineScrollOffset), this.getWidth());
        boolean flag = j >= 0 && j <= s.length();
        boolean flag1 = this.isFocused && this.cursorCounter / 6 % 2 == 0 && flag;
        int l = this.enableBackgroundDrawing ? this.x + 4 : this.x;
        int i1 = this.enableBackgroundDrawing ? this.y + (this.height - 8) / 2 : this.y;
        int j1 = l;

        if (k > s.length()) {
            k = s.length();
        }

        if (!s.isEmpty()) {
            String s1 = flag ? s.substring(0, j) : s;
            j1 = this.fontRenderer.drawStringWithShadow(s1, l, i1, i);
        }

        boolean flag2 = this.cursorPosition < text2.length() || text2.length() >= this.getMaxStringLength();
        int k1 = j1;

        if (!flag) {
            k1 = j > 0 ? l + this.width : l;
        } else if (flag2) {
            k1 = j1 - 1;
            --j1;
        }

        if (!s.isEmpty() && flag && j < s.length()) {
            this.fontRenderer.drawStringWithShadow(s.substring(j), j1, i1, i);
        }

        if (flag1) {
            if (flag2) {
                Gui.drawRect(k1, i1 - 1, k1 + 1, i1 + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
            } else {
                this.fontRenderer.drawStringWithShadow("_", k1, i1, i);
            }
        }

        if (k != j) {
            int l1 = l + this.fontRenderer.getStringWidth(s.substring(0, k));
            this.drawSelectionBox(k1, i1 - 1, l1 - 1, i1 + 1 + this.fontRenderer.FONT_HEIGHT);
        }
    }

    /** 파란 선택 상자 */
    private void drawSelectionBox(int startX, int startY, int endX, int endY) {
        if (startX < endX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        if (startY < endY) {
            int j = startY;
            startY = endY;
            endY = j;
        }
        if (endX > this.x + this.width) {
            endX = this.x + this.width;
        }
        if (startX > this.x + this.width) {
            startX = this.x + this.width;
        }

        Tessellator tessellator = Tessellator.instance;
        GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glLogicOp(GL11.GL_OR_REVERSE);
        tessellator.startDrawingQuads();
        tessellator.addVertex(startX, endY, 0.0D);
        tessellator.addVertex(endX, endY, 0.0D);
        tessellator.addVertex(endX, startY, 0.0D);
        tessellator.addVertex(startX, startY, 0.0D);
        tessellator.draw();
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public void setMaxStringLength(int length) {
        this.maxStringLength = length;
        if (this.text.length() > length) {
            this.text = this.text.substring(0, length);
        }
    }

    public int getMaxStringLength() {
        return this.maxStringLength;
    }

    public int getCursorPosition() {
        return this.cursorPosition;
    }

    public boolean getEnableBackgroundDrawing() {
        return this.enableBackgroundDrawing;
    }

    public void setEnableBackgroundDrawing(boolean enableBackgroundDrawingIn) {
        this.enableBackgroundDrawing = enableBackgroundDrawingIn;
    }

    public void setTextColor(int color) {
        this.enabledColor = color;
    }

    public void setDisabledTextColour(int color) {
        this.disabledColor = color;
    }

    /** 원작과 다름: 원작은 currentScreen.setFocused도 불렀지만 1.7.10 GuiScreen에는 그 메서드가 없다 */
    public void setFocused(boolean isFocusedIn) {
        if (isFocusedIn && !this.isFocused) {
            this.cursorCounter = 0;
        }
        if (isFocusedIn != isFocused) {
            switchFocused(isFocusedIn);
        }
        this.isFocused = isFocusedIn;
    }

    /** 포커스가 바뀌기 직전에 호출된다 (isFocused는 아직 이전 값) */
    protected void switchFocused(boolean isFocusedIn) {}

    public boolean isFocused() {
        return this.isFocused;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public int getSelectionEnd() {
        return this.selectionEnd;
    }

    public int getWidth() {
        return this.getEnableBackgroundDrawing() ? this.width - 8 : this.width;
    }

    public void setSelectionPos(int position) {
        int i = this.text.length();
        position = Math.max(0, Math.min(position, i));
        this.selectionEnd = position;

        if (this.fontRenderer != null) {
            if (this.lineScrollOffset > i) {
                this.lineScrollOffset = i;
            }

            int j = this.getWidth();
            String s = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), j);
            int k = s.length() + this.lineScrollOffset;

            if (position == this.lineScrollOffset) {
                this.lineScrollOffset -= this.fontRenderer.trimStringToWidth(this.text, j, true)
                    .length();
            }

            if (position > k) {
                this.lineScrollOffset += position - k;
            } else if (position <= this.lineScrollOffset) {
                this.lineScrollOffset -= this.lineScrollOffset - position;
            }

            this.lineScrollOffset = Math.max(0, Math.min(this.lineScrollOffset, i));
        }
    }

    public void setCanLoseFocus(boolean canLoseFocusIn) {
        this.canLoseFocus = canLoseFocusIn;
    }

    public boolean getVisible() {
        return this.visible;
    }

    public void setVisible(boolean isVisible) {
        this.visible = isVisible;
    }
}
