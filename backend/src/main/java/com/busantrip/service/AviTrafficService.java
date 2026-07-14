package com.busantrip.service;

import com.busantrip.client.AviTrafficClient;
import com.busantrip.dto.traffic.AviTrafficApiResponse;
import com.busantrip.dto.traffic.AviTrafficResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

@Service
public class AviTrafficService {

    private final AviTrafficClient aviTrafficClient;

    public AviTrafficService(AviTrafficClient aviTrafficClient) {
        this.aviTrafficClient = aviTrafficClient;
    }

    public Mono<List<AviTrafficResponse>> getTraffic() {
        return aviTrafficClient.fetch().map(this::toResponses);
    }

    private List<AviTrafficResponse> toResponses(AviTrafficApiResponse response) {
        if (response.content() == null || response.content().items() == null
                || response.content().items().isNull()) {
            return List.of();
        }

        JsonNode items = response.content().items();
        List<AviTrafficResponse> result = new ArrayList<>();
        if (items.isArray()) {
            items.forEach(item -> addIfCoordinateExists(item, result));
        } else if (items.isObject()) {
            addIfCoordinateExists(items, result);
        }
        return List.copyOf(result);
    }

    private void addIfCoordinateExists(JsonNode item, List<AviTrafficResponse> result) {
        JsonNode latitudeNode = item.get("lat");
        JsonNode longitudeNode = item.get("lot");
        if (!isCoordinate(latitudeNode, -90, 90) || !isCoordinate(longitudeNode, -180, 180)) {
            return;
        }

        result.add(new AviTrafficResponse(
                text(item, "aviSpotNm"),
                text(item, "statsDt"),
                number(item, "vol"),
                latitudeNode.doubleValue(),
                longitudeNode.doubleValue()
        ));
    }

    private boolean isCoordinate(JsonNode node, double min, double max) {
        if (node == null || !node.isNumber()) {
            return false;
        }
        double value = node.doubleValue();
        return Double.isFinite(value) && value >= min && value <= max;
    }

    private String text(JsonNode item, String fieldName) {
        JsonNode value = item.get(fieldName);
        return value != null && value.isString() ? value.stringValue() : "";
    }

    private long number(JsonNode item, String fieldName) {
        JsonNode value = item.get(fieldName);
        return value != null && value.isNumber() ? value.longValue() : 0;
    }
}
