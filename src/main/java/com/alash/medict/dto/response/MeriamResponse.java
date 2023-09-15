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
public class MeriamResponse {
    private MetaDto meta;
    private Hwi hwi;
    private String fl;
    private List<Def> def;
    private List<String> shortdef;
}








