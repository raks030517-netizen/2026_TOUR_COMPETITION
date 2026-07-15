package com.busantrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.busantrip.client.AviTrafficClient;
import com.busantrip.dto.traffic.AviTrafficApiResponse;
import com.busantrip.dto.traffic.AviTrafficResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AviTrafficServiceTest {

    private final AviTrafficClient client = mock(AviTrafficClient.class);
    private final AviTrafficService service = new AviTrafficService(client);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void convertsOfficialFieldsAndExcludesItemsWithoutCoordinates() throws Exception {
        JsonNode items = objectMapper.readTree("""
                [
                  {
                    "statsDt": "2026-07-14 10:00:00",
                    "aviSpotNm": "광안대교",
                    "lot": 129.1234,
                    "lat": 35.1234,
                    "vol": 1000
                  },
                  {
                    "statsDt": "2026-07-14 10:00:00",
                    "aviSpotNm": "좌표 없음",
                    "vol": 200
                  }
                ]
                """);
        AviTrafficApiResponse response = new AviTrafficApiResponse(
                "00",
                "NORMAL SERVICE",
                new AviTrafficApiResponse.Content(1, 100, 2, items)
        );
        when(client.fetch()).thenReturn(Mono.just(response));

        List<AviTrafficResponse> result = service.getTraffic().block();

        assertThat(result).containsExactly(new AviTrafficResponse(
                "광안대교",
                "2026-07-14 10:00:00",
                1000,
                35.1234,
                129.1234
        ));
    }

    @Test
    void supportsSingleItemObjectFromOfficialSchema() throws Exception {
        JsonNode item = objectMapper.readTree("""
                {
                  "statsDt": "2026-07-14 11:00:00",
                  "aviSpotNm": "수영교차로",
                  "lot": 129.115,
                  "lat": 35.17,
                  "vol": 321
                }
                """);
        when(client.fetch()).thenReturn(Mono.just(new AviTrafficApiResponse(
                "00", "NORMAL SERVICE", new AviTrafficApiResponse.Content(1, 1, 1, item))));

        List<AviTrafficResponse> result = service.getTraffic().block();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stationName()).isEqualTo("수영교차로");
    }

    @Test
    void returnsEmptyListWhenItemsAreMissing() {
        when(client.fetch()).thenReturn(Mono.just(new AviTrafficApiResponse(
                "00", "NORMAL SERVICE", new AviTrafficApiResponse.Content(1, 100, 0, null))));

        assertThat(service.getTraffic().block()).isEmpty();
    }
}
