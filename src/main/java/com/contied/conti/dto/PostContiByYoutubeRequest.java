package com.contied.conti.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostContiByYoutubeRequest {
    private String title;
    private String description;
    
    @JsonProperty("youtubeURL")
    private String youtubeUrl;
}
