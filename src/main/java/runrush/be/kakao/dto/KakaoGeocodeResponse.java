package runrush.be.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoGeocodeResponse {
    private List<Document> documents;

    public String getFirstAddressName() {
        if (documents != null && !documents.isEmpty()) {
            Address address = documents.get(0).getAddress();
            if (address.getBuildingName() != null && !address.getBuildingName().isBlank()) {
                return address.getBuildingName();
            }
            return address.getRegion2() + " " + address.getRegion3();
        }
        return "주소 정보 없음";
    }

    @Getter
    @NoArgsConstructor
    public static class Document {
        private Address address;
    }

    @Getter
    @NoArgsConstructor
    public static class Address {
        @JsonProperty("address_name")
        private String addressName; // 전체 주소

        @JsonProperty("region_1depth_name")
        private String region1; // 시/도

        @JsonProperty("region_2depth_name")
        private String region2; // 구/군

        @JsonProperty("region_3depth_name")
        private String region3; // 동/읍/면

        @JsonProperty("road_name")
        private String roadName;

        @JsonProperty("building_name")
        private String buildingName;
    }
}