package com.sparta.hh99_actualproject.service;


import com.sparta.hh99_actualproject.dto.*;
import com.sparta.hh99_actualproject.dto.MemberInfo.MemberInfoChatResponseDto;
import com.sparta.hh99_actualproject.dto.MemberInfo.MemberInfoMessageResponseDto;
import com.sparta.hh99_actualproject.dto.MemberInfo.MemberInfoResponseDto;
import com.sparta.hh99_actualproject.dto.MemberInfo.MemebrInfoFollowResponseDto;
import com.sparta.hh99_actualproject.exception.PrivateException;
import com.sparta.hh99_actualproject.exception.StatusCode;
import com.sparta.hh99_actualproject.jwt.TokenProvider;
import com.sparta.hh99_actualproject.model.*;
import com.sparta.hh99_actualproject.repository.*;
import com.sparta.hh99_actualproject.service.validator.Validator;
import com.sparta.hh99_actualproject.util.SecurityUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AllArgsConstructor
public class MemberService {

    private MemberRepository memberRepository;
    private Validator validator;
    private PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final FollowRepository followRepository;
    private final ResponseTagRepository responseTagRepository;
    private final ScoreRepository scoreRepository;
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final BoardRepository boardRepository;
    private final ResponseTagService responseTagService;

    public boolean signup(MemberRequestDto memberRequestDto) {
        validator.validateSignUpInput(memberRequestDto);

        if (memberRepository.existsByMemberId(memberRequestDto.getMemberId()))
            throw new PrivateException(StatusCode.SIGNUP_MEMBER_ID_DUPLICATE_ERROR);

        Member member = Member.builder()
                .memberId(memberRequestDto.getMemberId())
                .nickname(memberRequestDto.getName())
                .password(passwordEncoder.encode(memberRequestDto.getPassword()))
                .reward(5F)
                .build();

        memberRepository.save(member);

        return true;
    }

    public TokenDto login(MemberRequestDto memberRequestDto) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(memberRequestDto.getMemberId(), memberRequestDto.getPassword());

        //authenticate ???????????? ????????? ??? ??? CustomMemberDetailsService ?????? loadMemberByMembername ???????????? ?????? ???
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        //?????? ???????????? ????????? SecurityContext ??? ??????
        SecurityContextHolder.getContext().setAuthentication(authentication);

        TokenDto tokenDto;

        Member findedMember = memberRepository.findByMemberId(memberRequestDto.getMemberId())
                .orElseThrow(() -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        //TODO : refreshToken ?????? ??????
        String accessToken = tokenProvider.createAccessToken(authentication.getName(), findedMember.getNickname());

        tokenDto = TokenDto.builder()
                .accessToken(accessToken)
                .build();

        return tokenDto;
    }

