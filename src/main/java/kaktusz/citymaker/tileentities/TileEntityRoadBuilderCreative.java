package kaktusz.citymaker.tileentities;

public class TileEntityRoadBuilderCreative extends TileEntityRoadBuilder {
	@Override
	protected boolean needsEnergy() {
		return false;
	}

	@Override
	protected boolean consumesSupplies() {
		return false;
	}
}
