package ru.practicum.shareit.item.dto;

import ru.practicum.shareit.item.model.Comment;

public class CommentMapper {

    public static Comment toComment(CommentDto commentDto) {
        return new Comment(commentDto.getId(), commentDto.getText(), null, null, commentDto.getCreated());
    }

    public static CommentDto toCommentDto(Comment comment) {
        return new CommentDto(comment.getId(),
                comment.getText(),
                comment.getItem().getUserId(),
                comment.getAuthor().getName(),
                comment.getCreated());
    }

}
