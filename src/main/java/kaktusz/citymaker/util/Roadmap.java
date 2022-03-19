package kaktusz.citymaker.util;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import net.minecraft.util.math.MathHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("UnstableApiUsage")
public class Roadmap {

	public enum CellState {
		PREVIEW_NONE(255,255,0),
		EMPTY(64,64,64),
//		TERRAIN_LIQUID(79,125,255),
//		TERRAIN_SOLID(65,127,0),
		FOOTPATH(128,128,128),
		ROAD(20,20,20),
		CENTRELINE(0,0,255);

		public final int r,g,b;
		CellState(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public static CellState fromByte(byte b) {
			return CellState.values()[b];
		}

		public byte toByte() {
			return (byte)this.ordinal();
		}
	}

	public byte[][] statemap;
	public short[][] heightmap;
	public boolean[][] terrainmap;
	private boolean hasTerrainData = false;

	/**
	 * Maximum size of the map in chunks along any axis
	 */
	private static final int MAX_CHUNK_SIZE = 32;

	/**
	 * Initialise the roadmap's arrays.
	 * @param sizeX Size of roadmap in blocks along X axis.
	 * @param sizeZ Size of roadmap in blocks along Z axis.
	 */
	public Roadmap initialise(int sizeX, int sizeZ) {
		statemap = new byte[sizeX][sizeZ];
		heightmap = new short[sizeX][sizeZ];
		terrainmap = new boolean[sizeX][sizeZ];

		return this;
	}

	public void markAsHavingTerrainData() {
		hasTerrainData = true;
	}

	@Override
	public String toString() {
		if(statemap.length < 1)
			return "(Roadmap of degenerate size)";
		return "(Roadmap of size " + statemap.length + "x" + statemap[0].length + ")";
	}

	public String ascii() {
		StringBuilder sb = new StringBuilder();
		int sizeZ = statemap[0].length;
		for (int z = sizeZ-1; z >= 0; z--) {
			for (int x = 0; x < statemap.length; x++) {
				sb.append(statemap[x][z]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public int getArea() {
		if(statemap.length < 1)
			return 0;

		return statemap.length * statemap[0].length;
	}

	public int getColour(int x, int z) {
		CellState state = CellState.fromByte(statemap[x][z]);
		int r,g,b;
		if(state != CellState.EMPTY) {
			r = state.r;
			g = state.g;
			b = state.b;
		} else {
			if(terrainmap[x][z]) {
				r = 65;
				g = 127;
				b = 0;
			} else {
				r = 79;
				g = 125;
				b = 255;
			}
		}

		short height = heightmap[x][z];
		short heightNorth = height;
		if(z < heightmap[0].length - 1)
			heightNorth = heightmap[x][z+1];

		if(height < heightNorth) {
			r -= 15;
			g -= 15;
			b -= 15;
		} else if(height > heightNorth) {
			r += 15;
			g += 15;
			b += 15;
		}

		r = MathHelper.clamp(r, 0, 255);
		g = MathHelper.clamp(g, 0, 255);
		b = MathHelper.clamp(b, 0, 255);

		return ColourUtils.colourAsInt(r,g,b);
	}

	public static Roadmap fromBase64(String dataStr) {
		try {
			Roadmap roadmap = new Roadmap();

			byte[] data = Base64.getDecoder().decode(dataStr);
			try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data));
				 LittleEndianDataInputStream in = new LittleEndianDataInputStream(gz)) {

				char c;
				while ((c = in.readChar()) != '0') {
					log("Read section header char: '" + c + "'.");
					switch (c) {
						case 'M':
							readMapData(roadmap, in);
							break;
						case 'G':
							readGridData(roadmap, in);
							break;
						case 'T':
							readTerrainData(roadmap, in);
							break;
						default:
							continue;
					}
				}

				if(roadmap.statemap == null || roadmap.getArea() == 0)
					return null;

				return roadmap;
			} catch (IOException | IllegalArgumentException e) {
				e.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			log("Invalid base64 string: " + dataStr);
		}
		return null;
	}

	private static void readMapData(Roadmap roadmap, LittleEndianDataInputStream in) throws IOException, IllegalArgumentException {
		int chunksX = in.readInt();
		int chunksZ = in.readInt();
		if(chunksX > MAX_CHUNK_SIZE || chunksZ > MAX_CHUNK_SIZE)
			throw new IllegalArgumentException("Chunk size too large! (" + chunksX + "x" + chunksZ + ", max=" + MAX_CHUNK_SIZE + ")");
		roadmap.initialise(chunksX*16, chunksZ*16);
		log("Read map data - size " + chunksX + "x" + chunksZ);
	}

	private static void readGridData(Roadmap roadmap, LittleEndianDataInputStream in) throws IOException {
		for (int cx = 0; cx < roadmap.statemap.length/16; cx++) {
			for(int cz = 0; cz < roadmap.statemap[cx].length/16; cz++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						roadmap.statemap[cx*16 + x][cz*16 + z] = in.readByte();
					}
				}
			}
		}
	}

