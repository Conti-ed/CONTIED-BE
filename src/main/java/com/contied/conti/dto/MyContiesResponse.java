package com.contied.conti.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyContiesResponse {
    private List<ContiResponse> myContiData;
    private Long nextCursor;
}
