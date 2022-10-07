package kaktusz.citymaker.util;

import com.google.common.base.MoreObjects;
import jdk.nashorn.internal.ir.annotations.Immutable;
import net.minecraft.util.math.MathHelper;

@Immutable
public class Vec2i implements Comparable<Vec2i> {
	public static final Vec2i NULL_VECTOR = new Vec2i(0, 0);
	public final int x;
	public final int y;

	public Vec2i(int xIn, int yIn)
	{
		this.x = xIn;
		this.y = yIn;
	}

	public Vec2i(double xIn, double yIn)
	{
		this(MathHelper.floor(xIn), MathHelper.floor(yIn));
	}

	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		else if (!(other instanceof Vec2i))
		{
			return false;
		}
		else
		{
			Vec2i vec2i = (Vec2i)other;

			if (this.getX() != vec2i.getX())
			{
				return false;
			}
			else
			{
				return this.getY() == vec2i.getY();
			}
		}
	}

	public int hashCode()
	{
		return this.getY() * 31*31 + this.getX();
	}

	public int compareTo(Vec2i other)
	{
		if (this.getY() == other.getY())
		{
			return this.getX() - other.getX();
		}
		else
		{
			return this.getY() - other.getY();
		}
	}

	public int getX()
	{
		return this.x;
	}

	public int getY()
	{
		return this.y;
	}

	public double getDistance(int xIn, int yIn)
	{
		double d0 = (double)(this.getX() - xIn);
		double d1 = (double)(this.getY() - yIn);
		return Math.sqrt(d0 * d0 + d1 * d1);
	}

	public double distanceSq(double toX, double toY)
	{
		double d0 = (double)this.getX() - toX;
		double d1 = (double)this.getY() - toY;
		return d0 * d0 + d1 * d1;
	}

	public double distanceSqToCenter(double xIn, double yIn)
	{
		double d0 = (double)this.getX() + 0.5D - xIn;
		double d1 = (double)this.getY() + 0.5D - yIn;
		return d0 * d0 + d1 * d1;
	}

	public double distanceSq(Vec2i to)
	{
		return this.distanceSq((double)to.getX(), (double)to.getY());
	}

	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("x", this.getX()).add("y", this.getY()).toString();
	}
}
