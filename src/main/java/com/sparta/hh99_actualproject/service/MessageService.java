package com.sparta.hh99_actualproject.service;

import com.sparta.hh99_actualproject.dto.MessageDetailResponseDto;
import com.sparta.hh99_actualproject.dto.MessageRequestDto;
import com.sparta.hh99_actualproject.exception.PrivateException;
import com.sparta.hh99_actualproject.exception.StatusCode;
import com.sparta.hh99_actualproject.model.Member;
import com.sparta.hh99_actualproject.model.Message;
import com.sparta.hh99_actualproject.model.NotiTypeEnum;
import com.sparta.hh99_actualproject.model.Notification;
import com.sparta.hh99_actualproject.repository.MemberRepository;
import com.sparta.hh99_actualproject.repository.MessageRepository;
import com.sparta.hh99_actualproject.service.validator.Validator;
import com.sparta.hh99_actualproject.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final Validator validator;
    private final NotificationService notificationService;
    
    public MessageDetailResponseDto getMessageDetail(Long messageId) {

        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MESSAGE));


        return MessageDetailResponseDto.builder()
                .reqUserNickName(message.getReqUserNickName())
                .reqUserId(message.getReqUserId())
                .resUserNickName(message.getResUserNickName())
                .resUserId(message.getResUserId())
                .createdAt(String.valueOf(message.getCreatedAt()))
                .message(message.getMessage())
                .build();
    }


    @Transactional
    public void sendMessage(MessageRequestDto messageRequestDto) {
        String memberId = SecurityUtil.getCurrentMemberId();

        Member reqMember = memberRepository.findByMemberId(memberId).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));

        Member resMember = memberRepository.findByMemberId(messageRequestDto.getResUserId()).orElseThrow(
                () -> new PrivateException(StatusCode.NOT_FOUND_MEMBER));


        validator.hasNullCheckMessage(messageRequestDto);

        Message message = Message.builder()
                .member(resMember) //?????? ????????? ???????????? ????????????????????? ?????? member?????? ?????????
                .reqUserId(reqMember.getMemberId())
                .resUserId(resMember.getMemberId())
                .reqUserNickName(reqMember.getNickname())
                .resUserNickName(resMember.getNickname())
                .message(messageRequestDto.getMessage())
                .build();

        //?????? ????????? ?????? ?????? ????????? DB??? ?????? . ??????????????? ?????? ????????? ????????? ?????? ?????????!
        Notification savedNotification = notificationService.saveNotification(resMember.getMemberId(),NotiTypeEnum.MESSAGE, reqMember.getNickname() , null);
        //???????????? color ??? ?????????????????? ??????
        if (savedNotification != null) {
            savedNotification.setOppositeMemberColor(reqMember.getColor());
        }

        messageRepository.save(message);
    }
}
