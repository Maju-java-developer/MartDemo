package raven.modal.demo.tables;

import lombok.Getter;

import javax.swing.*;
import java.util.function.BiConsumer;

@Getter
public class ActionItem {
    private final String text;
    private final Icon icon;
    // BiConsumer for the action: takes JTable and the row index
    private final BiConsumer<JTable, Integer> action;

    /**
     * Creates an ActionItem with text and an action.
     * @param text The button text.
     * @param action The action to execute (JTable, row index).
     */
    public ActionItem(String text, BiConsumer<JTable, Integer> action) {
        this(text, null, action);
    }

    /**
     * Creates an ActionItem with an icon and an action.
     * @param icon The button icon.
     * @param action The action to execute (JTable, row index).
     */
    public ActionItem(Icon icon, BiConsumer<JTable, Integer> action) {
        this(null, icon, action);
    }

    /**
     * Creates an ActionItem with optional text/icon and an action.
     * @param text The button text (can be null if icon is present).
     * @param icon The button icon (can be null if text is present).
     * @param action The action to execute (JTable, row index).
     */
    public ActionItem(String text, Icon icon, BiConsumer<JTable, Integer> action) {
        if (text == null && icon == null) {
            throw new IllegalArgumentException("ActionItem must have either text or an icon.");
        }
        this.text = text;
        this.icon = icon;
        this.action = action;
    }

    public String getText() { return text; }
    public Icon getIcon() { return icon; }
    public BiConsumer<JTable, Integer> getAction() { return action; }
}