	private static void readTerrainData(Roadmap roadmap, LittleEndianDataInputStream in) throws IOException {
		for (int cx = 0; cx < roadmap.statemap.length/16; cx++) {
			for(int cz = 0; cz < roadmap.statemap[cx].length/16; cz++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						roadmap.heightmap[cx*16 + x][cz*16 + z] = in.readShort();
						roadmap.terrainmap[cx*16 + x][cz*16 + z] = in.readBoolean();
					}
				}
			}
		}
		roadmap.hasTerrainData = true;
	}

	public static String toBase64(Roadmap roadmap) {
		return toBase64(roadmap, true);
	}

	public static String toBase64(Roadmap roadmap, boolean includeTerrainData) {
		if(roadmap == null)
			return "";

		try(ByteArrayOutputStream bs = new ByteArrayOutputStream()) {

			try (LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(bs)) {
				writeMapData(roadmap, out);
				writeGridData(roadmap, out);
				if(includeTerrainData)
					writeTerrainData(roadmap, out);

				out.writeChar('0');
			}
			bs.flush();
			byte[] bytes = bs.toByteArray();

			try(ByteArrayOutputStream gzOut = new ByteArrayOutputStream()) {
				try(GZIPOutputStream gz = new GZIPOutputStream(gzOut)) {
					gz.write(bytes);
				}
				gzOut.flush();

				byte[] bytesCompressed = gzOut.toByteArray();
				return Base64.getEncoder().encodeToString(bytesCompressed);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "ERROR";
		}
	}

	private static void writeMapData(Roadmap roadmap, LittleEndianDataOutputStream out) throws IOException {
		out.writeChar('M');

		out.writeInt(roadmap.statemap.length/16);
		out.writeInt(roadmap.statemap[0].length/16);
	}

	private static void writeGridData(Roadmap roadmap, LittleEndianDataOutputStream out) throws IOException {
		out.writeChar('G');

		for (int cx = 0; cx < roadmap.statemap.length/16; cx++) {
			for(int cz = 0; cz < roadmap.statemap[cx].length/16; cz++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						out.writeByte(roadmap.statemap[cx*16 + x][cz*16 + z]);
					}
				}
			}
		}
	}

	private static void writeTerrainData(Roadmap roadmap, LittleEndianDataOutputStream out) throws IOException {
		out.writeChar('T');

		for (int cx = 0; cx < roadmap.statemap.length/16; cx++) {
			for(int cz = 0; cz < roadmap.statemap[cx].length/16; cz++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						out.writeShort(roadmap.heightmap[cx*16 + x][cz*16 + z]);
						out.writeBoolean(roadmap.terrainmap[cx*16 + x][cz*16 + z]);
					}
				}
			}
		}
	}

	public static void mergeGridData(Roadmap source, Roadmap target) {
		target.statemap = source.statemap;
	}

	private static void log(String message) {
		//Citymaker.logger.info(message);
		System.out.println(message);
	}
}
