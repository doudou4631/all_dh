package com.geek.server.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreeQueryLoginSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String account;
}
