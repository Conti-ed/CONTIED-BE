package com.contied.conti.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PostContiByCreationRequest {
    private String title;
    private String description;
    private List<Long> songs;
}
