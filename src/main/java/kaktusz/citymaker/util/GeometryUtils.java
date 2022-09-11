package kaktusz.citymaker.util;

import net.minecraft.util.math.Vec2f;

public class GeometryUtils {

	/**
	 * Finds the intersection between two lines each defined by a point and a direction (lines stretch infinitely)
	 */
	public static Vec2f findLinesIntersect(Vec2f p1, Vec2f dir1, Vec2f p2, Vec2f dir2)
	{
		float c1 = dir1.y * p1.x - dir1.x * p1.y;
		float c2 = dir2.y * p2.x - dir2.x * p2.y;
		float delta = dir1.x*dir2.y - dir1.y*dir2.x;

		if (delta == 0) return new Vec2f(Float.NaN, Float.NaN);
		return new Vec2f((dir1.x * c2 - dir2.x * c1) / delta, (dir1.y * c2 - dir2.y * c1) / delta);
	}

	/**
	 * Computes the anticlockwise angle between two vectors a,b in the range 0-360 degrees
	 */
	public static float deltaAngle360(Vec2f a, Vec2f b)
	{
		float angle = (float)Math.toDegrees(Math.atan2(a.x*b.y - a.y*b.x, a.x*b.x + a.y*b.y));
		if (Float.isNaN(angle)) return 0f;
		if (angle < 0)
			angle += 360f;
		return angle;
	}

	/**
	 * Computes the signed anticlockwise angle between two vectors a,b in the range -180 to +180 degrees
	 */
	public static float signedAngle(Vec2f a, Vec2f b) {
		return (float)Math.toDegrees(Math.atan2(a.x*b.y - a.y*b.x, a.x*b.x + a.y*b.y));
	}
}
