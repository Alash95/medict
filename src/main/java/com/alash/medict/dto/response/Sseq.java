package com.alash.medict.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class Sense {
    private List<List<Object>> senseDetails;
}
