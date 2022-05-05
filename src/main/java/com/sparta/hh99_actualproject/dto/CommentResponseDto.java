package com.sparta.hh99_actualproject.dto;

import com.sparta.hh99_actualproject.model.Board;
import com.sparta.hh99_actualproject.model.Member;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Getter
public class CommentResponseDto {

    private Long commentId;

    private Long boardPostId;

    private String member;

    private String comment;

    private LocalDateTime createdAt;

    private boolean liked;
}