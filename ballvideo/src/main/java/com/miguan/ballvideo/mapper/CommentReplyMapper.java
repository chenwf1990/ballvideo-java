package com.miguan.ballvideo.mapper;


import com.miguan.ballvideo.entity.CommentReply;
import com.miguan.ballvideo.entity.CommentReplyRequest;
import com.miguan.ballvideo.entity.CommentReplyResponse;

import java.util.List;

public interface CommentReplyMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CommentReply record);

    int insertSelective(CommentReply record);

    CommentReply selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CommentReply record);

    int updateByPrimaryKey(CommentReply record);

    List<CommentReplyResponse> findAllCommentReply(CommentReplyRequest record);

    List<CommentReplyResponse> findMessageCenter(CommentReplyRequest commentReply);

    int updateCommentReplyByPComment(Long toFromUid);

    int updateCommentReplyByToFromUserIdAndName(CommentReply commentReply);

    int findAllCommentReplyByAlreadyRead(String userId);

    List<CommentReplyResponse> findAllCommentReplyTow(CommentReplyRequest commentReply);

    List<CommentReplyResponse> findOneCommentReply(CommentReplyRequest record);
}