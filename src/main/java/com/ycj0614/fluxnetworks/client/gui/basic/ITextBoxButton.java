package com.ycj0614.fluxnetworks.client.gui.basic;

import com.ycj0614.fluxnetworks.client.gui.button.TextboxButton;

public interface ITextBoxButton {

    /** 텍스트 상자가 포커스를 잃을 때 호출된다 */
    default void onTextBoxChanged(TextboxButton text) {}
}
