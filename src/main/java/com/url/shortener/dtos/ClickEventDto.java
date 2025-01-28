package com.url.shortener.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClickEventDto {
    private LocalDate clickDate;
    private Long count;
}
