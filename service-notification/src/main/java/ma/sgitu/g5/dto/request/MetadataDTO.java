package ma.sgitu.g5.dto.request;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Schema(description = "Métadonnées métier / variables du template (format libre)")
public class MetadataDTO {

    @Schema(description = "Données absorbées dynamiquement (ex: missionCode, lineId, montant...)",
            example = "{\"missionCode\":\"MIS-2026-0012\",\"lineId\":12}")
    private Map<String, Object> data = new HashMap<>();

    @JsonAnySetter
    public void add(String key, Object value) {
        this.data.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getData() {
        return data;
    }
}