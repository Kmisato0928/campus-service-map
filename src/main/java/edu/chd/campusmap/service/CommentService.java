package edu.chd.campusmap.service;

import edu.chd.campusmap.dao.CommentDAO;
import edu.chd.campusmap.dao.CommentLikeDAO;
import edu.chd.campusmap.model.Comment;

import java.util.List;

public class CommentService {
    private final CommentDAO commentDAO;
    private final CommentLikeDAO likeDAO;

    public CommentService() {
        this.commentDAO = new CommentDAO();
        this.likeDAO = new CommentLikeDAO();
    }

    public List<Comment> getCommentsByBuilding(int buildingId) {
        return commentDAO.findByBuildingId(buildingId);
    }

    /**
     * 获取评论列表，附带当前用户的点赞状态和点赞数
     */
    public List<Comment> getCommentsByBuilding(int buildingId, Integer currentUserId) {
        List<Comment> comments = commentDAO.findByBuildingId(buildingId);
        for (Comment c : comments) {
            c.setLikeCount(likeDAO.countByComment(c.getId()));
            c.setLikedByMe(currentUserId != null && likeDAO.isLiked(currentUserId, c.getId()));
        }
        return comments;
    }

    public boolean addComment(int userId, int buildingId, String content, int rating) {
        if (content == null || content.trim().isEmpty()) return false;
        if (rating < 1 || rating > 5) rating = 5;
        Comment comment = new Comment(userId, buildingId, content.trim(), rating);
        return commentDAO.save(comment);
    }

    /**
     * 点赞评论
     */
    public boolean likeComment(int userId, int commentId) {
        return likeDAO.add(userId, commentId);
    }

    /**
     * 取消点赞
     */
    public boolean unlikeComment(int userId, int commentId) {
        return likeDAO.remove(userId, commentId);
    }

    /**
     * 删除评论（评论作者或管理员可删）
     */
    public boolean deleteComment(int commentId, int userId, boolean isAdmin) {
        Comment comment = commentDAO.findById(commentId);
        if (comment == null) return false;
        if (comment.getUserId() == userId || isAdmin) {
            return commentDAO.delete(commentId);
        }
        return false;
    }
}
