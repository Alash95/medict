package com.alash.medict.controller;

import com.alash.medict.dto.response.FinalResponse;
import com.alash.medict.dto.response.MeriamResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user/dic")
public class APIController {


    private final String apiKey = "6369b959-f9d5-4157-bd2e-a25e70a7a6fe";
    private final String baseUrl = "https://dictionaryapi.com/api/v3/references/";

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/definition/{word}")
    public List<FinalResponse> getDefinition(@PathVariable String word) {
        String uri = baseUrl + "medical" + "/json/" + word + "?key=" + apiKey;
        MeriamResponse[] response = restTemplate.getForObject(uri, MeriamResponse[].class);

        List<FinalResponse> responseList = new ArrayList<>();
        assert response != null;
        for(MeriamResponse response1: response){
            FinalResponse finalResponse = FinalResponse.builder()
                    .word(response1.getMeta().getId())
                    .shortDefinition(response1.getShortdef())
                    .build();
            responseList.add(finalResponse);
        }
        return  responseList;
    }
}

