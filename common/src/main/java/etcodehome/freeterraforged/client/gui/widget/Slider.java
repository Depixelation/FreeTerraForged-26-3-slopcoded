package etcodehome.freeterraforged.client.gui.widget;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

//TODO this should be cleaned up
public class Slider extends AbstractSliderButton {
	private double min;
	private double max;
	private Component name;
	private Format format;
	@Nullable
	private Callback callback;
	private boolean isApplying = false;

	public Slider(int x, int y, int width, int height, float initialValue, float min, float max, Component name, Format format, @Nullable Callback callback) {
		super(x, y, width, height, CommonComponents.EMPTY, 0.0);
		this.name = name;
		this.format = format;
		this.min = min;
		this.max = max;
		this.callback = callback;
		this.value = this.getSliderValue(initialValue);
		this.updateMessage();
	}

	@Override
	public void setValue(double value) {
		double prev = this.value;
		this.value = Double.isNaN(value) ? 0.0 : Mth.clamp(value, 0.0, 1.0);
		this.updateMessage();
		if (!this.isApplying && prev != this.value) {
			this.applyValue();
		}
	}

	public double getValue() {
		return this.value;
	}

	public double getMin() {
		return this.min;
	}

	public double getMax() {
		return this.max;
	}

	public void setRange(double min, double max) {
		double current = this.getLerpedValue();
		this.min = min;
		this.max = max;
		this.setValue(this.getSliderValue((float) current));
	}

	public double getSliderValue(float value) {
		double range = this.max - this.min;
		if (range <= 0.0) {
			return 0.0;
		}
		return (Mth.clamp(value, (float) this.min, (float) this.max) - this.min) / range;
	}

	public double getLerpedValue() {
		return this.lerpValue(this.value);
	}

	public double lerpValue(double value) {
		return Mth.lerp(Mth.clamp(value, 0.0, 1.0), this.min, this.max);
	}

	public double scaleValue(double value) {
		return this.format.scale(this.lerpValue(value));
	}

	@Override
	public void applyValue() {
		if (this.callback != null && !this.isApplying) {
			this.isApplying = true;
			try {
				double adjusted = this.callback.apply(this, this.value);
				this.value = Double.isNaN(adjusted) ? 0.0 : Mth.clamp(adjusted, 0.0, 1.0);
				this.updateMessage();
			} finally {
				this.isApplying = false;
			}
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		this.applyValue();
	}

	@Override
	protected void updateMessage() {
		this.setMessage(CommonComponents.optionNameValue(this.name, Component.literal(this.format.getMessage(this.scaleValue(this.value)))));
	}

	public static enum Format {
		INT {
			@Override
			public double scale(double input) {
				return (int) input;
			}

			@Override
			public String getMessage(double input) {
				return String.valueOf((int) input);
			}
		},
		FLOAT {
			@Override
			public double scale(double input) {
				return input;
			}

			@Override
			public String getMessage(double input) {
				return String.format("%.3f", input);
			}
		};

		public abstract double scale(double input);

		public abstract String getMessage(double input);
	}

	public interface Callback {
		double apply(Slider slider, double value);
	}
}