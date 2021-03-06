package com.sparta.hh99_actualproject.service;

import com.sparta.hh99_actualproject.dto.NotificationResponseDto;
import com.sparta.hh99_actualproject.dto.UnReadAlarmResponseDto;
import com.sparta.hh99_actualproject.model.NotiTypeEnum;
import com.sparta.hh99_actualproject.model.Notification;
import com.sparta.hh99_actualproject.repository.NotificationRepository;
import com.sparta.hh99_actualproject.util.SecurityUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class NotificationService {
    private NotificationRepository notificationRepository;

    public UnReadAlarmResponseDto getUnReadAlarmNum() {
        String memberId = SecurityUtil.getCurrentMemberId();
        long unReadAlarmNum = notificationRepository.countByMemberIdAndIsRead(memberId, false);

        return UnReadAlarmResponseDto.builder().unReadAlarmNum(unReadAlarmNum).build();
    }

    @Transactional
    public List<NotificationResponseDto> getAlarmAllList() {
        String memberId = SecurityUtil.getCurrentMemberId();

        List<Notification> notificationList = notificationRepository.findAllByMemberId(memberId);

        List<NotificationResponseDto> notificationResponseDtoList = new ArrayList<>();
        for (Notification notification : notificationList) {
            notificationResponseDtoList.add(NotificationResponseDto.builder()
                                                                    .notiType(notification.getNotiType())
                                                                    .notiContent(notification.getNotiContent())
                                                                    .oppositeMemberColor(notification.getOppositeMemberColor())
                                                                    .notiPostId(notification.getNotiPostId())
                                                                    .createAt(notification.getCreatedAt())
                                                                    .isRead(notification.isRead())
                                                                    .build());
        }

        //Get ????????? ????????? ?????? ????????? ???????????? ????????? ?????????
        for(Notification notification : notificationList){
            if (!notification.isRead()) {
                notification.setRead(true);
            }
        }

        return notificationResponseDtoList;
    }

    public Notification saveNotification(String memberId , NotiTypeEnum notiType , String notiContent , Long notiPostId){
        if(notiType.toString().equals("FOLLOW") || notiType.toString().equals("MESSAGE")){
            //????????? ??? ???????????? PostId??? ???????????? ??????.
            notiPostId = null;
        }

        //?????? ?????? ??? ???????????? ???????????? ????????? ???????????? ??? ??? ???????????? ????????? ????????? ?????? ?????? ???????????? ??????
        if(notiType.toString().equals("FOLLOW") || notiType.toString().equals("CHOICE")){
            //?????? ?????? ????????? ????????? ????????? ?????????.
            Notification findedNotification = notificationRepository.findTopByMemberIdAndNotiTypeAndNotiContentOrderByCreatedAtDesc(memberId, notiType,notiContent).orElse(null);
            //????????? ???????????? ????????? ????????? ?????? ????????? ???????????? ????????????.
            if(!isValidNotiAlarmTimeInterval(findedNotification)){
                //???????????? ????????? ????????? ???????????? ?????? ?????? Return
                return null;
            }
        }

        //?????? ????????? ?????? ?????? ????????? DB??? ??????
        return notificationRepository.save(Notification.builder()
                .memberId(memberId)
                .notiType(notiType)
                .notiContent(notiContent)
                .notiPostId(notiPostId)
                .build());
    }

    private boolean isValidNotiAlarmTimeInterval(Notification findedNotification){
        //?????? ?????? ????????? ????????? ?????? ?????? OK
        if (findedNotification == null) {
            return true;
        }
        // ????????? ????????? ????????? ?????????
        LocalDateTime savedTime = findedNotification.getCreatedAt();

        // ?????? ??????
        LocalDateTime nowTime = LocalDateTime.now();

        //?????? ?????????????????? 1?????? ???????????? True.  ?????? ???????????? ???????????? ?????? ??????
        return nowTime.isAfter(savedTime.plusMinutes(1L));
    }
}