    public TokenDto updateMemberInfo(EssentialInfoRequestDto essentialInfoRequestDto) {
        validator.validateMemberInfoInput(essentialInfoRequestDto);

        Member findedMember = memberRepository.findByMemberId(SecurityUtil.getCurrentMemberId())
                .orElseThrow(() -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        //??????????????? ?????? ?????? ?????? ??????
        if(findedMember.getNickname() != null){
            //?????? ??????????????? ???????????? ??? ???????????? ????????? ????????? ?????? ????????????
            if( !findedMember.getNickname().equals(essentialInfoRequestDto.getNickname())){
                checkNickname(essentialInfoRequestDto.getNickname());
            }
        }

        findedMember.updateMemberEssentialInfo(essentialInfoRequestDto);

        memberRepository.save(findedMember);

        TokenDto tokenDto;

        //TODO : refreshToken ?????? ??????
        String accessToken = tokenProvider.createAccessToken(findedMember.getMemberId(), findedMember.getNickname());

        tokenDto = TokenDto.builder()
                .accessToken(accessToken)
                .build();

        return tokenDto;
    }

    @Transactional
    public MemberInfoResponseDto getMemberProfile(){
        String memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findByMemberId(memberId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));


        //????????? ??????????????? ?????? ?????? ??? ??????
        //followerMemberList??? ?????? ??????????????? ????????? ?????? ????????????.
        //?????? ???????????? ???????????? ????????? ?????? ???????????? ??? ???????????? ??????
        List<Follow> getFollowerList = followRepository.findAllByFollowMemberId(memberId);

        ResTagResponseDto resTagResponseDto = responseTagService.findMemberMostResTag(memberId);

        //score??? memberId??? ?????? ????????? score??? ????????????.
        Score score = null;

        try {
            score = scoreRepository.findByMemberId(memberId).orElseThrow(
                    () -> new PrivateException(StatusCode.NOT_FOUND_SCORE));
        }catch (PrivateException exception){
            score = Score.builder()
                    .score(36.5F)
                    .build();
        }

        MemberInfoResponseDto memberInfoResponseDto = MemberInfoResponseDto.builder()
                .memberId(memberId)
                .nickname(member.getNickname())
                .color(member.getColor())
                .gender(member.getGender())
                .lovePeriod(member.getLovePeriod())
                .loveType(member.getLoveType())
                .age(member.getAge())
                .dating(member.getDating())
                .score(score.getScore())
                .reward(member.getReward())
                .follower(getFollowerList.size())
                .build();

        if (resTagResponseDto != null && resTagResponseDto.getResTag1() != null){
            memberInfoResponseDto.setResTag1(resTagResponseDto.getResTag1());
        }

        if (resTagResponseDto != null && resTagResponseDto.getResTag2() != null){
            memberInfoResponseDto.setResTag2(resTagResponseDto.getResTag2());
        }

        return memberInfoResponseDto;
    }

