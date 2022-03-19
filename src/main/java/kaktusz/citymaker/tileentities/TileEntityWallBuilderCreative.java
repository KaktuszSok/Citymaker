package kaktusz.citymaker.tileentities;

public class TileEntityWallBuilderCreative extends TileEntityWallBuilder {
	@Override
	protected boolean needsEnergy() {
		return false;
	}

	@Override
	protected boolean consumesSupplies() {
		return false;
	}
}
