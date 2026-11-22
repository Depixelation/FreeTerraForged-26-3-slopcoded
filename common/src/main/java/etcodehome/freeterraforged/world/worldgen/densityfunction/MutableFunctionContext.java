package etcodehome.freeterraforged.world.worldgen.densityfunction;

import etcodehome.freeterraforged.world.worldgen.densityfunction.MarkerFunction.FunctionContext;

public class MutableFunctionContext implements FunctionContext {
	private int blockX, blockY, blockZ;

	public MutableFunctionContext at(int x, int y, int z) {
		this.blockX = x;
		this.blockY = y;
		this.blockZ = z;
		return this;
	}
	
	@Override
	public int blockX() {
		return this.blockX;
	}

	@Override
	public int blockY() {
		return this.blockY;
	}

	@Override
	public int blockZ() {
		return this.blockZ;
	}
}