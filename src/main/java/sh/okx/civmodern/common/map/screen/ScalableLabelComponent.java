package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.AnimatableProperty;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.util.Observable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ScalableLabelComponent extends BaseComponent {

    protected final Font textRenderer = Minecraft.getInstance().font;

    protected Component text;
    protected List<FormattedCharSequence> wrappedText;

    protected VerticalAlignment verticalTextAlignment = VerticalAlignment.TOP;
    protected HorizontalAlignment horizontalTextAlignment = HorizontalAlignment.LEFT;

    protected final AnimatableProperty<Color> color = AnimatableProperty.of(Color.WHITE);
    protected final Observable<Integer> lineHeight = Observable.of(this.textRenderer.lineHeight);
    protected final Observable<Integer> lineSpacing = Observable.of(2);
    protected boolean shadow;
    protected int maxWidth;

    protected float scale;

    protected boolean hover = true;

    protected Consumer<ScalableLabelComponent> onPress;

    protected ScalableLabelComponent(Component text, Consumer<ScalableLabelComponent> onPress) {
        this.text = text;
        this.wrappedText = new ArrayList<>();

        this.shadow = false;
        this.maxWidth = Integer.MAX_VALUE;

        this.onPress = onPress;

        Observable.observeAll(this::notifyParentIfMounted, this.lineHeight, this.lineSpacing);
    }

    public ScalableLabelComponent textHeight(int height) {
        this.scale = height / (float) this.textRenderer.lineHeight;
        this.lineHeight.set(height);
        return this;
    }

    public ScalableLabelComponent text(Component text) {
        this.text = text;
        this.notifyParentIfMounted();
        return this;
    }

    public Component text() {
        return this.text;
    }

    public ScalableLabelComponent maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        this.notifyParentIfMounted();
        return this;
    }

    public ScalableLabelComponent hoverEffect(boolean hover) {
        this.hover = hover;
        return this;
    }

    public int maxWidth() {
        return this.maxWidth;
    }

    public ScalableLabelComponent shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public boolean shadow() {
        return this.shadow;
    }

    public ScalableLabelComponent color(Color color) {
        this.color.set(color);
        return this;
    }

    public AnimatableProperty<Color> color() {
        return this.color;
    }

    public ScalableLabelComponent verticalTextAlignment(VerticalAlignment verticalAlignment) {
        this.verticalTextAlignment = verticalAlignment;
        return this;
    }

    public VerticalAlignment verticalTextAlignment() {
        return this.verticalTextAlignment;
    }

    public ScalableLabelComponent horizontalTextAlignment(HorizontalAlignment horizontalAlignment) {
        this.horizontalTextAlignment = horizontalAlignment;
        return this;
    }

    public HorizontalAlignment horizontalTextAlignment() {
        return this.horizontalTextAlignment;
    }

    public ScalableLabelComponent lineHeight(int lineHeight) {
        this.lineHeight.set(lineHeight);
        return this;
    }

    public int lineHeight() {
        return this.lineHeight.get();
    }

    public ScalableLabelComponent lineSpacing(int lineSpacing) {
        this.lineSpacing.set(lineSpacing);
        return this;
    }

    public int lineSpacing() {
        return this.lineSpacing.get();
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        int widestText = 0;
        for (var line : this.wrappedText) {
            int width = this.textRenderer.width(line);
            if (width > widestText) widestText = width;
        }

        if (widestText > this.maxWidth) {
            this.wrapLines();
            return this.determineHorizontalContentSize(sizing);
        } else {
            return widestText;
        }
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        this.wrapLines();
        return this.textHeight();
    }

    @Override
    public void inflate(Size space) {
        this.wrapLines();
        super.inflate(space);
    }

    private void wrapLines() {
        this.wrappedText = this.textRenderer.split(this.text, this.horizontalSizing.get().isContent() ? this.maxWidth : this.width);
    }

    protected int textHeight() {
        return (this.wrappedText.size() * (this.lineHeight() + this.lineSpacing())) - this.lineSpacing();
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        this.color.update(delta);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        var matrices = context.pose();

        matrices.pushMatrix();
        matrices.translate(0, 1 / Minecraft.getInstance().getWindow().getGuiScale());

        int x = this.x;
        int y = this.y;

        if (this.horizontalSizing.get().isContent()) {
            x += this.horizontalSizing.get().value;
        }
        if (this.verticalSizing.get().isContent()) {
            y += this.verticalSizing.get().value;
        }

        switch (this.verticalTextAlignment) {
            case CENTER -> y += (this.height - (this.textHeight())) / 2;
            case BOTTOM -> y += this.height - (this.textHeight());
        }

        final int lambdaX = x;
        final int lambdaY = y;

        final float lambdaScale = scale;

        for (int i = 0; i < this.wrappedText.size(); i++) {
            var renderText = this.wrappedText.get(i);
            int renderX = lambdaX;

            switch (this.horizontalTextAlignment) {
                case CENTER -> renderX += (this.width - this.textRenderer.width(renderText)) / 2;
                case RIGHT -> renderX += this.width - this.textRenderer.width(renderText);
            }

            int renderY = lambdaY + i * (this.lineHeight() + this.lineSpacing());
//                renderY += this.lineHeight() - this.textRenderer.lineHeight;

            context.push();
            context.scale(lambdaScale, lambdaScale);
            float sx = renderX / lambdaScale;
            float sy = renderY / lambdaScale;
            float left = sx - margins.get().left();
            float top = sy;
            float right = sx + (int) (this.space.width() / lambdaScale);
            float bottom = sy + height + margins.get().bottom();
            float cx = mouseX / lambdaScale;
            float cy = mouseY / lambdaScale;
            if (hover && cx >= left && cx <= right && cy >= top - 2 && cy <= bottom + (2)) {
                context.fill((int) left, (int) top - 1, (int) right, (int) bottom + 1, 0xffa09f9b);

                context.drawLine((int) top, (int) left, (int) bottom, (int) right, 1, color.get());
//                    Tesselator tessellator = Tesselator.getInstance();
//                    BufferBuilder buffer = tessellator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
//                    Matrix4f matrix4f = context.getMatrixStack().last().pose();
//                    buffer.addVertex(matrix4f, top, left, 0).setColor(colour);
//                    buffer.addVertex(matrix4f, top, right, 0).setColor(colour);
//                    buffer.addVertex(matrix4f, bottom, right, 0).setColor(colour);
//                    buffer.addVertex(matrix4f, bottom, left, 0).setColor(colour);
//                    RenderType.gui().draw(buffer.buildOrThrow());
            }
            context.drawString(this.textRenderer, renderText, (int) sx, (int) sy, this.color.get().argb(), this.shadow);


            context.pop();
        }

        matrices.popMatrix();
    }

    @Override
    public void drawTooltip(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        super.drawTooltip(context, mouseX, mouseY, partialTicks, delta);
        context.renderComponentHoverEffect(this.textRenderer, this.styleAt(mouseX - this.x, mouseY - this.y), mouseX, mouseY);
    }

    @Override
    public boolean shouldDrawTooltip(double mouseX, double mouseY) {
        var hoveredStyle = this.styleAt((int) (mouseX - this.x), (int) (mouseY - this.y));
        return super.shouldDrawTooltip(mouseX, mouseY) || (hoveredStyle != null && hoveredStyle.getHoverEvent() != null && this.isInBoundingBox(mouseX, mouseY));
    }

    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        float sx = (x / scale);
        float sy = (y / scale);
        float left = sx - margins.get().left();
        float top = sy - 1;
        float right = sx + (int) (this.space.width() / scale);
        float bottom = sy + height + 2;
        double cx = mouseX / scale;
        double cy = mouseY / scale;
        if (hover && cx >= left && cx <= right && cy >= top - 2 && cy <= bottom + (2)) {
            this.onPress.accept(this);
            return true;
        }
        return false;
    }

    protected Style styleAt(int mouseX, int mouseY) {
        return this.textRenderer.getSplitter().componentStyleAtWidth(this.wrappedText.get(Math.min(mouseY / (this.lineHeight() + this.lineSpacing()), this.wrappedText.size() - 1)), mouseX);
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);
        UIParsing.apply(children, "text", UIParsing::parseText, this::text);
        UIParsing.apply(children, "max-width", UIParsing::parseUnsignedInt, this::maxWidth);
        UIParsing.apply(children, "color", Color::parse, this::color);
        UIParsing.apply(children, "shadow", UIParsing::parseBool, this::shadow);
        UIParsing.apply(children, "line-height", UIParsing::parseUnsignedInt, this::lineHeight);
        UIParsing.apply(children, "line-spacing", UIParsing::parseUnsignedInt, this::lineSpacing);

        UIParsing.apply(children, "vertical-text-alignment", VerticalAlignment::parse, this::verticalTextAlignment);
        UIParsing.apply(children, "horizontal-text-alignment", HorizontalAlignment::parse, this::horizontalTextAlignment);
    }
}
