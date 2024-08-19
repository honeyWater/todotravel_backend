package org.example.todotravel.domain.plan.service.implement;

import lombok.RequiredArgsConstructor;
import org.example.todotravel.domain.notification.dto.request.AlarmRequestDto;
import org.example.todotravel.domain.notification.service.AlarmService;
import org.example.todotravel.domain.plan.dto.response.PlanListResponseDto;
import org.example.todotravel.domain.plan.entity.Plan;
import org.example.todotravel.domain.plan.entity.PlanUser;
import org.example.todotravel.domain.plan.repository.PlanUserRepository;
import org.example.todotravel.domain.plan.service.PlanService;
import org.example.todotravel.domain.plan.service.PlanUserService;
import org.example.todotravel.domain.user.dto.response.UserProfileResponseDto;
import org.example.todotravel.domain.user.entity.User;
import org.example.todotravel.domain.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanUserServiceImpl implements PlanUserService {
    private final PlanUserRepository planUserRepository;
    private final PlanService planService;
    private final UserService userService;
    private final AlarmService alarmService; //알림 자동 생성

    //플랜 초대
    @Override
    @Transactional
    public PlanUser addPlanUser(Long planId, Long userId) {
        Plan plan = planService.getPlan(planId);
        User user = userService.getUserByUserId(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        PlanUser planUser = PlanUser.builder()
                .status(PlanUser.StatusType.PENDING)
                .user(user)
                .plan(plan)
                .build();

        PlanUser newPlanUser = planUserRepository.save(planUser);


        AlarmRequestDto requestDto = new AlarmRequestDto(plan.getPlanUser().getUserId(),
                "[" + plan.getTitle() + "] 플랜에 " + user.getNickname()+ "님이 초대 되었습니다.");
        alarmService.createAlarm(requestDto);

        return newPlanUser;
    }

    //플랜 초대 거절
    @Override
    @Transactional
    public PlanUser rejected(Long planParticipantId) {
        PlanUser planUser = planUserRepository.findById(planParticipantId).orElseThrow(() -> new RuntimeException("플랜 참여 상태를 찾을 수 없습니다."));
        planUser.setStatus(PlanUser.StatusType.REJECTED);
        return planUserRepository.save(planUser);
    }

    //플랜 초대 수락
    @Override
    @Transactional
    public PlanUser accepted(Long planParticipantId) {
        PlanUser planUser = planUserRepository.findById(planParticipantId).orElseThrow(() -> new RuntimeException("플랜 참여 상태를 찾을 수 없습니다."));
        planUser.setStatus(PlanUser.StatusType.ACCEPTED);
        return planUserRepository.save(planUser);
    }

    //플랜 참여자 조회
    @Override
    @Transactional(readOnly = true)
    public List<PlanUser> getAllPlanUser(Long planId) {
        Plan plan = planService.getPlan(planId);
        return planUserRepository.findAllByPlan(plan);
    }

    @Override
    @Transactional
    public void removePlanUser(Long planId, Long userId) {
        Plan plan = planService.getPlan(planId);
        User user = userService.getUserByUserId(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        planUserRepository.deletePlanUserByPlanAndUser(plan, user);
    }

    // 사용자 프로필 조회
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getUserProfile(String subject, Long userId) {
        User user = userService.getUserById(userId);
        List<PlanListResponseDto> planList;

        if (subject.equals("other")) {
            planList = getAllPlansByUser(userId);
        } else {
            planList = getRecentPlansByUser(userId);
        }

        return UserProfileResponseDto.builder()
            .userId(userId)
            .nickname(user.getNickname())
            .followerCount(user.getFollowers().size())
            .followingCount(user.getFollowings().size())
            .planCount(user.getPlans().size())
            .planList(planList)
            .build();
    }

    // 특정 사용자가 참여한 모든 플랜 조회
    @Override
    @Transactional(readOnly = true)
    public List<PlanListResponseDto> getAllPlansByUser(Long userId) {
        List<Plan> plans = planUserRepository.findAllPlansByUserId(userId, PlanUser.StatusType.ACCEPTED);
        return plans.stream()
            .map(planService::convertToPlanListResponseDto)
            .collect(Collectors.toList());
    }

    // 특정 사용자가 참여한 최근 플랜 3개 조회
    @Override
    @Transactional(readOnly = true)
    public List<PlanListResponseDto> getRecentPlansByUser(Long userId) {
        List<Plan> plans = planUserRepository.findRecentPlansByUserId(userId);
        return plans.stream()
            .map(planService::convertToPlanListResponseDto)
            .collect(Collectors.toList());
    }
}
