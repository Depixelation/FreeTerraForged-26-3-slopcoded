package etcodehome.freeterraforged.client.gui.widget;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;

public class WidgetList<T extends AbstractWidget> extends ContainerObjectSelectionList<WidgetList.Entry<T>> {
	private boolean renderSelected;
	
    public WidgetList(Minecraft minecraft, int i, int j, int k, int l) {
        super(minecraft, i, j, k, l);
    }

    public void select(T widget) {
    	for(Entry<T> entry : this.children()) {
    		if(entry.widget.equals(widget)) {
    			this.setSelected(entry);
    			return;
    		}
    	}
    }
    
    public <W extends T> W addWidget(W widget) {
        super.addEntry(new Entry<>(widget));
        return widget;
    }

    public void setRenderSelected(boolean renderSelected) {
    	this.renderSelected = renderSelected;
    }

    @Override
    protected boolean entriesCanBeSelected() {
        return this.renderSelected;
    }

    @Override
    protected void extractSelection(GuiGraphicsExtractor graphics, Entry<T> entry, int outlineColor) {
        if (this.renderSelected) {
            super.extractSelection(graphics, entry, outlineColor);
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight();
    }

    public static class Entry<T extends AbstractWidget> extends ContainerObjectSelectionList.Entry<Entry<T>> {
        private T widget;

        public Entry(T widget) {
            this.widget = widget;
        }

        public T getWidget() {
        	return this.widget;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean isHovered, float partialTicks) {
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            int optionWidth = Math.min(396, width);
            int padding = (width - optionWidth) / 2;
            widget.setX(this.getContentX() + padding);
            widget.setY(this.getContentY());
            widget.visible = true;
            widget.setWidth(optionWidth);
            widget.setHeight(height - 1);
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        }

		@Override
		public List<T> narratables() {
			return Collections.singletonList(this.widget);
		}
    }
}
