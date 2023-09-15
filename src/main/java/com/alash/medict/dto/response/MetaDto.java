package com.alash.medict.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaDto {
    private String id;
    private String uuid;
    private String sort;
    private String src;
    private String section;
    private List<String> stems;
    private boolean offensive;


}
