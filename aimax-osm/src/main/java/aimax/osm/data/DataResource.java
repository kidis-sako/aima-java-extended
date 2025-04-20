package aimax.osm.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Provides a stream with OSM map data describing the city of Ulm.
 * @author Ruediger Lunde, Kidis Sako
 */
public class DataResource {
	public static InputStream getUlmFileResource() {
		// First try to load from resources
		InputStream is = DataResource.class.getResourceAsStream("ulm.osm");
		
		// If that fails, try to load from absolute path
		if (is == null) {
			try {
				File file = new File("C:/dev/Other projects/aima-java/aimax-osm/src/main/resources/aimax/osm/data/ulm.osm");
				if (file.exists()) {
					is = new FileInputStream(file);
				} else {
					System.err.println("Map file not found at: " + file.getAbsolutePath());
				}
			} catch (Exception e) {
				System.err.println("Error loading map from file: " + e.getMessage());
			}
		}
		
		return is;
	}
}
