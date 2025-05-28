package runrush.be.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeoJsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode parseGeoJson(String geoJsonString) {
        if (geoJsonString == null || geoJsonString.trim().isEmpty()) {
            return mapper.createObjectNode();
        }

        try {
            return mapper.readTree(geoJsonString);
        } catch (Exception e) {
            log.error("GeoJSON 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("GeoJSON 파싱 오류: " + e.getMessage(), e);
        }
    }
}