    @Transactional
    public List<MemberInfoChatResponseDto> getMemberChatHistory() {
        String memberId = SecurityUtil.getCurrentMemberId();

        Member member = memberRepository.findByMemberId(memberId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        //????????? ???????????? ?????? ??? ??????
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllByReqMemberIdOrResMemberIdOrderByCreatedAtDesc(memberId , memberId);

        //?????? ??????????????? ????????? Dto??? ?????? ??????
        List<MemberInfoChatResponseDto> chatHistoryResponseDtoList = new ArrayList<>();

        //ChatRoom??? data??? return??? ??????????????? ?????? -> ???????????? ?????????.
        //ChatHistory??? ??????
        for (ChatRoom chatRoom : chatRoomList) {
            MemberInfoChatResponseDto chatHistoryReponseDto = MemberInfoChatResponseDto.builder()
                    .reqComment(chatRoom.getReqTitle())
                    .reqCategory(chatRoom.getReqCategory())
                    .createdAt(chatRoom.getMatchTime())
                    .build();



            if (member.getMemberId().equals(chatRoom.getReqMemberId())){
                chatHistoryReponseDto.setMyRole("????????? ??????");
                chatHistoryReponseDto.setNickname(chatRoom.getResNickname());
                chatHistoryReponseDto.setColor(chatRoom.getResMemberColor());
            }else if (member.getMemberId().equals(chatRoom.getResMemberId())){
                chatHistoryReponseDto.setMyRole("????????? ??????");
                chatHistoryReponseDto.setNickname(chatRoom.getReqNickname());
                chatHistoryReponseDto.setColor(chatRoom.getReqMemberColor());
            }
            chatHistoryResponseDtoList.add(chatHistoryReponseDto);

            if (chatHistoryResponseDtoList.size() == 6){
                break;
            }
        }
        return chatHistoryResponseDtoList;
    }
    @Transactional
    public List<MemberInfoMessageResponseDto> getMemberMessage(int page) {
        String memberId = SecurityUtil.getCurrentMemberId();
        PageRequest pageRequest = PageRequest.of(page-1 , 3);

        //????????? ????????? ???????????? ????????? ???
        Page<Message> messageList = messageRepository.findAllByMemberMemberIdOrderByCreatedAtDesc(memberId , pageRequest);

        List<MemberInfoMessageResponseDto> messageListResponseDtos = new ArrayList<>();

        for (Message getMessage : messageList) {
            MemberInfoMessageResponseDto messageResponseDto = MemberInfoMessageResponseDto.builder()
                    .messageId(getMessage.getMessageId())
                    .createdAt(getMessage.getCreatedAt())
                    .reqMemberNickname(getMessage.getReqUserNickName())
                    .message(getMessage.getMessage())
                    .totalPages(messageList.getTotalPages())
                    .build();
            messageListResponseDtos.add(messageResponseDto);
        }

        return messageListResponseDtos;
    }
    @Transactional
    public List<MemebrInfoFollowResponseDto> getMemberFollow(int page) {
        //????????? ????????? ?????? ?????? ??? ??????
        String memberId = SecurityUtil.getCurrentMemberId();

        PageRequest pageRequest = PageRequest.of(page-1 , 5);

        Page<Follow> getFollowList = followRepository.findAllByMemberMemberIdOrderByCreatedAt(memberId, pageRequest);

        List<MemebrInfoFollowResponseDto> followList = new ArrayList<>();



        for (Follow follow : getFollowList) {
            MemebrInfoFollowResponseDto followResponseDto = MemebrInfoFollowResponseDto.builder()
                    .followMemberId(follow.getFollowMemberId())
                    .createdAt(String.valueOf(follow.getCreatedAt()))
                    .nickname(follow.getNickname())
                    .color(follow.getColor())
                    .totalPages(getFollowList.getTotalPages())
                    .build();
            followList.add(followResponseDto);
        }

        return followList;
    }
    @Transactional
    public BoardResponseDto.AllPostPageResponseDto getMemberBoard(int page) {
        String memberId = SecurityUtil.getCurrentMemberId();

        //?????? ???????????? ?????? ????????????.
        //????????? ???????????? ??????????????? ?????? build??? dto??? ????????????.
        PageRequest pageRequest = PageRequest.of(page-1 , 8);

        Page<SimpleBoardInfoInterface> simpleBoardInfoPages = boardRepository.findAllPostByMemberId(memberId , pageRequest);

        return BoardResponseDto.AllPostPageResponseDto.builder()
                .content(simpleBoardInfoPages.getContent())
                .totalPages(simpleBoardInfoPages.getTotalPages())
                .totalElements(simpleBoardInfoPages.getTotalElements())
                .pageNumber(simpleBoardInfoPages.getPageable().getPageNumber()+1) // Request Page = getPageNumber + 1
                .size(simpleBoardInfoPages.getSize())
                .first(simpleBoardInfoPages.isFirst())
                .last(simpleBoardInfoPages.isLast())
                .empty(simpleBoardInfoPages.isEmpty())
                .build();
    }


    public void checkMemberId(String memberId) {
        if(!validator.isValidMemberId(memberId)){
            throw new PrivateException(StatusCode.SIGNUP_MEMBER_ID_FORM_ERROR);
        }

        if (memberRepository.existsByMemberId(memberId)) {
            throw new PrivateException(StatusCode.SIGNUP_MEMBER_ID_DUPLICATE_ERROR);
        }
    }

    public void checkNickname(String nickname) {
        if(!validator.isValidNickname(nickname)){
            throw new PrivateException(StatusCode.SIGNUP_NICKNAME_FORM_ERROR);
        }

        if (memberRepository.existsByNickname(nickname))
            throw new PrivateException(StatusCode.SIGNUP_NICKNAME_DUPLICATE_ERROR);
    }


    public HashMap<String , Float> getReward() {
        String memberId = SecurityUtil.getCurrentMemberId();
        Member member = memberRepository.findByMemberId(memberId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        HashMap<String , Float> reward = new HashMap<>();

        reward.put("reward" , member.getReward());

        return reward;
    }
}
