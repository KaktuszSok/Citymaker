package kaktusz.citymaker.util;

public class MutableAABB {
	private double minX,minY,minZ;
	private double maxX,maxY,maxZ;
	
	public MutableAABB(double x, double y, double z) {
		minX = maxX = x;
		minY = maxY = y;
		minZ = maxZ = z;
	}

	public double getMinX() {
		return minX;
	}

	public double getMinY() {
		return minY;
	}

	public double getMinZ() {
		return minZ;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}

	public double getMaxZ() {
		return maxZ;
	}
	
	public double getSizeX() {
		return maxX - minX;
	}
	
	public double getSizeY() {
		return maxY - minY;
	}
	
	public double getSizeZ() {
		return maxZ - minZ;
	}

	/**
	 * Expand the area in such a way that it will now encompass the given point.
	 */
	public void addPoint(double x, double y, double z) {
		if(x < minX)
			minX = x;
		else if(x > maxX)
			maxX = x;

		if(y < minY)
			minY = y;
		else if(y > maxY)
			maxY = y;

		if(z < minZ)
			minZ = z;
		else if(z > maxZ)
			maxZ = z;
	}

	/**
	 * Grow (or shrink) this AABB by a certain amount in each direction.
	 * Shrinking past current size will result in the minimum and maximum values flipping to preserve min <= max.
	 */
	public void grow(double dx, double dy, double dz) {
		growUnchecked(dx, dy, dz);
		fixMinMax();
	}

	/**
	 * Grow (or shrink) this AABB by a certain amount in each direction.
	 * Shrinking past current size will result in the minimum coordinate being greater than the maximum. Exercise caution.
	 */
	public void growUnchecked(double dx, double dy, double dz) {
		minX -= dx;
		maxX += dx;
		minY -= dy;
		maxY += dy;
		minZ -= dz;
		maxZ += dz;
	}
	
	private void fixMinMax() {
		if(maxX < minX) {
			double temp = minX;
			minX = maxX;
			maxX = minX;
		}
		if(maxY < minY) {
			double temp = minY;
			minY = maxY;
			maxY = minY;
		}
		if(maxZ < minZ) {
			double temp = minZ;
			minZ = maxZ;
			maxZ = minZ;
		}
	}
}
