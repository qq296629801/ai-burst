package com.aiburst.mag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MagKbEntryCreateRequest {

    @NotBlank
    @Size(max = 256)
    private String title;

    @NotBlank
    private String body;

    private String tagsJson;

    @Size(max = 512)
    private String keywords;
}
