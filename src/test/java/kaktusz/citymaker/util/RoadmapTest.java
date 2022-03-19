package kaktusz.citymaker.util;

import com.google.common.io.LittleEndianDataInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("UnstableApiUsage")
public class RoadmapTest {

	private static final String dataStr = Roadmap.toBase64(new Roadmap().initialise(16*16, 9*16));

	@Test
	public void printBase64Bytes() {
		byte[] data = Base64.getDecoder().decode(dataStr);
		StringBuilder bytesStr = new StringBuilder("bytes: ");
		for (byte b : data) {
			bytesStr.append(b).append(",");
		}
		System.out.println(bytesStr.toString());
	}

	@Test
	public void printStream() {
		byte[] data = Base64.getDecoder().decode(dataStr);
		StringBuilder bytesStr = new StringBuilder("bytes: ");
		try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data));
			 LittleEndianDataInputStream in = new LittleEndianDataInputStream(gz)) {

			while (true) {
				try {
					bytesStr.append(in.readInt()).append(",");
				} catch (EOFException e) {
					break;
				}
			}
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
			assert false;
		}
		System.out.println(bytesStr.toString());
	}

	@Test
	public void fromBase64() {
		Roadmap roadmap = Roadmap.fromBase64(dataStr);
		System.out.println("Result: " + roadmap);
		Assert.assertNotNull(roadmap);
		System.out.println(roadmap.ascii());
	}
}