package com.social.app.service;

import com.social.app.dto.PostCreateDTO;
import com.social.app.dto.PostResponseDTO;
import com.social.app.enums.VoteTypeEnum;
import com.social.app.model.ContentModel;
import com.social.app.model.PostModel;
import com.social.app.model.VoteModel;
import com.social.app.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepo postRepo;
    private final  VoteService voteService;
    private final ContentService contentService;
    private final CommentService commentService;

    public List<PostModel> getAllPosts() {
        return postRepo.findAll();
    }

    @Transactional
    public PostModel createPost(PostCreateDTO newPostDTO,String userId){

        PostModel newPost = PostModel.builder()
                .title(newPostDTO.getTitle())
                .description(newPostDTO.getDescription())
                .userId(userId)
                .build();

        if(newPostDTO.getContent() != null){
            ContentModel newContent = ContentModel.builder()
                    .type(newPostDTO.getContentType())
                    .link(contentService.uploadContent(newPostDTO.getContent()))
                    .build();
            ContentModel dbContent = contentService.createContent(newContent);
            newPost.setContentId(dbContent.getId());
        }
        return postRepo.save(newPost);
    }

    public PostModel getPostById(String postId) {
        return postRepo.findById(postId).get();
    }

    public PostResponseDTO postModelToResponse(PostModel postModel){

        PostResponseDTO resPost = PostResponseDTO.builder()
                .id(postModel.getId())
                .title(postModel.getTitle())
                .userId(postModel.getUserId())
                .description(postModel.getDescription())
                .votes(voteService.getVoteCountByPost(postModel.getId()))
                .build();

        if(postModel.getContentId() != null){
            ContentModel dbContent = contentService.getContentById(postModel.getContentId());
            resPost.setContentLink(dbContent.getLink());
            resPost.setContentType(dbContent.getType());
        }
        return resPost;
    }
    public String votePost(String postId,String userId, VoteTypeEnum voteType){
        VoteModel dbVote =voteService.getVoteByPostIdAndUserId(postId, userId);
        if(dbVote == null) {
            VoteModel newVote = VoteModel.builder()
                    .voteType(voteType)
                    .userId(userId)
                    .postId(postId)
                    .build();
             voteService.createVote(newVote);
             return "Vote Created";
        }
        if(dbVote.getVoteType() == voteType){
            voteService.deleteVote(dbVote.getId());
            return "Vote Removed";
        }else{
            voteService.updateVote(dbVote.getId(), voteType);
            return "Vote Updated";
        }

    }
@Transactional
    public void deletePostById(String postId,String userId) {
        PostModel dbPost = postRepo.findByIdAndUserId(postId, userId).get();

        commentService.deleteAllByPostId(dbPost.getId());
        voteService.deleteAllByPostId(dbPost.getId());
        if(dbPost.getContentId() != null)
            contentService.deleteById(dbPost.getContentId());

        postRepo.deleteById(dbPost.getId());
    }
    public String changePublishState(String postId,String userId, boolean isPublished) {
        PostModel dbPost = postRepo.findByIdAndUserId(postId, userId).get();
        dbPost.setPublished(isPublished);
        postRepo.save(dbPost);
        return "Post updated";
    }
}
